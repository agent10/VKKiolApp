package kiol.vkapp.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.tabs.TabLayout
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kiol.vkapp.commondata.domain.Place
import kiol.vkapp.commondata.domain.PlaceType
import kiol.vkapp.commondata.domain.places.PlacesUseCase
import kiol.vkapp.commonui.permissions.PermissionManager
import kiol.vkapp.commonui.viewLifecycleLazy
import kiol.vkapp.map.databinding.GmapFragmentLayoutBinding
import kiol.vkapp.map.renderers.MarkerImageGenerator
import timber.log.Timber
import java.util.*

class GMapFragment : Fragment(R.layout.gmap_fragment_layout), OnMapReadyCallback {

    companion object {
        private val SPB_LAT_LONG = LatLng(59.9343, 30.3351)
    }

    private val binding by viewLifecycleLazy {
        GmapFragmentLayoutBinding.bind(requireView())
    }

    private lateinit var mapview: FrameLayout

    private lateinit var mapFragment: SupportMapFragment

    private lateinit var progressBar: ProgressBar

    private lateinit var googleMap: GoogleMap
    private var clusterManager: PlaceClusterManager? = null

    private val placesUseCase = PlacesUseCase()

    private var disposable: Disposable? = null

    private lateinit var markerImageGenerator: MarkerImageGenerator

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var permissionManager: PermissionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionManager = PermissionManager(
            requireContext(), listOf(
                Manifest.permission.ACCESS_COARSE_LOCATION, Manifest
                    .permission.ACCESS_FINE_LOCATION
            )
        )
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        markerImageGenerator = MarkerImageGenerator(requireContext())
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
    }

    @SuppressLint("MissingPermission")
    override fun onStart() {
        super.onStart()
        permissionManager.checkPermissions(this) {
            val locationTask = fusedLocationClient.lastLocation
            locationTask.addOnFailureListener {
                Timber.e("kiol location $it")
            }
            locationTask.addOnSuccessListener {
                Timber.d("kiol location $it")
                googleMap.isMyLocationEnabled = true

                val latLng = LatLng(it.latitude, it.longitude)
//                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14f))
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14f))
                //                val a = Geocoder(requireContext()).getFromLocation(it.latitude, it.longitude, 1)
                Timber.d("kiol location addrs")
            }
            locationTask.addOnCompleteListener {
                Timber.d("kiol location $it")
            }
            locationTask.addOnCanceledListener {
                Timber.d("kiol location $it")
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapview = view.findViewById(R.id.mapview)
        progressBar = view.findViewById(R.id.progressbar)

        mapFragment = SupportMapFragment()
        childFragmentManager.beginTransaction().replace(R.id.mapview, mapFragment).commitAllowingStateLoss()

        mapFragment.getMapAsync(this)

        binding.toolbar.setOnMenuItemClickListener {
            if (it.itemId == R.id.add_box) {
                childFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.viewer_fragment_open_enter,
                        R.anim.viewer_fragment_open_enter,
                        R.anim.viewer_fragment_open_exit,
                        R.anim.viewer_fragment_open_exit
                    ).replace(
                        R.id.contentViewer, CamFragment()
                    ).addToBackStack(null)
                    .commitAllowingStateLoss()
            }
            true
        }

        //        tabs.setOnApplyWindowInsetsListener { v, insets ->
        //            view.updatePadding(top = insets.systemWindowInsetTop)
        //            insets
        //        }
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
            initClusterManager(googleMap)

            updateMap(PlaceType.Box)
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        permissionManager.invokePermissionRequest(requestCode, permissions, grantResults)
    }

}