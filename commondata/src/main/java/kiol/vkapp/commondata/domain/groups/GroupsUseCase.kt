package kiol.vkapp.commondata.domain.groups

import com.google.gson.Gson
import com.vk.api.sdk.VK
import com.vk.api.sdk.requests.VKRequest
import io.reactivex.Flowable
import kiol.vkapp.commondata.data.VKGroup
import kiol.vkapp.commondata.data.VKListContainerResponse
import kiol.vkapp.commondata.data.VKResponse
import kiol.vkapp.commondata.data.fromJson
import org.json.JSONObject

class GroupsUseCase {

    fun getGroups() = Flowable.fromCallable {
        val request = VKRequest<JSONObject>("groups.get")
            .addParam("filter", "groups, publics")
            .addParam("extended", 1)
            .addParam("fields", "description")
            .addParam("count", 1000)
            .addParam("offset", 0)
        VK.executeSync(request)
    }.map {
        val groups: VKResponse<VKListContainerResponse<VKGroup>> = Gson().fromJson(it.toString())
        groups.response.items
    }
}