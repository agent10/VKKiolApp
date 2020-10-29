package kiol.vkapp.commondata.domain.places

import android.net.Uri
import com.google.gson.Gson
import com.vk.api.sdk.VK
import com.vk.api.sdk.requests.VKRequest
import io.reactivex.Flowable
import kiol.vkapp.commondata.data.*
import kiol.vkapp.commondata.domain.Place
import kiol.vkapp.commondata.domain.PlaceType
import kiol.vkapp.commondata.domain.PlaceType.*
import org.json.JSONObject
import kotlin.random.Random

class PlacesUseCase {

    private val placesCache = PlacesCache()
    private val boxPlacesCache = BoxPlacesCache()

    var currentPrettyAddress = ""

    fun getPlaces(placeType: PlaceType): Flowable<List<Place>> {
        return placesCache.getValue(placeType)
    }

    fun getBoxes(): Flowable<List<Place>> {
        return boxPlacesCache.getValue(null)
    }

    fun observeChanges() = boxPlacesCache.observeChanges()

    fun addBoxForCheck(uri: Uri) = boxPlacesCache.addBoxForCheck(currentPrettyAddress, uri)

    fun setLatLong(lat: Float, lon: Float) {
        boxPlacesCache.setLatLong(lat, lon)
    }
}