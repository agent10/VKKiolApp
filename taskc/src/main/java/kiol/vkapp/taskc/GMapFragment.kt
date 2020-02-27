package kiol.vkapp.taskc

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.material.tabs.TabLayout
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.algo.NonHierarchicalDistanceBasedAlgorithm
import com.google.maps.android.clustering.algo.PreCachingAlgorithmDecorator
import com.google.maps.android.clustering.algo.ScreenBasedAlgorithmAdapter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kiol.vkapp.commondata.domain.Place
import kiol.vkapp.commondata.domain.PlaceType
import kiol.vkapp.commondata.domain.places.PlacesUseCase
import kiol.vkapp.commondata.domain.places.PlacesUseCase2
import kiol.vkapp.taskc.renderers.MarkerImageGenerator
import kiol.vkapp.taskc.renderers.PlaceClusterRenderer
import timber.log.Timber

class GMapFragment : Fragment(R.layout.gmap_fragment_layout), OnMapReadyCallback {

    companion object {
        private const val MAX_DISTANCE = 200
    }

    private lateinit var mapview: FrameLayout

    private lateinit var tabs: TabLayout

    private lateinit var mapFragment: SupportMapFragment

    private lateinit var googleMap: GoogleMap
    private var clusterManager: ClusterManager<PlaceClusterItem>? = null

    private val placesUseCase = PlacesUseCase2()

    private var disposable: Disposable? = null

    private lateinit var markerImageGenerator: MarkerImageGenerator

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        markerImageGenerator = MarkerImageGenerator(requireContext())
    }

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
                    Toast.makeText(requireContext(), R.string.nothing_found, Toast.LENGTH_SHORT).show()
                }

                it.forEach {
                    clusterManager?.addItem(PlaceClusterItem(it))
                }
                clusterManager?.cluster()
            }, {
                Timber.e("Groups error: $it")
            })
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        Timber.e("onMapReady: $googleMap")
        googleMap?.let {
            this.googleMap = googleMap
            initClusterManager(googleMap)
        }
    }

    private fun initClusterManager(googleMap: GoogleMap) {
        val clusterManager = ClusterManager<PlaceClusterItem>(requireContext(), googleMap)
        clusterManager.renderer = PlaceClusterRenderer(
            requireContext(),
            googleMap,
            clusterManager,
            markerImageGenerator
        )

        val algo = NonHierarchicalDistanceBasedAlgorithm<PlaceClusterItem>()
        algo.maxDistanceBetweenClusteredItems = MAX_DISTANCE
        clusterManager.algorithm = ScreenBasedAlgorithmAdapter(PreCachingAlgorithmDecorator(algo))
        clusterManager.setOnClusterClickListener {
            true
        }
        clusterManager.setOnClusterItemClickListener {
            val p = it.place
            if (p.placeType == PlaceType.Photos) {
                showImageViewer(p)
            } else {
                DescriptionDialog.create(p).show(childFragmentManager, null)
            }
            true
        }
        googleMap.setOnCameraIdleListener(clusterManager)
        this.clusterManager = clusterManager
    }

    private fun showImageViewer(place: Place) {
        childFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.viewer_fragment_open_enter,
                R.anim.viewer_fragment_open_enter,
                R.anim.viewer_fragment_open_exit,
                R.anim.viewer_fragment_open_exit
            ).replace(
                R.id.contentViewer, ImageViewerFragment.create(place)
            ).addToBackStack(null)
            .commitAllowingStateLoss()
    }

}