package kiol.vkapp.taskc

import com.vk.api.sdk.VKApiConfig
import com.vk.api.sdk.VKDefaultValidationHandler
import com.vk.api.sdk.auth.VKScope
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import kiol.vkapp.commonui.VKKiolActivity


operator fun CompositeDisposable.plusAssign(d: Disposable) {
    this.add(d)
}

class MainActivity : VKKiolActivity() {

    override fun getVKScopes() = arrayListOf(VKScope.PHOTOS, VKScope.GROUPS, VKScope.OFFLINE)


    //Need to override VK api version to use "place" field for groups.get
    //It doesn't work in 5.103 for me
    override fun getCustomVKConfig() = VKApiConfig(
        context = this,
        appId = resources.getInteger(R.integer.com_vk_sdk_AppId),
        version = "5.61",
        validationHandler = VKDefaultValidationHandler(this)
    )

    override fun startMainFragment(containerId: Int) {
        val f = GMapFragment()
        supportFragmentManager.beginTransaction()
            .setPrimaryNavigationFragment(f)
            .replace(containerId, f)
            .commitAllowingStateLoss()
    }
}
