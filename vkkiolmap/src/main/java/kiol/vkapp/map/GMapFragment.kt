package kiol.vkapp.map

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.tabs.TabLayout
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kiol.vkapp.commondata.domain.Place
import kiol.vkapp.commondata.domain.PlaceType
import kiol.vkapp.commondata.domain.places.PlacesUseCase
import kiol.vkapp.map.renderers.MarkerImageGenerator
import timber.log.Timber

class GMapFragment : Fragment(R.layout.gmap_fragment_layout), OnMapReadyCallback {

    companion object {
        private val SPB_LAT_LONG = LatLng(59.9343, 30.3351)
    }

    private lateinit var mapview: FrameLayout

    private lateinit var tabs: TabLayout

    private lateinit var mapFragment: SupportMapFragment

    private lateinit var progressBar: ProgressBar

    private lateinit var googleMap: GoogleMap
    private var clusterManager: PlaceClusterManager? = null

    private val placesUseCase = PlacesUseCase()

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
        progressBar = view.findViewById(R.id.progressbar)

        mapFragment = SupportMapFragment()
        childFragmentManager.beginTransaction().replace(R.id.mapview, mapFragment).commitAllowingStateLoss()

        mapFragment.getMapAsync(this)

        tabs.setOnApplyWindowInsetsListener { v, insets ->
            view.updatePadding(top = insets.systemWindowInsetTop)
            insets
        }

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

    override fun onDestroyView() {
        super.onDestroyView()
        disposable?.dispose()
        disposable = null
    }

    private fun updateMap(placeType: PlaceType) {
        clusterManager?.clearItems()
        disposable?.dispose()

        progressBar.visibility = View.VISIBLE
        disposable = placesUseCase.getPlaces(placeType).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnEach {
                progressBar.visibility = View.GONE
            }.subscribe({ places ->
                Timber.d("Groups: $places")
                if (places.isEmpty()) {
                    Toast.makeText(requireContext(), R.string.nothing_found, Toast.LENGTH_SHORT).show()
                }

                clusterManager?.apply {
                    places.forEach {
                        addItem(PlaceClusterItem(it))
                    }
                    cluster()
                }
            }, {
                Toast.makeText(requireContext(), "${getString(R.string.error)} $it", Toast.LENGTH_SHORT).show()
                Timber.e("Groups error: $it")
            })
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        Timber.e("onMapReady: $googleMap")
        googleMap?.let {
            this.googleMap = googleMap
            googleMap.setMinZoomPreference(1.0f)
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(SPB_LAT_LONG, 7f))
            initClusterManager(googleMap)
        }
    }

    private fun initClusterManager(googleMap: GoogleMap) {
        clusterManager = PlaceClusterManager(
            requireContext(),
            googleMap, markerImageGenerator
        ).apply {
            setOnClusterItemClickListener {
                val p = it.place
                if (p.placeType == PlaceType.Photos) {
                    showImageViewer(p)
                } else {
                    DescriptionDialog.create(p).show(childFragmentManager, null)
                }
                true
            }
        }
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