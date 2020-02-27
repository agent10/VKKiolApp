package kiol.vkapp.taskc

import android.content.Context
import com.google.android.gms.maps.GoogleMap
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.algo.NonHierarchicalDistanceBasedAlgorithm
import com.google.maps.android.clustering.algo.PreCachingAlgorithmDecorator
import com.google.maps.android.clustering.algo.ScreenBasedAlgorithmAdapter
import kiol.vkapp.taskc.renderers.MarkerImageGenerator
import kiol.vkapp.taskc.renderers.PlaceClusterRenderer

class PlaceClusterManager(
    context: Context,
    googleMap: GoogleMap,
    markerImageGenerator: MarkerImageGenerator
) : ClusterManager<PlaceClusterItem>(context, googleMap) {

    companion object {
        private const val MAX_DISTANCE = 200
    }

    init {
        renderer = PlaceClusterRenderer(
            context,
            googleMap,
            this,
            markerImageGenerator
        )

        val algo = NonHierarchicalDistanceBasedAlgorithm<PlaceClusterItem>()
        algo.maxDistanceBetweenClusteredItems = MAX_DISTANCE
        algorithm = ScreenBasedAlgorithmAdapter(PreCachingAlgorithmDecorator(algo))
        setOnClusterClickListener {
            true
        }

        googleMap.setOnCameraIdleListener(this)
    }
}