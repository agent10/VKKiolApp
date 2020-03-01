package kiol.vkapp.docs

import android.Manifest
import android.content.Intent
import android.opengl.Visibility
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.vk.api.sdk.VK
import io.reactivex.disposables.CompositeDisposable
import kiol.vkapp.commondata.domain.DocItem
import kiol.vkapp.commonui.permissions.PermissionManager
import kiol.vkapp.commonui.plusAssign
import kiol.vkapp.docs.R
import timber.log.Timber

class MainFragment : Fragment(R.layout.main_fragment_layout) {

    private val viewModel: MainViewModel by viewModels()

    lateinit var docs: RecyclerView
    lateinit var adapter: DocsAdapter

    lateinit var contentViewer: FrameLayout

    private lateinit var swiper: SwipeRefreshLayout

    private lateinit var permissionManager: PermissionManager

    private val compositeDisposable = CompositeDisposable()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        permissionManager = PermissionManager(requireContext(), listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE))

        contentViewer = view.findViewById(R.id.contentViewer)
        docs = view.findViewById(R.id.docs)
        swiper = view.findViewById(R.id.swipetorefresh)
        val notFoundHint = view.findViewById<TextView>(R.id.notFoundHint)

        view.findViewById<Toolbar>(R.id.toolbar).apply {
            setOnApplyWindowInsetsListener { v, insets ->
                view.updatePadding(top = insets.systemWindowInsetTop)
                insets
            }
        }

        swiper.setOnRefreshListener {
            viewModel.loadDocs()
        }

        adapter = DocsAdapter({ vh, item ->
            vh.root.setOnClickListener {
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
                            adapter.refreshing = it.items.isNotEmpty() && it.hasMore
                            notFoundHint.visibility = if (it.items.isEmpty()) View.VISIBLE else View.INVISIBLE
                        }
                    }

                }
                is MainViewModel.UIEvent.DocsError -> {
                    Timber.e("docs loading failed $it")
                    adapter.refreshing = false
                    Toast.makeText(requireContext(), R.string.docs_loading_error, Toast.LENGTH_SHORT).show()
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

            permissionManager.checkPermissions(this) {
                if (it) {
                    val builder = AlertDialog.Builder(requireContext())
                        .setTitle(R.string.download_dialog_title)
                        .setMessage(R.string.download_dialog_msg)
                        .setPositiveButton(R.string.download_dialog_ok) { _, _ ->
                            viewModel.downloadDoc(docItem)
                        }
                        .setNegativeButton(R.string.common_cancel, null)
                    builder.show()
                } else {
                    Toast.makeText(requireContext(), R.string.no_perms_for_download, Toast.LENGTH_SHORT).show()
                }
            }
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
            .setTitle(R.string.rename_dialog_title)
            .setView(view)
            .setPositiveButton(R.string.rename_dialog_save) { _, _ ->
                val newList = viewModel.renameDoc(editText.text.toString(), docItem, adapter.currentList)
                adapter.submitList(newList)
            }
            .setNegativeButton(R.string.common_cancel, null)
            .create()
        dialog.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        permissionManager.invokePermissionRequest(requestCode, permissions, grantResults)
    }
}