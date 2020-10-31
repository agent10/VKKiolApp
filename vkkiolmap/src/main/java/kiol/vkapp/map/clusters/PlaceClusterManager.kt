package kiol.vkapp.map.clusters

import android.content.Context
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.algo.NonHierarchicalDistanceBasedAlgorithm
import com.google.maps.android.clustering.algo.PreCachingAlgorithmDecorator
import com.google.maps.android.clustering.algo.ScreenBasedAlgorithmAdapter
import kiol.vkapp.commondata.domain.Place
import kiol.vkapp.map.renderers.MarkerImageGenerator
import kiol.vkapp.map.renderers.PlaceClusterRenderer

class PlaceClusterManager(
    context: Context,
    googleMap: GoogleMap,
    markerImageGenerator: MarkerImageGenerator
) : ClusterManager<PlaceClusterItem>(context, googleMap) {

    companion object {
        private const val MAX_DISTANCE = 165
    }

    init {
        renderer = PlaceClusterRenderer(
            context,
            googleMap,
            this,
            markerImageGenerator
        )

        val algo = NonHierarchicalDistanceBasedAlgorithm<PlaceClusterItem>()
        algo.maxDistanceBetweenClusteredItems =
            MAX_DISTANCE
        algorithm = ScreenBasedAlgorithmAdapter(PreCachingAlgorithmDecorator(algo))
        setOnClusterClickListener {
            googleMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(it.position, googleMap.cameraPosition.zoom + 1.0f), 300,
                null
            )
            true
        }

        googleMap.setOnCameraIdleListener(this)
    }
}

fun PlaceClusterManager.addPlace(place: Place) {
    addPlaces(listOf(place))
}

fun PlaceClusterManager.addPlaces(places: List<Place>) {
    places.forEach {
        addItem(PlaceClusterItem(it))
    }
    cluster()
}