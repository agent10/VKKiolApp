package kiol.vkappvkkiolapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.vk.api.sdk.auth.VKScope
import kiol.vkapp.commonui.VKKiolActivity
import kiol.vkapp.docs.MainFragment

class MainActivity : VKKiolActivity() {

    override fun getTaskDescription() = getString(R.string.app_name)

    override fun getVKScopes() = arrayListOf(VKScope.DOCS, VKScope.OFFLINE)

    override fun startMainFragment(containerId: Int) {
        val mainFragment = EntryFragment()
        supportFragmentManager.beginTransaction().setPrimaryNavigationFragment(mainFragment)
            .replace(containerId, mainFragment).commitAllowingStateLoss()
    }
}
