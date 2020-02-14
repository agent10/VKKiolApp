package kiol.vkapp

import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import coil.api.load
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hannesdorfmann.adapterdelegates4.ListDelegationAdapter
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegate
import com.vk.api.sdk.VK
import com.vk.api.sdk.VKApiCallback
import com.vk.api.sdk.auth.VKAccessToken
import com.vk.api.sdk.auth.VKAuthCallback
import com.vk.api.sdk.auth.VKScope
import com.vk.api.sdk.exceptions.VKApiExecutionException
import com.vk.api.sdk.requests.VKRequest
import org.json.JSONObject
import timber.log.Timber
import java.util.*
import kotlin.math.abs


data class DocItem(val docTitle: String, val docInfo: String, val tags: String, val type: Int, val imageTemp: String)

data class VKDocItem(
    val title: String, val size: Long, val ext: String, val date: Long, val tags: List<String>?, val type: Int,
    val preview: VKPhotoPreview?
)

data class VKResponse<T>(val response: T)

data class VKListContainerResponse<T>(val count: Int, val items: List<T>)

data class VKImagePreview(val src: String, val width: Int, val height: Int)

data class VKPhotoPreview(val photo: VKSizesPreview?)

data class VKSizesPreview(val sizes: List<VKImagePreview>)

class MainActivity : AppCompatActivity() {

    lateinit var adapter: ListDelegationAdapter<List<DocItem>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (VK.isLoggedIn()) {
            getDocs()
        } else {
            VK.login(this, arrayListOf(VKScope.DOCS))
        }

        val docs = findViewById<RecyclerView>(R.id.docs)

        adapter = ListDelegationAdapter<List<DocItem>>(adapterDelegate(R.layout.doc_item_layout) {
            val titleTv = findViewById<TextView>(R.id.docTitleTv)
            val imageTv = findViewById<ImageView>(R.id.docImageIv)
            val infoTv = findViewById<TextView>(R.id.docInfoTv)
            val tagsTv = findViewById<TextView>(R.id.docTagsTv)
            bind {
                if (item.imageTemp.isNotEmpty()) {
                    imageTv.load(item.imageTemp)
                }
                titleTv.text = item.docTitle
                infoTv.text = item.docInfo
                if (item.tags.isEmpty()) {
                    tagsTv.visibility = View.GONE
                } else {
                    tagsTv.text = item.tags
                    tagsTv.visibility = View.VISIBLE
                }
            }
        })
        docs.adapter = adapter
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val callback = object : VKAuthCallback {
            override fun onLogin(token: VKAccessToken) {
                getDocs()
            }

            override fun onLoginFailed(errorCode: Int) {
                Timber.e("onLoginFailed $errorCode")
            }
        }
        if (data == null || !VK.onActivityResult(requestCode, resultCode, data, callback)) {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun getDocs() {
        val request = VKRequest<JSONObject>("docs.get").addParam("return_tags", 1)

        VK.execute(request, object : VKApiCallback<JSONObject> {
            override fun fail(error: VKApiExecutionException) {
                Timber.e("getDocs failed $error")
            }

            override fun success(result: JSONObject) {
                Timber.d("getDocs success $result")

                val docsResponse: VKResponse<VKListContainerResponse<VKDocItem>> = Gson().fromJson(result.toString())
                adapter.items = docsResponse.response.items.map {
                    DocItem(
                        it.title,
                        "${it.ext
                            .toUpperCase()} · ${humanReadableByteCountSI(it.size)} · ${humanReadableTimestamp(it.date * 1000)}",
                        it.tags
                            ?.joinToString(", ") ?: "",
                        0,
                        it.preview?.photo
                            ?.sizes?.firstOrNull()?.src ?: ""
                    )
                }
                adapter.notifyDataSetChanged()
            }
        })
    }

    inline fun <reified T> Gson.fromJson(json: String) = fromJson<T>(json, object : TypeToken<T>() {}.type)

    fun humanReadableTimestamp(timestamp: Long): String {
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

    fun humanReadableByteCountSI(bytes: Long): String? {
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
