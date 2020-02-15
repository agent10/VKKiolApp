package kiol.vkapp

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.widget.*
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
import kiol.vkapp.commondata.data.VKDocItem.VKDocType.*
import kiol.vkapp.commondata.domain.DocItem
import kiol.vkapp.commondata.domain.DocsUseCase
import timber.log.Timber


fun ImageView.setVKPreview(docItem: DocItem) {
    val bgdColor = when (docItem.type) {
        is Text, is Audio, is Unknown, is Image, is Gif -> R.color.doc_type_color_1
        is Zip -> R.color.doc_type_color_2
        is Video -> R.color.doc_type_color_3
        is Ebook -> R.color.doc_type_color_4
    }

    val icon = when (docItem.type) {
        is Text -> R.drawable.ic_doc_type_text
        is Audio -> R.drawable.ic_doc_type_audio
        is Ebook -> R.drawable.ic_doc_type_ebook
        is Video -> R.drawable.ic_doc_type_video
        is Zip -> R.drawable.ic_doc_type_zip
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

    lateinit var contentViewer: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        contentViewer = findViewById(R.id.contentViewer)

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

                imageTv.setOnClickListener {
                    openContent(item)
                }

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

    private fun openContent(docItem: DocItem) {
        val contentViewerFactory = ContentViewerFactory()
        try {
            contentViewerFactory.create(docItem)
            supportFragmentManager.beginTransaction().replace(
                R.id.contentViewer, contentViewerFactory.create(docItem)
            ).commitAllowingStateLoss()
        } catch (e: Exception) {
            Timber.w("Can't create content viewer, $e")
            val request =
                DownloadManager.Request(Uri.parse(docItem.contentUrl))
                    .setTitle(docItem.docTitle)
                    .setDescription("Downloading")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, docItem.docTitle)
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(false)
            val downloadID = (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
            Toast.makeText(this, "Start downloading", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        if (supportFragmentManager.fragments.isNotEmpty()) {
            supportFragmentManager.beginTransaction().remove(supportFragmentManager.fragments.first())
                .commitAllowingStateLoss()
        } else {
            super.onBackPressed()
        }
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
