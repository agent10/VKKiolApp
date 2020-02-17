package kiol.vkapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.vk.api.sdk.VK
import io.reactivex.disposables.CompositeDisposable
import kiol.vkapp.commondata.domain.DocItem
import kiol.vkapp.utils.plusAssign
import timber.log.Timber

class MainFragment : Fragment(R.layout.main_fragment_layout) {

    private val viewModel: MainViewModel by viewModels()

    lateinit var docs: RecyclerView
    lateinit var adapter: DocsAdapter

    lateinit var contentViewer: FrameLayout

    private lateinit var swiper: SwipeRefreshLayout

    private val compositeDisposable = CompositeDisposable()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        contentViewer = view.findViewById(R.id.contentViewer)
        docs = view.findViewById(R.id.docs)
        swiper = view.findViewById(R.id.swipetorefresh)


        swiper.setOnRefreshListener {
            viewModel.loadDocs()
        }

        adapter = DocsAdapter({ vh, item ->
            vh.imageTv.setOnClickListener {
                openContent(item)
            }

            vh.menuBtn.setOnClickListener {
                showPopup(it, item, adapter)
            }
        }, {
            val willLoad = viewModel.loadMoreDocs()
            if (willLoad) {
                adapter.refreshing = true
            }
        })

        docs.adapter = adapter

        compositeDisposable += viewModel.uiEvents.subscribe({
            swiper.isRefreshing = false
            when (it) {
                is MainViewModel.UIEvent.DocsLoaded -> {
                    Timber.d("docs loaded $it")
                    adapter.refreshing = it.hasMore
                    adapter.submitList(it.items) {
                        if (it.initial) {
                            docs.scrollToPosition(0)
                        }
                    }

                }
                is MainViewModel.UIEvent.DocsError -> {
                    Timber.e("docs loading failed $it")
                    adapter.refreshing = false
                    Toast.makeText(requireContext(), "Loading error", Toast.LENGTH_SHORT).show()
                }
            }
        }, {
            Timber.e("Should never happened: $it")
        })

        if (VK.isLoggedIn()) {
            swiper.isRefreshing = true
            viewModel.loadDocs()
        }
    }

    private fun openContent(docItem: DocItem) {
        val contentViewerFactory = ContentViewerFactory()
        try {
            contentViewerFactory.create(docItem)
            childFragmentManager.beginTransaction()
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

            val builder = AlertDialog.Builder(requireContext())
                .setTitle("Скачивание")
                .setMessage("Хотите скачать файл?")
                .setPositiveButton("Скачать") { _, _ ->
                    viewModel.downloadDoc(docItem)
                }
                .setNegativeButton("Отмена", null)
            builder.show()
        } catch (e: Exception) {
            Timber.e("Can't create content viewer, $e")
        }
    }

    private fun showPopup(v: View, docItem: DocItem, adapter: DocsAdapter) {
        PopupMenu(requireContext(), v).apply {
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.renameMenu -> {
                        showRenameDialog(docItem, adapter)
                    }
                    R.id.removeMenu -> {
                        val newList = viewModel.removeDoc(docItem, adapter.currentList)
                        adapter.submitList(newList)
                    }
                }
                true
            }
            inflate(R.menu.doc_item_menu)
            show()
        }
    }

    private fun showRenameDialog(docItem: DocItem, adapter: DocsAdapter) {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.rename_dialog_layout, null)
        val editText = view.findViewById<EditText>(R.id.renameET)
        editText.setText(docItem.docTitle)
        val dialog: AlertDialog = AlertDialog.Builder(requireContext())
            .setTitle("Название документа")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val newList = viewModel.renameDoc(editText.text.toString(), docItem, adapter.currentList)
                adapter.submitList(newList)
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
    }
}