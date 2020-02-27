package kiol.vkapp.taskc

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.vk.api.sdk.VK
import com.vk.api.sdk.VKApiConfig
import com.vk.api.sdk.auth.VKAccessToken
import com.vk.api.sdk.auth.VKAuthCallback
import com.vk.api.sdk.auth.VKScope
import timber.log.Timber

abstract class VKKiolActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.vkkiol_activity)

        val config = getCustomVKConfig()
        config?.let {
            VK.setConfig(it)
        }

        checkIsVKLoggedIn(savedInstanceState)

        findViewById<Button>(R.id.vklogin).setOnClickListener {
            VK.login(this, getVKScopes())

        }
    }

    private fun checkIsVKLoggedIn(savedInstanceState: Bundle?) {
        if (VK.isLoggedIn()) {
            if (savedInstanceState == null) {
                startMainFragment(R.id.contentViewer)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val callback = object : VKAuthCallback {
            override fun onLogin(token: VKAccessToken) {
                startMainFragment(R.id.contentViewer)
            }

            override fun onLoginFailed(errorCode: Int) {
                Toast.makeText(this@VKKiolActivity, "VKError $errorCode", Toast.LENGTH_SHORT).show()
                Timber.e("onLoginFailed $errorCode")
            }
        }
        if (data == null || !VK.onActivityResult(requestCode, resultCode, data, callback)) {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    abstract fun getVKScopes(): Collection<VKScope>

    abstract fun startMainFragment(containerId: Int)

    open fun getCustomVKConfig(): VKApiConfig? = null
}