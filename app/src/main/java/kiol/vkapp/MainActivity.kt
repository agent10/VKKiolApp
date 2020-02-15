package kiol.vkapp

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.recyclerview.widget.RecyclerView
import coil.api.load
import com.hannesdorfmann.adapterdelegates4.ListDelegationAdapter
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegate
import com.vk.api.sdk.VK
import com.vk.api.sdk.auth.VKAccessToken
import com.vk.api.sdk.auth.VKAuthCallback
import com.vk.api.sdk.auth.VKScope
import io.reactivex.android.schedulers.AndroidSchedulers
import kiol.vkapp.commondata.data.VKDocItem
import kiol.vkapp.commondata.data.VKDocItem.VKDocType.*
import kiol.vkapp.commondata.domain.DocsUseCase
import kiol.vkapp.commondata.data.VKSizesPreview
import kiol.vkapp.commondata.domain.DocItem
import timber.log.Timber


fun ImageView.setVKPreview(docItem: DocItem) {
    val bgdColor = when (docItem.type) {
        Text, Audio, Unknown, Image, Gif -> R.color.doc_type_color_1
        Zip -> R.color.doc_type_color_2
        Video -> R.color.doc_type_color_3
        Ebook -> R.color.doc_type_color_4
    }

    val icon = when (docItem.type) {
        Text -> R.drawable.ic_doc_type_text
        Audio -> R.drawable.ic_doc_type_audio
        Ebook -> R.drawable.ic_doc_type_ebook
        Video -> R.drawable.ic_doc_type_video
        Zip -> R.drawable.ic_doc_type_zip
        else -> R.drawable.ic_doc_type_other
    }

    val shapeDrawable = ContextCompat.getDrawable(context, R.drawable.doc_image_bgd) as GradientDrawable
    shapeDrawable.color = ColorStateList.valueOf(ContextCompat.getColor(context, bgdColor))
    background = shapeDrawable

    val vkPreviews = docItem.images
    vkPreviews?.let {
        doOnLayout {
            val tw = measuredWidth
            val th = measuredHeight

            scaleType = ImageView.ScaleType.CENTER_CROP

            var best = vkPreviews.sizes.firstOrNull()
            vkPreviews.sizes.forEach {
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
    } ?: run {
        scaleType = ImageView.ScaleType.CENTER_INSIDE
        setImageResource(icon)
    }
}


class MainActivity : AppCompatActivity() {

    lateinit var docsUseCase: DocsUseCase

    lateinit var adapter: ListDelegationAdapter<List<DocItem>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (VK.isLoggedIn()) {
            getDocs()
        } else {
            VK.login(this, arrayListOf(VKScope.DOCS, VKScope.OFFLINE))
        }

        docsUseCase = DocsUseCase()

        val docs = findViewById<RecyclerView>(R.id.docs)

        adapter = ListDelegationAdapter<List<DocItem>>(adapterDelegate(R.layout.doc_item_layout) {

            val titleTv = findViewById<TextView>(R.id.docTitleTv)
            val imageTv = findViewById<ImageView>(R.id.docImageIv)
            val infoTv = findViewById<TextView>(R.id.docInfoTv)
            val tagsTv = findViewById<TextView>(R.id.docTagsTv)
            val menuBtn = findViewById<ImageButton>(R.id.docMenuBtn)
            bind {
                imageTv.setVKPreview(item)


                imageTv.clipToOutline = true

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
                        docsUseCase.removeDoc(docItem)
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
        val view = LayoutInflater.from(this).inflate(R.layout.rename_dialog_layout, null)
        val editText = view.findViewById<EditText>(R.id.renameET)
        editText.setText(docItem.docTitle)
        val dialog: AlertDialog = AlertDialog.Builder(this)
            .setTitle("Название документа")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val index = adapter.items.indexOf(docItem)
                val newDocItem = docItem.copy(docTitle = editText.text.toString())
                (adapter.items as MutableList)[index] = newDocItem
                adapter.notifyItemChanged(index)
                docsUseCase.updateDocName(newDocItem)

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
        val d = DocsUseCase().getDocs().observeOn(AndroidSchedulers.mainThread()).subscribe({
            Timber.d("getDocs success $it")
            adapter.items = it
            adapter.notifyDataSetChanged()
        }, {
            Timber.e("getDocs failed $it")
        })
    }
}
