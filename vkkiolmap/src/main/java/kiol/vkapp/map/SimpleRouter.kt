package kiol.vkapp.map

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import kiol.vkapp.map.addbox.AddBoxFragment

fun Fragment.getSimpleRouter() = (requireActivity() as RouterProvider).provideSimpleRouter()


interface RouterProvider {
    fun provideSimpleRouter(): SimpleRouter
}

class SimpleRouter(private val fragmentManager: FragmentManager) {

    fun routeToCamera() {
        beginDefTransaction().add(R.id.contentViewer, AddBoxFragment.create()).addToBackStack(null).commit()
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