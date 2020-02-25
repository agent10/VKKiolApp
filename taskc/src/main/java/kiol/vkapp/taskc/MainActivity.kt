package kiol.vkapp.taskc

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.ImageViewTarget
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.vk.api.sdk.VK
import com.vk.api.sdk.VKApiConfig
import com.vk.api.sdk.VKDefaultValidationHandler
import com.vk.api.sdk.auth.VKAccessToken
import com.vk.api.sdk.auth.VKAuthCallback
import com.vk.api.sdk.auth.VKScope
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kiol.vkapp.commondata.domain.photos.getGroups
import timber.log.Timber


operator fun CompositeDisposable.plusAssign(d: Disposable) {
    this.add(d)
}

fun Fragment.getAppContext() = requireContext().applicationContext

class MainActivity : AppCompatActivity() {

    private lateinit var simpleRouter: SimpleRouter

    private lateinit var mapview: MapView

    private lateinit var image: ImageView

    private val CAMERA_TARGET = Point(59.952, 30.318)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MapKitFactory.setApiKey("4f99324d-241f-473e-9274-80297deb2e9f")
        MapKitFactory.initialize(this.applicationContext)
        setContentView(R.layout.activity_main)

        image = findViewById(R.id.testImage)

        VK.setConfig(
            VKApiConfig(
                context = this,
                appId = 7334398,
                version = "5.61",
                validationHandler = VKDefaultValidationHandler(this)
            )
        )
        if (VK.isLoggedIn()) {
            if (savedInstanceState == null) {
                startMain()
            }
        } else {
            VK.login(this, arrayListOf(VKScope.PHOTOS, VKScope.OFFLINE))
        }

        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION
            ), 10
        )

        window?.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        simpleRouter = SimpleRouter(supportFragmentManager)

        mapview = findViewById<View>(R.id.mapview) as MapView
    }


    private fun startMain() {
        val d = getGroups().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe({
            Timber.d("Groups: $it")
            val mapObjects = mapview.getMap().getMapObjects().addCollection()
            it.forEach {
                val placeMark = mapObjects.addPlacemark(Point(it.place.latitude.toDouble(), it.place.longitude.toDouble()))
                Glide.with(this).asBitmap().load(it.place.group_photo).into(object : SimpleTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        placeMark.setIcon(ImageProvider.fromBitmap(resource))
                    }

                })
                Glide.with(this).load(it.place.group_photo).into(image)
            }
            //            mapview.map.move(
            //                    CameraPosition(Point(it.place.latitude.toDouble(), it.place.longitude.toDouble()), 15.0f, 0.0f, 0.0f),
            //                    Animation(Animation.Type.SMOOTH, 0f),
            //                    null
            //                )
        }, {
            Timber.e("Groups error: $it")
        })
    }

    override fun onStop() {
        mapview.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()

    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        mapview.onStart()
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


    fun getRouter() = simpleRouter
}
