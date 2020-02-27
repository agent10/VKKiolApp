package kiol.vkapp.commondata.domain.places

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

class PlacesUseCase2 {

    private val placesCache = PlacesCache()

    fun getPlaces(placeType: PlaceType): Flowable<List<Place>> {
        return placesCache.getValue(placeType)
    }
}