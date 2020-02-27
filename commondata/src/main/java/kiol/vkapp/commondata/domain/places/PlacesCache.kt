package kiol.vkapp.commondata.domain.places

import com.google.gson.Gson
import com.vk.api.sdk.VK
import com.vk.api.sdk.requests.VKRequest
import io.reactivex.Flowable
import kiol.vkapp.commondata.cache.RxResponseCache
import kiol.vkapp.commondata.data.*
import kiol.vkapp.commondata.domain.Place
import kiol.vkapp.commondata.domain.PlaceType
import kiol.vkapp.commondata.domain.PlaceType.*
import kiol.vkapp.commondata.domain.convert
import org.json.JSONObject
import java.lang.RuntimeException
import java.net.ResponseCache

class PlacesCache : RxResponseCache<PlaceType, List<Place>>() {

    override fun fetch(param: PlaceType?): Flowable<List<Place>> {
        param?.let {
            return when (param) {
                Groups -> getGroupsOrEvents(false)
                Events -> getGroupsOrEvents(true)
                Photos -> getPhotos()
            }
        } ?: throw RuntimeException("PlacesCache param must be set")
    }

    private fun List<VKGroup>.mapGroups(placeType: PlaceType): List<Place> {
        return map {
            it.convert(placeType)
        }
    }

    private fun List<VKPhoto>.mapPhotos(placeType: PlaceType): List<Place> {
        return map {
            it.convert(placeType)
        }
    }

    private fun getGroupsOrEvents(events: Boolean) = Flowable.fromCallable {
        val request = VKRequest<JSONObject>("groups.get")
            .addParam("filter", if (events) "events" else "groups")
            .addParam("extended", 1)
            .addParam("fields", "place,description")
            .addParam("count", 1000)
            .addParam("offset", 0)
        VK.executeSync(request)
    }.map {
        val groups: VKResponse<VKListContainerResponse<VKGroup>> = Gson().fromJson(it.toString())
        groups.response.items.mapGroups(if (events) Events else Groups)
    }

    private fun getPhotos() = Flowable.fromCallable {
        val request = VKRequest<JSONObject>("photos.get")
            .addParam("count", 1000)
            .addParam("photo_sizes", 1)
            .addParam("album_id", "wall")
            .addParam("offset", 0)
        VK.executeSync(request)
    }.map {
        val groups: VKResponse<VKListContainerResponse<VKPhoto>> = Gson().fromJson(it.toString())
        groups.response.items.mapPhotos(Photos)
    }
}