package kiol.vkapp.taskc

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.tabs.TabLayout
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.*
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.Frame
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kiol.vkapp.commondata.domain.Place
import kiol.vkapp.commondata.domain.PlaceType
import kiol.vkapp.commondata.domain.places.PlacesUseCase
import timber.log.Timber

class GMapFragment : Fragment(R.layout.gmap_fragment_layout), OnMapReadyCallback {

    private lateinit var mapview: FrameLayout

    private lateinit var tabs: TabLayout

    private lateinit var mapFragment: SupportMapFragment

    private lateinit var googleMap: GoogleMap

    private val placesUseCase = PlacesUseCase()

    private var disposable: Disposable? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tabs = view.findViewById(R.id.tabs)
        mapview = view.findViewById(R.id.mapview)

        mapFragment = SupportMapFragment()
        childFragmentManager.beginTransaction().replace(R.id.mapview, mapFragment).commitAllowingStateLoss()

        mapFragment.getMapAsync(this)

        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> updateMap(PlaceType.Events)
                    1 -> updateMap(PlaceType.Photos)
                    2 -> updateMap(PlaceType.Groups)
                    else -> Timber.e("Unknown tab")
                }

            }
        })

        updateMap(PlaceType.Events)
    }

    private fun updateMap(placeType: PlaceType) {
        disposable?.dispose()

        disposable = placesUseCase.getPlaces(placeType).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                Timber.d("Groups: $it")
                if (it.isEmpty()) {
                    Toast.makeText(requireContext(), "Ничего не найдено", Toast.LENGTH_SHORT).show()
                }

                val clusterManager = ClusterManager<PlaceClusterItem>(requireContext(), googleMap)
                clusterManager.renderer = PlaceClusterRenderer(requireContext(), googleMap, clusterManager)
                googleMap.setOnCameraIdleListener(clusterManager)

                it.forEach {
                    clusterManager.addItem(PlaceClusterItem(it))
                    //                    googleMap.addMarker(
                    //                        MarkerOptions().position(LatLng(it.latitude.toDouble(), it.longitude.toDouble())).icon(
                    //                            BitmapDescriptorFactory.fromBitmap(getStubBitmap())
                    //                        )
                    //                    )
                    //val point = Point(it.latitude.toDouble(), it.longitude.toDouble())
                    //val placemarkMapObject = clusterizedCollection.addEmptyPlacemark(point)
                    //placemarkMapObject.userData = it
                    //loadPlacemarkImage(requireContext(), it, placemarkMapObject)
                    //clusterizedCollection.clusterPlacemarks(60.0, 15)
                }
            }, {
                Timber.e("Groups error: $it")
            })
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        Timber.e("onMapReady: $googleMap")
        this.googleMap = googleMap!!
    }

}

class PlaceClusterRenderer(
    private val context: Context, googleMap: GoogleMap?, clusterManager:
    ClusterManager<PlaceClusterItem>
) :
    DefaultClusterRenderer<PlaceClusterItem>(context, googleMap, clusterManager) {
    init {
        minClusterSize = 2
    }

    override fun onBeforeClusterRendered(cluster: Cluster<PlaceClusterItem>?, markerOptions: MarkerOptions?) {
        super.onBeforeClusterRendered(cluster, markerOptions)
        Timber.d("kiol onBeforeClusterRendered")
    }

    override fun onClusterRendered(cluster: Cluster<PlaceClusterItem>?, marker: Marker?) {
        super.onClusterRendered(cluster, marker)
        Timber.d("kiol onClusterRendered")

    }

    override fun onBeforeClusterItemRendered(item: PlaceClusterItem, markerOptions: MarkerOptions) {
        super.onBeforeClusterItemRendered(item, markerOptions)
        //        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(getStubBitmap()))
        //        loadPlacemarkImage(context, item.place, markerOptions)

        Timber.d("kiol onBeforeClusterItemRendered")

    }

    override fun onClusterItemRendered(clusterItem: PlaceClusterItem, marker: Marker) {
        super.onClusterItemRendered(clusterItem, marker)

        loadPlacemarkImage(context, clusterItem.place, marker)
        Timber.d("kiol onClusterItemRendered")

    }

    override fun onClustersChanged(clusters: MutableSet<out Cluster<PlaceClusterItem>>?) {
        super.onClustersChanged(clusters)
        Timber.d("kiol onClustersChanged")

    }
}

class PlaceClusterItem(val place: Place) : ClusterItem {
    override fun getSnippet(): String {
        return "snippet"
    }

    override fun getTitle(): String {
        return "title"
    }

    override fun getPosition(): LatLng {
        return LatLng(place.latitude.toDouble(), place.longitude.toDouble())
    }

}