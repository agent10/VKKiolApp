package kiol.vkapp.taskc.renderers

import android.content.Context
import android.location.Location
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.SphericalUtil
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterManager
import kiol.vkapp.commondata.domain.PlaceType
import kiol.vkapp.taskc.PlaceClusterItem
import timber.log.Timber

class PlaceClusterRenderer(
    private val context: Context, googleMap: GoogleMap?, clusterManager:
    ClusterManager<PlaceClusterItem>, private val markerImageGenerator: MarkerImageGenerator
) :
    CustomClusterRenderer<PlaceClusterItem>(context, googleMap, clusterManager) {

    companion object {
        private const val MinDistanceCluster = 4
    }

    init {
        minClusterSize = 1
    }

    override fun onBeforeClusterRendered(cluster: Cluster<PlaceClusterItem>?, markerOptions: MarkerOptions?) {
        markerOptions?.zIndex(Float.MAX_VALUE)
        cluster?.let {
            markerOptions?.icon(markerImageGenerator.getClusterBitmapDescriptor(cluster))
        }
        Timber.d("kiol onBeforeClusterRendered")
    }

    override fun onClusterRendered(cluster: Cluster<PlaceClusterItem>?, marker: Marker) {
        super.onClusterRendered(cluster, marker)
        marker.tag = "tag"
        cluster?.let {
            val place = it.getPlace()
            place?.let {
                if (it.placeType == PlaceType.Photos) {
                    markerImageGenerator.loadPhotoClusterImageWithCount(context, it, marker, cluster.size)

                }
            }
        }
        Timber.d("kiol onClusterRendered")
    }

    override fun onBeforeClusterItemRendered(item: PlaceClusterItem, markerOptions: MarkerOptions) {
        markerOptions.icon(markerImageGenerator.getStubBimapDescriptor(item.place))
        Timber.d("kiol onBeforeClusterItemRendered")

    }

    override fun onClusterItemRendered(clusterItem: PlaceClusterItem, marker: Marker) {
        super.onClusterItemRendered(clusterItem, marker)
        marker.tag = "tag"
        markerImageGenerator.loadPlacemarkImage(context, clusterItem.place, marker)
        Timber.d("kiol onClusterItemRendered")

    }

    override fun onClusterUpdated(cluster: Cluster<PlaceClusterItem>?, marker: Marker) {
        marker.tag = "tag"
    }

    override fun onClusterItemUpdated(item: PlaceClusterItem?, marker: Marker?) {}

    override fun shouldRenderAsCluster(cluster: Cluster<PlaceClusterItem>?): Boolean {
        val c1 = super.shouldRenderAsCluster(cluster)

        var c2 = true
        cluster?.run {
            if (size in 2..4) {
                var centerLat = 0.0
                var centerLong = 0.0
                items.forEach {
                    centerLat += it.position.latitude
                    centerLong += it.position.longitude
                }
                centerLat /= size
                centerLong /= size
                val centerCluser = LatLng(centerLat, centerLong)

                var maxRadius = Double.MIN_VALUE
                items.forEach {
                    val d = SphericalUtil.computeDistanceBetween(centerCluser, it.position)
                    if (d > maxRadius) {
                        maxRadius = d
                    }
                }

                if (maxRadius >= 0 && maxRadius < MinDistanceCluster) {
                    c2 = false
                }
            }
        }

        return c1 && c2
    }
}