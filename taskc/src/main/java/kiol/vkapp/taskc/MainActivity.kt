package kiol.vkapp.taskc

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.vk.api.sdk.VKApiConfig
import com.vk.api.sdk.VKDefaultValidationHandler
import com.vk.api.sdk.auth.VKScope
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable


operator fun CompositeDisposable.plusAssign(d: Disposable) {
    this.add(d)
}

fun Fragment.getAppContext() = requireContext().applicationContext


class MainActivity : VKKiolActivity() {

    private lateinit var simpleRouter: SimpleRouter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        simpleRouter = SimpleRouter(supportFragmentManager)

    }

    override fun getVKScopes() = arrayListOf(VKScope.PHOTOS, VKScope.GROUPS, VKScope.OFFLINE)

    override fun getCustomVKConfig() = VKApiConfig(
        context = this,
        appId = 7334398,
        version = "5.61",
        validationHandler = VKDefaultValidationHandler(this)
    )

    override fun startMainFragment(containerId: Int) {
        supportFragmentManager.beginTransaction().replace(containerId, GMapFragment()).commitAllowingStateLoss()
    }


    fun getRouter() = simpleRouter
}
