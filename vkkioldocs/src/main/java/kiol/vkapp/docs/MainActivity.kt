package kiol.vkapp.docs

import com.vk.api.sdk.auth.VKScope
import kiol.vkapp.commonui.VKKiolActivity

class MainActivity : VKKiolActivity() {


    override fun getVKScopes() = arrayListOf(VKScope.DOCS, VKScope.OFFLINE)

    override fun startMainFragment(containerId: Int) {
        val mainFragment = MainFragment()
        supportFragmentManager.beginTransaction().setPrimaryNavigationFragment(mainFragment)
            .replace(containerId, mainFragment).commitAllowingStateLoss()
    }

}
