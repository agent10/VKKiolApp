package kiol.vkapp.taskc

import android.app.Activity
import android.graphics.Color
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
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setDarkStatusBar(requireActivity())

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
                setStyle(CircularProgressDrawable.DEFAULT)
                start()
            }

            Glide.with(this).load(it).placeholder(circularProgressDrawable)
                .into(view.findViewById(R.id.photo_view))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        setLightStatusBar(requireActivity())
    }

    private fun setLightStatusBar(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            var flags: Int = activity.window.decorView.systemUiVisibility
            flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            activity.window.decorView.systemUiVisibility = flags
            activity.window.statusBarColor = Color.WHITE
        }
    }

    private fun setDarkStatusBar(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            var flags: Int = activity.window.decorView.systemUiVisibility
            flags =
                flags xor View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            activity.window.decorView.systemUiVisibility = flags
            activity.window.statusBarColor = Color.BLACK
        }
    }
}