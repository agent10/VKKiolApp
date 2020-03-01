package kiol.vkapp.docs.viewers

import android.os.Bundle
import android.view.View
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView
import kiol.vkapp.docs.ViewerNotAvailable
import kiol.vkapp.commondata.domain.DocItem
import kiol.vkapp.docs.R
import kiol.vkapp.docs.widgets.DragToDismissFrameLayout

class ImageViewerFragment : Fragment(R.layout.image_viewer_fragment_layout) {
    companion object {
        const val URL = "doc_url"
        private val supportedFormats = listOf("jpg", "png", "gif")
        fun create(docItem: DocItem): ImageViewerFragment {
            if (!supportedFormats.contains(docItem.type.ext)) {
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

        val dismissFrameLayout = view.findViewById<DragToDismissFrameLayout>(R.id.dismissLayout)
        val photoView = view.findViewById<PhotoView>(R.id.photo_view)
        photoView.setOnApplyWindowInsetsListener { v, insets ->
            view.updatePadding(bottom = insets.systemWindowInsetBottom)
            insets
        }

        photoView.setOnScaleChangeListener { _, _, _ ->
            photoView.setAllowParentInterceptOnEdge(photoView.scale <= 1.0f)
        }

        dismissFrameLayout.dissmissHandler = {
            parentFragmentManager.popBackStack()
        }

        arguments?.getString(URL)?.let {
            val circularProgressDrawable = CircularProgressDrawable(requireContext()).apply {
                setStyle(CircularProgressDrawable.DEFAULT)
                start()
            }

            Glide.with(this).load(it).placeholder(circularProgressDrawable).error(R.drawable.ic_error_image)
                .into(view.findViewById(R.id.photo_view))
        }
    }
}