package kiol.vkapp.taskc.renderers

import android.content.Context
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
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
}