package kiol.vkapp.viewers

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kiol.vkapp.R
import kiol.vkapp.ViewerNotAvailable
import kiol.vkapp.commondata.domain.DocItem
import java.io.BufferedReader
import java.io.InputStreamReader


class TextViewerFragment : Fragment(R.layout.text_viewer_fragment_layout) {
    companion object {
        const val URL = "doc_url"
        fun create(docItem: DocItem): TextViewerFragment {
            if (docItem.type.ext != "txt") {
                throw ViewerNotAvailable(docItem)
            }
            return TextViewerFragment().apply {
                arguments = Bundle().apply {
                    putString(URL, docItem.contentUrl)
                }
            }
        }
    }

    private val disposable = CompositeDisposable()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.getString(URL)?.let {

            disposable.add(Flowable.fromCallable {
                val url = java.net.URL(it)
                val reader = BufferedReader(InputStreamReader(url.openStream()))
                var content = ""
                reader.use { r ->
                    content = r.readText()
                }
                content
            }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe({
                view.findViewById<TextView>(R.id.text).text = it
            }, {
                Toast.makeText(requireContext(), "Не удалось загрузить файл", Toast.LENGTH_SHORT).show()
            }))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        disposable.clear()
    }
}