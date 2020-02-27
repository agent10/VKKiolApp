package kiol.vkapp.taskc

import android.content.Context
import android.graphics.Color
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
import com.google.maps.android.clustering.algo.NonHierarchicalDistanceBasedAlgorithm
import com.google.maps.android.clustering.algo.PreCachingAlgorithmDecorator
import com.google.maps.android.clustering.algo.ScreenBasedAlgorithmAdapter
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
    private var clusterManager: ClusterManager<PlaceClusterItem>? = null

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
        clusterManager?.clearItems()
        disposable?.dispose()

        disposable = placesUseCase.getPlaces(placeType).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                Timber.d("Groups: $it")
                if (it.isEmpty()) {
                    Toast.makeText(requireContext(), "Ничего не найдено", Toast.LENGTH_SHORT).show()
                }

                it.forEach {
                    clusterManager?.addItem(PlaceClusterItem(it))
                }
                clusterManager?.cluster()
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

        val clusterManager = ClusterManager<PlaceClusterItem>(requireContext(), googleMap)
        clusterManager.renderer = PlaceClusterRenderer(requireContext(), googleMap, clusterManager)

        val algo = NonHierarchicalDistanceBasedAlgorithm<PlaceClusterItem>()
        algo.maxDistanceBetweenClusteredItems = 200
        clusterManager.algorithm = ScreenBasedAlgorithmAdapter(PreCachingAlgorithmDecorator(algo))
        clusterManager.setOnClusterClickListener {
            true
        }
        clusterManager.setOnClusterItemClickListener {
            val p = it.place

            if (p.placeType == PlaceType.Photos) {
                childFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.viewer_fragment_open_enter,
                        R.anim.viewer_fragment_open_enter,
                        R.anim.viewer_fragment_open_exit,
                        R.anim.viewer_fragment_open_exit
                    ).replace(
                        R.id.contentViewer, ImageViewerFragment.create(p)
                    ).addToBackStack(null)
                    .commitAllowingStateLoss()
            } else {
                DescriptionDialog.create(p).show(childFragmentManager, null)
            }
            true
        }
        googleMap.setOnCameraIdleListener(clusterManager)
        this.clusterManager = clusterManager
    }

}

class PlaceClusterRenderer(
    private val context: Context, googleMap: GoogleMap?, clusterManager:
    ClusterManager<PlaceClusterItem>
) :
    CustomClusterRenderer<PlaceClusterItem>(context, googleMap, clusterManager) {
    init {
        minClusterSize = 1
    }

    override fun getColor(clusterSize: Int): Int {
        return Color.RED
    }

    override fun onBeforeClusterRendered(cluster: Cluster<PlaceClusterItem>?, markerOptions: MarkerOptions?) {
        markerOptions?.zIndex(Float.MAX_VALUE)
        markerOptions?.icon(BitmapDescriptorFactory.fromBitmap(getCroppedPhotoStubBitmapWithBadge(context, cluster?.size ?: 0)))

        cluster?.items?.forEach {
            it.isFirstInCluster = false
        }

        val firstPlace = cluster?.items?.firstOrNull()
        firstPlace?.let {
            if (it.place.placeType == PlaceType.Photos) {
                it.isFirstInCluster = true
            }
        }
        Timber.d("kiol onBeforeClusterRendered")
    }

    override fun onClusterRendered(cluster: Cluster<PlaceClusterItem>?, marker: Marker) {
        super.onClusterRendered(cluster, marker)
        marker.tag = "tag"
        val firstPlace = cluster?.items?.firstOrNull()
        firstPlace?.let {
            if (it.place.placeType == PlaceType.Photos) {
                loadPlacemarkImageWithCount(context, it.place, marker, cluster.size)
            }
        }
        Timber.d("kiol onClusterRendered")
    }

    override fun onBeforeClusterItemRendered(item: PlaceClusterItem, markerOptions: MarkerOptions) {
        if (item.isFirstInCluster) {
            markerOptions.zIndex(Float.MAX_VALUE - 1)
        }
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(getCroppedPhotoStubBitmap()))
        Timber.d("kiol onBeforeClusterItemRendered")

    }

    override fun onClusterItemRendered(clusterItem: PlaceClusterItem, marker: Marker) {
        super.onClusterItemRendered(clusterItem, marker)
        marker.tag = "tag"
        if (clusterItem.isFirstInCluster) {
            marker.zIndex = Float.MAX_VALUE - 1
        }
        loadPlacemarkImage(context, clusterItem.place, marker)
        Timber.d("kiol onClusterItemRendered")

    }

    override fun onClusterUpdated(cluster: Cluster<PlaceClusterItem>?, marker: Marker) {
        marker.tag = "tag"
        marker.zIndex = Float.MAX_VALUE
        //        super.onClusterUpdated(cluster, marker)
        //        marker?.setIcon(BitmapDescriptorFactory.fromBitmap(getCroppedPhotoStubBitmap()))
        //        val firstPlace = cluster?.items?.firstOrNull()
        //        firstPlace?.let {
        //            if (it.place.placeType == PlaceType.Photos) {
        //                it.isFirstInCluster = true
        //                loadPlacemarkImage(context, it.place, marker)
        //            }
        //        }
    }

    override fun onClusterItemUpdated(item: PlaceClusterItem?, marker: Marker?) {
//        super.onClusterItemUpdated(item, marker)
    }

    override fun onClustersChanged(clusters: MutableSet<out Cluster<PlaceClusterItem>>?) {
        super.onClustersChanged(clusters)
        clusters?.forEach {
            it.items.forEach {
                it.isFirstInCluster = false
            }
        }
        Timber.d("kiol onClustersChanged")
    }
}

class PlaceClusterItem(val place: Place) : ClusterItem {

    var isFirstInCluster = false

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