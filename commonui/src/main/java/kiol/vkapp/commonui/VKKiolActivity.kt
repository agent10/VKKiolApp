package kiol.vkapp.commonui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.vk.api.sdk.VK
import com.vk.api.sdk.VKApiConfig
import com.vk.api.sdk.auth.VKAccessToken
import com.vk.api.sdk.auth.VKAuthCallback
import com.vk.api.sdk.auth.VKScope
import ltd.abtech.commonui.R
import timber.log.Timber

abstract class VKKiolActivity : AppCompatActivity() {

    private lateinit var vkBtn: Button
    private lateinit var vkDesc: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.vkkiol_activity)

        vkDesc = findViewById(R.id.vkinfo)
        vkDesc.text = getTaskDescription()

        vkBtn = findViewById(R.id.vklogin)
        vkBtn.setOnClickListener {
            VK.login(this, getVKScopes())
        }

        val config = getCustomVKConfig()
        config?.let {
            VK.setConfig(it)
        }

        checkIsVKLoggedIn(savedInstanceState)
    }

    private fun checkIsVKLoggedIn(savedInstanceState: Bundle?) {
        if (VK.isLoggedIn()) {
            if (savedInstanceState == null) {
                hideVkControls()
                startMainFragment(R.id.contentViewer)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val callback = object : VKAuthCallback {
            override fun onLogin(token: VKAccessToken) {
                hideVkControls()
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

    open fun getTaskDescription() = ""

    abstract fun getVKScopes(): Collection<VKScope>

    abstract fun startMainFragment(containerId: Int)

    open fun getCustomVKConfig(): VKApiConfig? = null

    private fun hideVkControls() {
        vkDesc.visibility = View.GONE
        vkBtn.visibility = View.GONE
    }
}