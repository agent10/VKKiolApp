package kiol.vkapp.cam

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import kiol.vkapp.cam.camera.CameraContainerFragment
import kiol.vkapp.cam.editor.VideoEditorFragment

fun Fragment.getSimpleRouter() = (requireActivity() as MainActivity).getRouter()

class SimpleRouter(private val fragmentManager: FragmentManager) {

    fun routeToEditor() {
        beginDefTransaction().replace(R.id.contentViewer, VideoEditorFragment()).addToBackStack(null).commit()
    }

    fun routeToCamera() {
        beginDefTransaction().replace(R.id.contentViewer, CameraContainerFragment()).commit()
    }

    private fun beginDefTransaction(): FragmentTransaction {
        return fragmentManager.beginTransaction().setCustomAnimations(
            R.anim.viewer_fragment_open_enter,
            R.anim.viewer_fragment_open_enter,
            R.anim.viewer_fragment_open_exit,
            R.anim.viewer_fragment_open_exit
        )
    }
}