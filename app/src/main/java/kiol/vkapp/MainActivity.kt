package kiol.vkapp

import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.doOnLayout
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


data class DocItem(
    val id: Int,
    val owner_id: Int, val docTitle: String, val docInfo: String, val tags: String, val type: Int, val images:
    VKSizesPreview?
)

data class VKDocItem(
    val id: Int,
    val owner_id: Int,
    val title: String, val size: Long, val ext: String, val date: Long, val tags: List<String>?, val type: Int,
    val preview: VKPhotoPreview?
)

data class VKResponse<T>(val response: T)

data class VKListContainerResponse<T>(val count: Int, val items: List<T>)

data class VKImagePreview(val src: String, val width: Int, val height: Int)

data class VKPhotoPreview(val photo: VKSizesPreview?)

data class VKSizesPreview(val sizes: List<VKImagePreview>)

fun ImageView.loadVKImages(vkPreviews: VKSizesPreview?) {
    doOnLayout {
        val tw = measuredWidth
        val th = measuredHeight

        var best = vkPreviews?.sizes?.firstOrNull()
        vkPreviews?.sizes?.forEach {
            val ib = best
            if (ib == null) {
                best = it
            } else {
                if (it.width >= it.height) {
                    if (it.width <= tw && it.width > ib.width) best = it
                } else {
                    if (it.height <= th && it.height > ib.height) best = it
                }
            }
        }

        best?.let {
            load(it.src)
        }
    }
}


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
            val menuBtn = findViewById<ImageButton>(R.id.docMenuBtn)
            bind {
                imageTv.loadVKImages(item.images)

                menuBtn.setOnClickListener {
                    showPopup(it, item, adapter)
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

    private fun showPopup(v: View, docItem: DocItem, adapter: ListDelegationAdapter<List<DocItem>>) {
        PopupMenu(this, v).apply {
            // MainActivity implements OnMenuItemClickListener
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.renameMenu -> {
                        showRenameDialog(docItem, adapter)
                    }
                    R.id.removeMenu -> {
                        removeDoc(docItem)
                        val index = adapter.items.indexOf(docItem)
                        (adapter.items as MutableList).removeAt(index)
                        adapter.notifyItemRemoved(index)
                    }
                }
                true
            }
            inflate(R.menu.doc_item_menu)
            show()
        }
    }

    private fun showRenameDialog(docItem: DocItem, adapter: ListDelegationAdapter<List<DocItem>>) {
        val taskEditText = EditText(this)
        taskEditText.setText(docItem.docTitle)
        val dialog: AlertDialog = AlertDialog.Builder(this)
            .setTitle("Название документа")
            .setView(taskEditText)
            .setPositiveButton("Save") { _, _ ->
                val index = adapter.items.indexOf(docItem)
                val newDocItem = docItem.copy(docTitle = taskEditText.text.toString())
                (adapter.items as MutableList)[index] = newDocItem
                adapter.notifyItemChanged(index)
                updateDocName(newDocItem)

            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
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
                        it.id, it.owner_id,
                        it.title,
                        "${it.ext
                            .toUpperCase()} · ${humanReadableByteCountSI(it.size)} · ${humanReadableTimestamp(it.date * 1000)}",
                        it.tags
                            ?.joinToString(", ") ?: "",
                        0,
                        it.preview?.photo

                    )
                }
                adapter.notifyDataSetChanged()
            }
        })
    }

    private fun removeDoc(docItem: DocItem) {
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

    private fun updateDocName(docItem: DocItem) {
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
