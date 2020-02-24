package kiol.vkapp.taskc

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction

fun Fragment.getSimpleRouter() = (requireActivity() as MainActivity).getRouter()

class SimpleRouter(private val fragmentManager: FragmentManager) {

    fun routeToEditor() {
        //        beginDefTransaction().replace(R.id.contentViewer, VideoEditorFragment()).addToBackStack(null).commit()
    }

    fun routeToCamera() {
        //        beginDefTransaction().replace(R.id.contentViewer, CameraContainerFragment()).commit()
    }

    private fun beginDefTransaction(): FragmentTransaction {
        return fragmentManager.beginTransaction()
    }
}