package kiol.vkapp.taskb

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import kiol.vkapp.taskb.editor.VideoEditorFragment

fun Fragment.getSimpleRouter() = (requireActivity() as MainActivity).getRouter()

class SimpleRouter(private val fragmentManager: FragmentManager) {

    fun routeToEditor() {
        fragmentManager.beginTransaction()
            .replace(R.id.contentViewer, VideoEditorFragment()).addToBackStack(null).commit()
    }
}