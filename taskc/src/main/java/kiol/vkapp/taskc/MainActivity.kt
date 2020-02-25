package kiol.vkapp.taskc

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.vk.api.sdk.VK
import com.vk.api.sdk.VKApiConfig
import com.vk.api.sdk.VKDefaultValidationHandler
import com.vk.api.sdk.auth.VKAccessToken
import com.vk.api.sdk.auth.VKAuthCallback
import com.vk.api.sdk.auth.VKScope
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.*
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kiol.vkapp.commondata.domain.Place
import kiol.vkapp.commondata.domain.PlaceType
import kiol.vkapp.commondata.domain.places.PlacesUseCase
import timber.log.Timber


operator fun CompositeDisposable.plusAssign(d: Disposable) {
    this.add(d)
}

fun Fragment.getAppContext() = requireContext().applicationContext


class MainActivity : AppCompatActivity(), ClusterListener, MapObjectTapListener {

    private lateinit var simpleRouter: SimpleRouter

    private lateinit var mapview: MapView

    private lateinit var image: View

    private lateinit var clusterizedCollection: ClusterizedPlacemarkCollection

    private val CAMERA_TARGET = Point(59.952, 30.318)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MapKitFactory.setApiKey("4f99324d-241f-473e-9274-80297deb2e9f")
        MapKitFactory.initialize(this.applicationContext)
        setContentView(R.layout.activity_main)

        image = findViewById(R.id.testImage)
        image.setOnClickListener {
            clusterizedCollection.clear()
        }

        mapview = findViewById<View>(R.id.mapview) as MapView

        clusterizedCollection = mapview.getMap().getMapObjects().addClusterizedPlacemarkCollection(this)


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


        clusterizedCollection.addTapListener(this)

        simpleRouter = SimpleRouter(supportFragmentManager)

    }

    private fun startMain() {
        val d = PlacesUseCase().getPlaces(PlaceType.Groups).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                Timber.d("Groups: $it")
                it.forEach {
                    if (it is Place.GroupPlace) {
                        val point = Point(it.latitude.toDouble(), it.longitude.toDouble())
                        val placemarkMapObject = clusterizedCollection.addEmptyPlacemark(point)
                        placemarkMapObject.userData = it
                        loadPlacemarkImage(this, it, placemarkMapObject)
                        clusterizedCollection.clusterPlacemarks(60.0, 15)
                    }
                }
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

    override fun onClusterAdded(cluster: Cluster) {
        cluster.appearance.setIcon(
            TextImageProvider(this, Integer.toString(cluster.size))
        )
    }

    override fun onMapObjectTap(p0: MapObject, p1: Point): Boolean {
        mapview.map.move(
            CameraPosition(p1, mapview.map.cameraPosition.zoom, 0f, 0f),
            Animation(Animation.Type.SMOOTH, 0.1f),
            null
        )
        p0.userData?.let {
            when (val place = it as Place) {
                is Place.GroupPlace -> {
                    DescriptionDialog.create(place).show(supportFragmentManager, null)
                }
            }
        }
        return true
    }
}
