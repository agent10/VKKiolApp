package kiol.vkapp.map

import android.Manifest
import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kiol.vkapp.commondata.domain.PlaceType
import kiol.vkapp.commondata.domain.places.PlacesUseCase
import kiol.vkapp.commonui.permissions.PermissionManager
import kiol.vkapp.commonui.toast
import kiol.vkapp.commonui.viewLifecycleLazy
import kiol.vkapp.map.clusters.PlaceClusterItem
import kiol.vkapp.map.clusters.PlaceClusterManager
import kiol.vkapp.map.clusters.addPlace
import kiol.vkapp.map.clusters.addPlaces
import kiol.vkapp.map.databinding.GmapFragmentLayoutBinding
import kiol.vkapp.map.description.DescriptionDialog
import kiol.vkapp.map.description.ImageViewerFragment
import kiol.vkapp.map.map.SimpleLocationProvider
import kiol.vkapp.map.map.getPrettyAddress
import kiol.vkapp.map.renderers.MarkerImageGenerator
import timber.log.Timber

class GMapFragment : Fragment(R.layout.gmap_fragment_layout), OnMapReadyCallback, DescriptionDialog.ImageClickListener {

    companion object {
        val placesUseCase = PlacesUseCase()
    }

    private val binding by viewLifecycleLazy {
        GmapFragmentLayoutBinding.bind(requireView())
    }

    private lateinit var mapFragment: SupportMapFragment

    private lateinit var googleMap: GoogleMap
    private var clusterManager: PlaceClusterManager? = null

    private val compositeDisposable = CompositeDisposable()

    private lateinit var markerImageGenerator: MarkerImageGenerator

    private lateinit var locationProvider: SimpleLocationProvider

    private lateinit var permissionManager: PermissionManager

    private var lastLocation: LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionManager = PermissionManager(
            requireContext(), listOf(
                Manifest.permission.ACCESS_COARSE_LOCATION, Manifest
                    .permission.ACCESS_FINE_LOCATION
            )
        )
    }

    @SuppressLint("MissingPermission")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        markerImageGenerator = MarkerImageGenerator(requireContext())
        locationProvider = SimpleLocationProvider(requireActivity()) {
            if (lastLocation == null) {
                googleMap.isMyLocationEnabled = true
                handleNewLocation(it)
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onStart() {
        super.onStart()
        permissionManager.checkPermissions(this) {
            if (it) {
                locationProvider.provide()
            } else {
                updateMap()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapFragment = SupportMapFragment()
        childFragmentManager.beginTransaction().replace(R.id.mapview, mapFragment).commitAllowingStateLoss()

        mapFragment.getMapAsync(this)

        binding.toolbar.apply {
            setOnApplyWindowInsetsListener { v, insets ->
                view.updatePadding(top = insets.systemWindowInsetTop)
                insets
            }

            setOnMenuItemClickListener {
                if (it.itemId == R.id.add_box) {
                    if (lastLocation == null) {
                        requireContext().toast(R.string.add_no_location_error)
                    } else {
                        getSimpleRouter().routeToCamera()
                    }
                }
                true
            }
        }

        compositeDisposable += placesUseCase.observeChanges().subscribe({
            clusterManager?.addPlace(it)
        }, {
            Timber.e("Error during places observing = $it")
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        compositeDisposable.clear()
    }

    private fun updateMap() {
        clusterManager?.clearItems()

        binding.progressbar.visibility = View.VISIBLE
        compositeDisposable += placesUseCase.getBoxes().subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnEach {
                binding.progressbar.visibility = View.GONE
            }.subscribe({ places ->
                Timber.d("Groups: $places")
                if (places.isEmpty()) {
                    Toast.makeText(requireContext(), R.string.nothing_found, Toast.LENGTH_SHORT).show()
                }

                clusterManager?.addPlaces(places)
            }, {
                binding.progressbar.visibility = View.GONE
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

    private fun handleNewLocation(latLng: LatLng) {
        if (lastLocation == null) {
            requireActivity().getPrettyAddress(latLng) {
                placesUseCase.currentPrettyAddress = it
            }
            placesUseCase.setLatLong(latLng.latitude.toFloat(), latLng.longitude.toFloat())
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10f))
            updateMap()
        }
        lastLocation = latLng
    }
}