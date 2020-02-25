package kiol.vkapp.commondata.domain.places

import com.google.gson.Gson
import com.vk.api.sdk.VK
import com.vk.api.sdk.requests.VKRequest
import io.reactivex.Flowable
import kiol.vkapp.commondata.data.VKGroup
import kiol.vkapp.commondata.data.VKListContainerResponse
import kiol.vkapp.commondata.data.VKResponse
import kiol.vkapp.commondata.data.fromJson
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
            Events -> TODO()
            Photos -> TODO()
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
}