package kiol.vkapp.commondata.domain.docs

import com.google.gson.Gson
import com.vk.api.sdk.VK
import com.vk.api.sdk.VKApiCallback
import com.vk.api.sdk.exceptions.VKApiExecutionException
import com.vk.api.sdk.requests.VKRequest
import io.reactivex.Flowable
import kiol.vkapp.commondata.cache.RxListResponseCache
import kiol.vkapp.commondata.data.VKDocItem
import kiol.vkapp.commondata.data.VKListContainerResponse
import kiol.vkapp.commondata.data.VKResponse
import kiol.vkapp.commondata.data.fromJson
import org.json.JSONObject
import timber.log.Timber

class DocsCache : RxListResponseCache<Nothing, VKDocItem, MutableCollection<VKDocItem>>() {

    override fun fetch(param: Nothing?, offset: Long, count: Long): Flowable<MutableCollection<VKDocItem>> {
        return Flowable.fromCallable {
            val request = VKRequest<JSONObject>("docs.get")
                .addParam("return_tags", 1)
                .addParam("count", count)
                .addParam("offset", offset)

            VK.executeSync(request)
        }.map {
            val docsResponse: VKResponse<VKListContainerResponse<VKDocItem>> = Gson().fromJson(it.toString())
            docsResponse.response.items
        }.map {
            it.toMutableList()
        }
    }

    fun removeDoc(docId: Int, ownerId: Int) {
        val request = VKRequest<JSONObject>("docs.delete")
            .addParam("owner_id", ownerId)
            .addParam("doc_id", docId)

        VK.execute(request, object : VKApiCallback<JSONObject> {
            override fun fail(error: VKApiExecutionException) {
                Timber.e("removeDoc failed $error")
            }

            override fun success(result: JSONObject) {
                Timber.d("removeDoc success $result")
                reset()
            }
        })
    }

    fun updateDocName(docId: Int, ownerId: Int, title: String) {
        val request = VKRequest<JSONObject>("docs.edit")
            .addParam("owner_id", ownerId)
            .addParam("doc_id", docId)
            .addParam("title", title)

        VK.execute(request, object : VKApiCallback<JSONObject> {
            override fun fail(error: VKApiExecutionException) {
                Timber.e("updateDocName failed $error")
            }

            override fun success(result: JSONObject) {
                Timber.d("updateDocName success $result")
                reset()
            }
        })
    }
}