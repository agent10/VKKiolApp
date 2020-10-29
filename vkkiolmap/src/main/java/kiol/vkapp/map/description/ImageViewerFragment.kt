package kiol.vkapp.map.description

import android.app.Activity
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView
import kiol.vkapp.commondata.data.getXSize
import kiol.vkapp.commondata.domain.Place
import kiol.vkapp.commonui.DragToDismissFrameLayout
import kiol.vkapp.map.R

class ImageViewerFragment : Fragment(R.layout.image_viewer_fragment_layout) {
    companion object {
        const val URL = "image_url"
        fun create(place: Place): ImageViewerFragment {
            return ImageViewerFragment().apply {
                arguments = Bundle().apply {
                    putString(URL, place.sizes.getXSize())
                }
            }
        }

        fun create(uri: Uri): ImageViewerFragment {
            return ImageViewerFragment().apply {
                arguments = Bundle().apply {
                    putString(URL, uri.toString())
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dismissFrameLayout = view.findViewById<DragToDismissFrameLayout>(R.id.dismissLayout)
        val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
        val photoView = view.findViewById<PhotoView>(R.id.photo_view)

        toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        photoView.setOnScaleChangeListener { _, _, _ ->
            photoView.setAllowParentInterceptOnEdge(photoView.scale <= 1.0f)
        }

        dismissFrameLayout.dissmissHandler = {
            parentFragmentManager.popBackStack()
        }

        arguments?.getString(URL)?.let {
            val circularProgressDrawable = CircularProgressDrawable(requireContext()).apply {
                setColorSchemeColors(Color.WHITE)
                setStyle(CircularProgressDrawable.DEFAULT)
                start()
            }

            Glide.with(this).load(Uri.parse(it)).placeholder(circularProgressDrawable)
                .into(view.findViewById(R.id.photo_view))
        }
    }
}