package kiol.vkapp.map

import android.Manifest
import android.annotation.SuppressLint
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kiol.vkapp.commondata.domain.Place
import kiol.vkapp.commondata.domain.PlaceType
import kiol.vkapp.commondata.domain.places.PlacesUseCase
import kiol.vkapp.commonui.permissions.PermissionManager
import kiol.vkapp.commonui.viewLifecycleLazy
import kiol.vkapp.map.clusters.PlaceClusterItem
import kiol.vkapp.map.clusters.PlaceClusterManager
import kiol.vkapp.map.databinding.GmapFragmentLayoutBinding
import kiol.vkapp.map.description.DescriptionDialog
import kiol.vkapp.map.description.ImageViewerFragment
import kiol.vkapp.map.renderers.MarkerImageGenerator
import timber.log.Timber

class GMapFragment : Fragment(R.layout.gmap_fragment_layout), OnMapReadyCallback, DescriptionDialog.ImageClickListener {

    companion object {
        private val SPB_LAT_LONG = LatLng(59.9343, 30.3351)
        val placesUseCase = PlacesUseCase()
    }

    private val binding by viewLifecycleLazy {
        GmapFragmentLayoutBinding.bind(requireView())
    }

    private lateinit var mapview: FrameLayout

    private lateinit var mapFragment: SupportMapFragment

    private lateinit var progressBar: ProgressBar

    private lateinit var googleMap: GoogleMap
    private var clusterManager: PlaceClusterManager? = null

    private var isDataDirty = true

    private var disposable: Disposable? = null

    private lateinit var markerImageGenerator: MarkerImageGenerator

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var permissionManager: PermissionManager

    private var currentAddr = ""

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
                if (isDataDirty) {
                    googleMap.isMyLocationEnabled = true

                    if (it != null) {
                        val latLng = LatLng(it.latitude, it.longitude)
                        findCurrentAddr(latLng)
                        placesUseCase.setLatLong(latLng.latitude.toFloat(), latLng.longitude.toFloat())
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10f))
                        updateMap()
                    }
                }
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

        binding.toolbar.apply {
            setOnApplyWindowInsetsListener { v, insets ->
                view.updatePadding(top = insets.systemWindowInsetTop)
                insets
            }
        }

        binding.toolbar.setOnMenuItemClickListener {
            if (it.itemId == R.id.add_box) {
                getSimpleRouter().routeToCamera(currentAddr)
                //                childFragmentManager.beginTransaction()
                //                    .setCustomAnimations(
                //                        R.anim.viewer_fragment_open_enter,
                //                        R.anim.viewer_fragment_open_enter,
                //                        R.anim.viewer_fragment_open_exit,
                //                        R.anim.viewer_fragment_open_exit
                //                    ).replace(
                //                        R.id.contentViewer, CamFragment()
                //                    ).addToBackStack(null)
                //                    .commitAllowingStateLoss()

                //                val newPlace = placesUseCase.addBoxForCheck()
                //                clusterManager?.addItem(PlaceClusterItem(newPlace))
                //                clusterManager?.cluster()
            }
            true
        }

        //        tabs.setOnApplyWindowInsetsListener { v, insets ->
        //            view.updatePadding(top = insets.systemWindowInsetTop)
        //            insets
        //        }

        val d = placesUseCase.observeChanges().subscribe({
            clusterManager?.addItem(PlaceClusterItem(it))
            clusterManager?.cluster()
        }, {

        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        disposable?.dispose()
        disposable = null
    }

    private fun updateMap() {
        clusterManager?.clearItems()
        disposable?.dispose()

        progressBar.visibility = View.VISIBLE
        disposable = placesUseCase.getBoxes().subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnEach {
                progressBar.visibility = View.GONE
            }.subscribe({ places ->
                //                isDataDirty = false
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
            googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.mapstyle))
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
                if (p.placeType == PlaceType.Box) {
                    DescriptionDialog.create(p).show(childFragmentManager, null)
                }
                true
            }
        }
    }

    override fun onDescriptionImageClicked(uri: Uri) {
        childFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.viewer_fragment_open_enter,
                R.anim.viewer_fragment_open_enter,
                R.anim.viewer_fragment_open_exit,
                R.anim.viewer_fragment_open_exit
            ).replace(
                R.id.imageViewer, ImageViewerFragment.create(uri)
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

    private fun findCurrentAddr(latLng: LatLng) {
        val d = Flowable.defer {
            val geocoder = Geocoder(requireContext()).getFromLocation(latLng.latitude, latLng.longitude, 1)
            Flowable.just(geocoder.firstOrNull()?.getAddressLine(0).orEmpty())
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe({
            currentAddr = it
        }, {})
    }
}