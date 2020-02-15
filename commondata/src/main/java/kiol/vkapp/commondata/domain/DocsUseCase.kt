package kiol.vkapp.commondata.domain

import android.text.format.DateFormat
import com.google.gson.Gson
import com.vk.api.sdk.VK
import com.vk.api.sdk.VKApiCallback
import com.vk.api.sdk.exceptions.VKApiExecutionException
import com.vk.api.sdk.requests.VKRequest
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import kiol.vkapp.commondata.data.VKDocItem
import kiol.vkapp.commondata.data.VKListContainerResponse
import kiol.vkapp.commondata.data.VKResponse
import kiol.vkapp.commondata.data.fromJson
import org.json.JSONObject
import timber.log.Timber
import java.util.*
import kotlin.math.abs

class DocsUseCase {

    fun getDocs(): Flowable<List<DocItem>> {
        val request = VKRequest<JSONObject>("docs.get")
            .addParam("return_tags", 1)

        return Flowable.fromCallable {
            VK.executeSync(request)
        }.map {
            val docsResponse: VKResponse<VKListContainerResponse<VKDocItem>> = Gson().fromJson(it.toString())
            docsResponse.response.items.map {
                DocItem(
                    it.id, it.owner_id,
                    it.title,
                    "${it.ext
                        .toUpperCase()} · ${humanReadableByteCountSI(it.size)} · ${humanReadableTimestamp(it.date * 1000)}",
                    it.tags
                        ?.joinToString(", ") ?: "",
                    it.getType(),
                    it.preview?.photo, it.url
                )
            }
        }.subscribeOn(Schedulers.io())
    }

    fun removeDoc(docItem: DocItem) {
        val request = VKRequest<JSONObject>("docs.delete")
            .addParam("owner_id", docItem.owner_id)
            .addParam("doc_id", docItem.id)

        VK.execute(request, object : VKApiCallback<JSONObject> {
            override fun fail(error: VKApiExecutionException) {
                Timber.e("removeDoc failed $error")
            }

            override fun success(result: JSONObject) {
                Timber.d("removeDoc success $result")
            }
        })
    }

    fun updateDocName(docItem: DocItem) {
        val request = VKRequest<JSONObject>("docs.edit")
            .addParam("owner_id", docItem.owner_id)
            .addParam("doc_id", docItem.id)
            .addParam("title", docItem.docTitle)

        VK.execute(request, object : VKApiCallback<JSONObject> {
            override fun fail(error: VKApiExecutionException) {
                Timber.e("updateDocName failed $error")
            }

            override fun success(result: JSONObject) {
                Timber.d("updateDocName success $result")
            }
        })
    }

    private fun humanReadableTimestamp(timestamp: Long): String {
        val nowTime: Calendar = Calendar.getInstance()
        val neededTime: Calendar = Calendar.getInstance()
        neededTime.timeInMillis = timestamp

        return if (neededTime.get(Calendar.YEAR) == nowTime.get(Calendar.YEAR)) {
            when {
                nowTime.get(Calendar.DATE) == neededTime.get(Calendar.DATE) -> {
                    "Сегодня"
                }
                nowTime.get(Calendar.DATE) - neededTime.get(Calendar.DATE) == 1 -> {
                    "Вчера"
                }
                else -> {
                    DateFormat.format("dd MMMM", neededTime).toString()
                }
            }
        } else {
            DateFormat.format("yyyy dd MMMM", neededTime).toString()
        }
    }

    private fun humanReadableByteCountSI(bytes: Long): String? {
        val s = if (bytes < 0) "-" else ""
        var b =
            if (bytes == Long.MIN_VALUE) Long.MAX_VALUE else abs(bytes)
        return if (b < 1000L) "$bytes Б" else if (b < 999950L) String.format(
            "%s%.1f КБ",
            s,
            b / 1e3
        ) else if (1000.let { b /= it; b } < 999950L) String.format(
            "%s%.1f МБ",
            s,
            b / 1e3
        ) else if (1000.let { b /= it; b } < 999950L) String.format(
            "%s%.1f ГБ",
            s,
            b / 1e3
        ) else if (1000.let { b /= it; b } < 999950L) String.format(
            "%s%.1f ТБ",
            s,
            b / 1e3
        ) else if (1000.let { b /= it; b } < 999950L) String.format(
            "%s%.1f ПБ",
            s,
            b / 1e3
        ) else String.format("%s%.1f ЭБ", s, b / 1e6)
    }
}