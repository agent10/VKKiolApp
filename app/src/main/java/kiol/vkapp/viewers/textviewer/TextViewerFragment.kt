package kiol.vkapp.viewers.textviewer

import android.app.Application
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.AndroidViewModel
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kiol.vkapp.R
import kiol.vkapp.ViewerNotAvailable
import kiol.vkapp.commondata.domain.DocItem
import kiol.vkapp.utils.SimpleTxtFileLoader
import java.util.*


class TextViewerViewModel(app: Application) : AndroidViewModel(app) {

    private val fileLoader = SimpleTxtFileLoader(app)

    fun load(url: String, docId: Int): Flowable<List<String>> {
        return fileLoader.loadDocTxtFile(url, docId).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    private fun SimpleTxtFileLoader.loadDocTxtFile(url: String, docId: Int): Flowable<List<String>> {
        return loadTxtFile(url, UUID.nameUUIDFromBytes(docId.toString().toByteArray()).toString() + "vkkiol")
    }
}

class TextViewerFragment : Fragment(R.layout.text_viewer_fragment_layout) {
    companion object {
        const val URL = "doc_url"
        const val ID = "doc_id"

        fun create(docItem: DocItem): TextViewerFragment {
            if (docItem.type.ext != "txt") {
                throw ViewerNotAvailable(docItem)
            }
            return TextViewerFragment().apply {
                arguments = Bundle().apply {
                    putString(URL, docItem.contentUrl)
                    putInt(ID, docItem.id)
                }
            }
        }
    }

    private val viewModel: TextViewerViewModel by viewModels()

    private val disposable = CompositeDisposable()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val progressBar = view.findViewById<ProgressBar>(R.id.progress).apply { visibility = View.VISIBLE }
        val textList = view.findViewById<RecyclerView>(R.id.textList)

        arguments?.let {
            val url = it.getString(URL, "")
            val id = it.getInt(ID, 0)
            disposable.add(viewModel.load(url, id).doOnEach {
                progressBar.visibility = View.INVISIBLE
            }.subscribe({
                val adapter = StringsAdapter(it)
                textList.adapter = adapter
            }, {
                showError(view)
            }))
        } ?: showError(view)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        disposable.clear()
    }

    private fun showError(view: View) {
        view.findViewById<TextView>(R.id.error).apply {
            text = "Не удалось загрузить файл"
            visibility = View.VISIBLE
        }
    }
}