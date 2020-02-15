package kiol.vkapp.viewers

import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.P
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment
import coil.ImageLoader
import coil.api.load
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import kiol.vkapp.R
import kiol.vkapp.ViewerNotAvailable
import kiol.vkapp.commondata.domain.DocItem

class ImageViewerFragment : Fragment(R.layout.image_viewer_fragment_layout) {
    companion object {
        const val URL = "doc_url"
        fun create(docItem: DocItem): ImageViewerFragment {
            if (!listOf("jpg", "png", "gif").contains(docItem.type.ext)) {
                throw ViewerNotAvailable(docItem)
            }
            return ImageViewerFragment().apply {
                arguments = Bundle().apply {
                    putString(URL, docItem.contentUrl)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val imageLoader = ImageLoader(requireContext()) {
            componentRegistry {
                if (SDK_INT >= P) {
                    add(ImageDecoderDecoder())
                } else {
                    add(GifDecoder())
                }
            }
        }
        arguments?.getString(URL)?.let {
            view.findViewById<ImageView>(R.id.photo_view).load(it, imageLoader)
        }
    }
}