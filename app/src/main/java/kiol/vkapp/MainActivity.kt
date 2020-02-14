package kiol.vkapp

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import coil.api.load
import com.vk.api.sdk.VK
import com.vk.api.sdk.auth.VKAccessToken
import com.vk.api.sdk.auth.VKAuthCallback
import io.reactivex.Flowable
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.Timed
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import timber.log.Timber

data class SampleData(val userId: Int, val title: String)

interface SampleRetrofit {
    @GET("posts")
    fun getSample(): Flowable<List<SampleData>>
}

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val retrofit = Retrofit.Builder()
            .baseUrl("https://jsonplaceholder.typicode.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()

        val d = retrofit.create(SampleRetrofit::class.java).getSample().subscribeOn(Schedulers.io()).observeOn(
            AndroidSchedulers
                .mainThread()
        ).subscribe({
            Timber.d("Test: $it")
        }, {
            Timber.e("Error: $it")
        })


        findViewById<ImageView>(R.id.image).load("https://www.google.com/images/branding/googlelogo/2x/googlelogo_color_272x92dp.png")

        //        Timber.d("Test log")
        //VK.login(this, arrayListOf(VKScope.WALL))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val callback = object : VKAuthCallback {
            override fun onLogin(token: VKAccessToken) {
                // User passed authorization
            }

            override fun onLoginFailed(errorCode: Int) {
                // User didn't pass authorization
            }
        }
        if (data == null || !VK.onActivityResult(requestCode, resultCode, data, callback)) {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}
