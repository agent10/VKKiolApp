package kiol.vkapp.commondata.domain.photos

import com.google.gson.Gson
import com.vk.api.sdk.VK
import com.vk.api.sdk.requests.VKRequest
import io.reactivex.Flowable
import kiol.vkapp.commondata.data.*
import org.json.JSONObject

fun getPhotos() = Flowable.fromCallable {
    val request = VKRequest<JSONObject>("photos.get")
    VK.executeSync(request)
}

fun getGroups() = Flowable.fromCallable {
    val request = VKRequest<JSONObject>("groups.get")
        .addParam("extended", 1)
        .addParam("fields", "place")
        .addParam("count", 100)
        .addParam("offset", 0)
    VK.executeSync(request)
}.map {
    val groups: VKResponse<VKListContainerResponse<VKGroup>> = Gson().fromJson(it.toString())
    groups.response.items
}