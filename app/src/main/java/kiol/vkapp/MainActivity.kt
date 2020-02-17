package kiol.vkapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.vk.api.sdk.VK
import com.vk.api.sdk.auth.VKAccessToken
import com.vk.api.sdk.auth.VKAuthCallback
import com.vk.api.sdk.auth.VKScope
import io.reactivex.android.schedulers.AndroidSchedulers
import kiol.vkapp.commondata.domain.DocItem
import kiol.vkapp.commondata.domain.docs.DocsUseCase
import kotlinx.android.synthetic.main.activity_main.view.*
import timber.log.Timber


class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val docsUseCase = DocsUseCase()

    lateinit var docs: RecyclerView
    lateinit var adapter: DocsAdapter

    lateinit var contentViewer: FrameLayout

    private lateinit var swiper: SwipeRefreshLayout

    private lateinit var docsDownloadManager: DocsDownloadManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        docsDownloadManager = DocsDownloadManager(application)

        contentViewer = findViewById(R.id.contentViewer)

        swiper = findViewById(R.id.swipetorefresh)
        swiper.swipetorefresh
        swiper.setOnRefreshListener {
            getDocs()
        }

        if (VK.isLoggedIn()) {
            swiper.isRefreshing = true
            getDocs()
        } else {
            VK.login(this, arrayListOf(VKScope.DOCS, VKScope.OFFLINE))
        }

        docs = findViewById(R.id.docs)

        adapter = DocsAdapter(docs, { vh, item ->
            vh.imageTv.setOnClickListener {
                openContent(item)
            }

            vh.menuBtn.setOnClickListener {
                showPopup(it, item, adapter)
            }
        }, {
            getMoreDocs()
        })

        docs.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        docsDownloadManager.register()
    }

    override fun onStop() {
        super.onStop()
        docsDownloadManager.unregister()
    }

    private fun openContent(docItem: DocItem) {
        val contentViewerFactory = ContentViewerFactory()
        try {
            contentViewerFactory.create(docItem)
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.viewer_fragment_open_enter,
                    R.anim.viewer_fragment_open_enter,
                    R.anim.viewer_fragment_open_exit,
                    R.anim.viewer_fragment_open_exit
                ).replace(
                    R.id.contentViewer, contentViewerFactory.create(docItem)
                ).addToBackStack(null)
                .commitAllowingStateLoss()
        } catch (e: ViewerNotAvailable) {
            Timber.w("Can't create content viewer, request for download, $e")

            val builder = AlertDialog.Builder(this)
                .setTitle("Скачивание")
                .setMessage("Хотите скачать файл?")
                .setPositiveButton("Скачать") { _, _ ->
                    docsDownloadManager.download(docItem)
                }
                .setNegativeButton("Отмена", null)
            builder.show()
        } catch (e: Exception) {
            Timber.e("Can't create content viewer, $e")
        }
    }

    private fun showPopup(v: View, docItem: DocItem, adapter: DocsAdapter) {
        PopupMenu(this, v).apply {
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.renameMenu -> {
                        showRenameDialog(docItem, adapter)
                    }
                    R.id.removeMenu -> {
                        docsUseCase.removeDoc(docItem)

                        val nl = adapter.currentList.toMutableList().apply {
                            remove(docItem)
                        }
                        adapter.submitList(nl)
                    }
                }
                true
            }
            inflate(R.menu.doc_item_menu)
            show()
        }
    }

    private fun showRenameDialog(docItem: DocItem, adapter: DocsAdapter) {
        val view = LayoutInflater.from(this).inflate(R.layout.rename_dialog_layout, null)
        val editText = view.findViewById<EditText>(R.id.renameET)
        editText.setText(docItem.docTitle)
        val dialog: AlertDialog = AlertDialog.Builder(this)
            .setTitle("Название документа")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val ndi = docItem.copy(docTitle = editText.text.toString())
                docsUseCase.updateDocName(ndi)
                adapter.submitList(adapter.currentList.toMutableList().apply {
                    set(indexOf(docItem), ndi)
                })
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val callback = object : VKAuthCallback {
            override fun onLogin(token: VKAccessToken) {
                getDocs()
                swiper.isRefreshing = true
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
        val d = docsUseCase.getDocs().observeOn(AndroidSchedulers.mainThread()).doOnComplete {
            swiper.isRefreshing = false
        }.subscribe({
            Timber.d("getDocs success $it")
            adapter.submitList(it) {
                docs.scrollToPosition(0)
            }
        }, {
            Timber.e("getDocs failed $it")
        })
    }

    private fun getMoreDocs() {
        if (docsUseCase.canLoadMore()) {
            adapter.refreshing = true
            val d = docsUseCase.loadMore().observeOn(AndroidSchedulers.mainThread()).doOnComplete {
                swiper.isRefreshing = false
                if (!docsUseCase.canLoadMore()) {
                    adapter.refreshing = false
                }
            }.subscribe({
                Timber.d("loadMoreDocs success $it")
                adapter.submitList(it)
            }, {
                Timber.e("loadMoreDocs failed $it")
            })
        }
    }


}
