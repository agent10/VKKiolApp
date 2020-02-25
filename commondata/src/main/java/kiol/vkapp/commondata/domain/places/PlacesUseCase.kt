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

class PlacesUseCase {

    fun getPlaces(placeType: PlaceType): Flowable<List<Place>> {
        return when (placeType) {
            Groups -> getGroupsOrEvents(false).map {
                it.map {
                    val p = it.place
                    Place(
                        placeType,
                        p.latitude, p.longitude,
                        p.title,
                        p.address.orEmpty(),
                        it.description.orEmpty(),
                        p.group_photo.orEmpty()
                    )
                }
            }
            Events -> getGroupsOrEvents(true).map {
                it.map {
                    val p = it.place
                    Place(
                        placeType,
                        p.latitude, p.longitude,
                        p.title,
                        p.address.orEmpty(),
                        it.description.orEmpty(),
                        p.group_photo.orEmpty()
                    )
                }
            }
            Photos -> getPhotos().doOnNext {
                val a = 10
            }.map {
                it.filter {
                    it.lat > 0f && it.long > 0f
                }.map {
                    Place(placeType, it.lat, it.long, "", "", it.text, it.photo_130)
                }
            }
        }
    }

    private fun getGroupsOrEvents(events: Boolean) = Flowable.fromCallable {
        val request = VKRequest<JSONObject>("groups.get")
            .addParam("filter", if (events) "events" else "groups")
            .addParam("extended", 1)
            .addParam("fields", "place,description")
            .addParam("count", 100)
            .addParam("offset", 0)
        VK.executeSync(request)
    }.map {
        val groups: VKResponse<VKListContainerResponse<VKGroup>> = Gson().fromJson(it.toString())
        groups.response.items
    }

    private fun getPhotos() = Flowable.fromCallable {
        val request = VKRequest<JSONObject>("photos.get")
            .addParam("count", 1000)
            .addParam("album_id", "wall")
            .addParam("offset", 0)
        VK.executeSync(request)
    }.map {
        val groups: VKResponse<VKListContainerResponse<VKPhoto>> = Gson().fromJson(it.toString())
        groups.response.items
    }
}