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
import java.lang.Exception

class PlacesCache : RxResponseCache<PlaceType, List<Place>>() {

    override fun fetch(param: PlaceType?): Flowable<List<Place>> {
        param?.let {
            return when (param) {
                Box -> getBoxes()
                Groups -> getGroupsOrEvents(false).map {
                    it.mapGroups(Groups)
                }
                Events -> getGroupsOrEvents(true).map {
                    it.mapGroups(Events)
                }
                Photos -> getPhotos()
            }
        } ?: throw Exception("PlacesCache param must be set")
    }

    private fun List<VKGroup>.mapGroups(placeType: PlaceType, vkGroups: List<VKGroup> = emptyList()): List<Place> {
        return filter {
            it.place != null &&
                    it.place.latitude > Float.MIN_VALUE &&
                    it.place.longitude > Float.MIN_VALUE
        }.map {
            it.convert(placeType, vkGroups)
        }
    }

    private fun List<VKPhoto>.mapPhotos(placeType: PlaceType): List<Place> {
        return filter {
            it.lat > Float.MIN_VALUE && it.long > Float.MIN_VALUE
        }.map {
            it.convert(placeType)
        }
    }

    private fun getBoxes() = getGroupsByIds().flatMap { stubGroups ->
        getGroupsOrEvents(false).map {
            it.mapGroups(Groups, stubGroups)
        }
    }

    private fun getGroupsOrEvents(events: Boolean) = Flowable.fromCallable {
        val request = VKRequest<JSONObject>("groups.get")
            .addParam("filter", if (events) "events" else "groups, publics")
            .addParam("extended", 1)
            .addParam("fields", "place,description")
            .addParam("count", 1000)
            .addParam("offset", 0)
        VK.executeSync(request)
    }.map {
        val groups: VKResponse<VKListContainerResponse<VKGroup>> = Gson().fromJson(it.toString())
        groups.response.items
    }

    private fun getGroupsByIds() = Flowable.fromCallable {
        val request = VKRequest<JSONObject>("groups.getById")
            .addParam("group_ids", "198155259,147415323,179600088,17796776")
        VK.executeSync(request)
    }.map {
        val groups: VKResponse<List<VKGroup>> = Gson().fromJson(it.toString())
        groups.response
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