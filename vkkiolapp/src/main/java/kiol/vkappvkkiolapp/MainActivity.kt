package kiol.vkappvkkiolapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.vk.api.sdk.auth.VKScope
import kiol.vkapp.commonui.VKKiolActivity

class MainActivity : VKKiolActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun getVKScopes() = arrayListOf(VKScope.PHOTOS, VKScope.GROUPS, VKScope.OFFLINE)


    override fun startMainFragment(containerId: Int) {
        //        TODO("Not yet implemented")
    }
}
