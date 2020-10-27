package kiol.vkappvkkiolapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.vk.api.sdk.VKApiConfig
import com.vk.api.sdk.VKDefaultValidationHandler
import com.vk.api.sdk.auth.VKScope
import kiol.vkapp.commonui.VKKiolActivity
import kiol.vkapp.docs.MainFragment
import kiol.vkapp.map.GMapFragment
import kiol.vkapp.map.RouterProvider
import kiol.vkapp.map.SimpleRouter

class MainActivity : VKKiolActivity(), RouterProvider {

    private lateinit var simpleRouter: SimpleRouter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        simpleRouter = SimpleRouter(supportFragmentManager)
    }

    override fun getTaskDescription() = getString(R.string.app_name)

    override fun getVKScopes() = arrayListOf(VKScope.DOCS, VKScope.PHOTOS, VKScope.GROUPS, VKScope.OFFLINE)

    override fun startMainFragment(containerId: Int) {
        val mainFragment = GMapFragment()
        supportFragmentManager.beginTransaction().setPrimaryNavigationFragment(mainFragment)
            .replace(containerId, mainFragment).commitAllowingStateLoss()
    }

    //Need to override VK api version to use "place" field for groups.get
    //It doesn't work in 5.103 for me
    override fun getCustomVKConfig() = VKApiConfig(
        context = this,
        appId = resources.getInteger(kiol.vkapp.map.R.integer.com_vk_sdk_AppId),
        version = "5.61",
        validationHandler = VKDefaultValidationHandler(this)
    )

    fun getRouter() = simpleRouter
    override fun provideSimpleRouter(): SimpleRouter {
        return simpleRouter
    }
}
