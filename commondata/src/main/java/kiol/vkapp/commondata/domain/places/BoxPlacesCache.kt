package kiol.vkapp.commondata.domain.places

import android.net.Uri
import com.google.gson.Gson
import com.vk.api.sdk.VK
import com.vk.api.sdk.requests.VKRequest
import io.reactivex.Flowable
import io.reactivex.processors.PublishProcessor
import kiol.vkapp.commondata.cache.RxResponseCache
import kiol.vkapp.commondata.data.VKGroup
import kiol.vkapp.commondata.data.VKResponse
import kiol.vkapp.commondata.data.fromJson
import kiol.vkapp.commondata.domain.*
import org.json.JSONObject
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random


class BoxPlacesCache : RxResponseCache<Nothing, List<Place>>() {

    companion object {
        private val PhotosFakes = listOf(
            "box_fake_1.jpg",
            "box_fake_2.jpg", "box_fake_3.jpg", "box_fake_4.jpg", "box_fake_5.jpg"
        )

        private val BigPhotosFakes = listOf(
            "box_big_fake_1.jpg",
            "box_big_fake_2.jpg", "box_big_fake_3.jpg", "box_big_fake_4.jpg", "box_big_fake_5.jpg"
        )

        private val AddrsFakes = listOf(
            "Выдуманный адрес 2к1",
            "Ещё какой-то адрес 15", "ул. Фейковая 112", "ул. Вконтакте 999", "пр-кт. Приложения 11"
        )

        private val DescrFakes = listOf(
            "Это придуманный ящик с описанием благотворительной организации. Помощь кому-то\nТел. 999-99-00\nWhatsapp: 9998877",
            "Другой фейковый ящик с описанием благотворительной организации. Помощь кому-то\nwww.site.com\nТел. " +
                    "999-99-00\nWhatsapp: " +
                    "9998877"
        )

        private val GroupsFakes = listOf(
            "198155259", "147415323", "179600088", "17796776", "15274000", "133169189", "6803013", "35966541"
        )
    }

    private var lat = 0f
    private var lon = 0f

    private val random = Random(1000)

    private val boxForChecks = mutableListOf<Place>()

    private val processor = PublishProcessor.create<Place>()

    override fun fetch(param: Nothing?): Flowable<List<Place>> {
        return Flowable.defer {
            val vkGroups = getGroupsByIds().blockingSingle(emptyList())
            val places = mutableListOf<Place>()
            repeat(1000) {
                places += getRandomPlace(
                    places.size,
                    random.nextInt(-160, 160).toFloat(),
                    random.nextInt(-90, 90).toFloat(),
                    vkGroups
                )
            }

            places += getRandomPlaces(30.3709f, 59.9396f, 10000, vkGroups)
            places += getRandomPlaces(37.6139f, 55.7470f, 10000, vkGroups)
            places += getRandomPlaces(lon, lat, 500, vkGroups, 8)

            places += boxForChecks

            Flowable.just(places)
        }
    }

    fun observeChanges(): Flowable<Place> = processor

    fun addBoxForCheck(addr: String, uri: Uri): Place {
        val place = getRandomPlaces(lon, lat, 10, emptyList(), 1).first()
        val nb = place.customPlaceParams!!.box.copy(boxType = BoxType.Unknown, photo = uri.toString())
        val np = place.copy(photo = uri.toString(), address = addr, customPlaceParams = CustomPlaceParams(nb))
        boxForChecks += np
        processor.offer(np)
        return np
    }

    private fun getRandomPlace(id: Int, x: Float, y: Float, vkGroups: List<VKGroup>): Place {
        val boxType = BoxType.values().random()
        val fakePhotoIndex = PhotosFakes.indexOf(PhotosFakes.random())
        val fid = random.nextInt() + 10000
        return Place(
            fid, PlaceType.Box, x, y,
            "Ящичек  №${fid}",
            AddrsFakes.random(),
            DescrFakes.random(), "file:///android_asset/${PhotosFakes[fakePhotoIndex]}", null,
            CustomPlaceParams(
                Box(
                    "file:///android_asset/${BigPhotosFakes[fakePhotoIndex]}",
                    boxType,
                    vkGroups.shuffled(random).take(4)
                )
            )
        )
    }

    private fun getGroupsByIds() = Flowable.fromCallable {
        val request = VKRequest<JSONObject>("groups.getById")
            .addParam("group_ids", GroupsFakes)
        VK.executeSync(request)
    }.map {
        val groups: VKResponse<List<VKGroup>> = Gson().fromJson(it.toString())
        groups.response
    }

    private fun getRandomPlaces(x0: Float, y0: Float, radius: Int, vkGroups: List<VKGroup>, count: Int = 30): List<Place> {
        val places = mutableListOf<Place>()
        getRandomLocations(x0, y0, radius, count).forEach {
            places += getRandomPlace(places.size + 1, it.first, it.second, vkGroups)
        }
        return places
    }

    private fun getRandomLocations(x0: Float, y0: Float, radius: Int, count: Int = 30): List<Pair<Float, Float>> {
        val res = mutableListOf<Pair<Float, Float>>()

        repeat(count) {
            val radiusInDegrees = radius / 111000f.toDouble()
            val u = random.nextDouble()
            val v = random.nextDouble()
            val w = radiusInDegrees * sqrt(u)
            val t = 2 * Math.PI * v
            val x = w * cos(t)
            val y = w * sin(t)

            val newx = x / cos(Math.toRadians(y0.toDouble()))
            val foundLongitude = newx + x0
            val foundLatitude = y + y0

            res += Pair(foundLatitude.toFloat(), foundLongitude.toFloat())
        }

        return res
    }

    internal fun setLatLong(lat: Float, lon: Float) {
        this.lat = lat
        this.lon = lon
    }
}