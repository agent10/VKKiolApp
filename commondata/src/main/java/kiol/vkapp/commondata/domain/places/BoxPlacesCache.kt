package kiol.vkapp.commondata.domain.places

import io.reactivex.Flowable
import kiol.vkapp.commondata.cache.RxResponseCache
import kiol.vkapp.commondata.domain.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random


class BoxPlacesCache : RxResponseCache<Nothing, List<Place>>() {

    override fun fetch(param: Nothing?): Flowable<List<Place>> {
        val places = mutableListOf<Place>()
        val r = Random(1000)
        repeat(1000) {
            places += Place(
                it, PlaceType.Box, r.nextInt(-160, 160).toFloat(), r.nextInt(-90, 90).toFloat(),
                "Test $it",
                "Addr $it",
                "Desc $it", "", null, CustomPlaceParams(Box("", BoxType.Fraud, emptyList()))
            )
        }

        getRandomLocations(30.3709f, 59.9396f, 10000).forEach {
            places += Place(
                places.size + 1, PlaceType.Box, it.first, it.second,
                "Test $it",
                "Addr $it",
                "Desc $it", "", null, CustomPlaceParams(Box("", BoxType.Fraud, emptyList()))
            )
        }

        getRandomLocations(37.6139f, 55.7470f, 20000).forEach {
            places += Place(
                places.size + 1, PlaceType.Box, it.first, it.second,
                "Test $it",
                "Addr $it",
                "Desc $it", "", null, CustomPlaceParams(Box("", BoxType.Fraud, emptyList()))
            )
        }

        return Flowable.fromCallable { places }
    }

    fun getRandomPlaces(x0: Float, y0: Float, radius: Int, count: Int = 30): List<Place> {
        val places = mutableListOf<Place>()
        getRandomLocations(x0, y0, radius, count).forEach {
            places += Place(
                places.size + 1, PlaceType.Box, it.first, it.second,
                "Test $it",
                "Addr $it",
                "Desc $it", "", null, CustomPlaceParams(Box("", BoxType.Fraud, emptyList()))
            )
        }
        return places
    }

    private fun getRandomLocations(x0: Float, y0: Float, radius: Int, count: Int = 30): List<Pair<Float, Float>> {

        val res = mutableListOf<Pair<Float, Float>>()

        val random = Random(1000)
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
}