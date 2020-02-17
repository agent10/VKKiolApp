package kiol.vkapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.vk.api.sdk.VK
import com.vk.api.sdk.auth.VKAccessToken
import com.vk.api.sdk.auth.VKAuthCallback
import com.vk.api.sdk.auth.VKScope
import timber.log.Timber


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (VK.isLoggedIn()) {
            if (savedInstanceState == null) {
                startMain()
            }
        } else {
            VK.login(this, arrayListOf(VKScope.DOCS, VKScope.OFFLINE))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val callback = object : VKAuthCallback {
            override fun onLogin(token: VKAccessToken) {
                startMain()
            }

            override fun onLoginFailed(errorCode: Int) {
                Timber.e("onLoginFailed $errorCode")
            }
        }
        if (data == null || !VK.onActivityResult(requestCode, resultCode, data, callback)) {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun startMain() {
        val mainFragment = MainFragment()
        supportFragmentManager.beginTransaction().setPrimaryNavigationFragment(mainFragment)
            .replace(R.id.contentViewer, mainFragment).commitAllowingStateLoss()
    }
}
