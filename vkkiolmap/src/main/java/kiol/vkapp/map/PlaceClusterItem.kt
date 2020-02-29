package kiol.vkapp.map

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem
import kiol.vkapp.commondata.domain.Place

class PlaceClusterItem(val place: Place) :
    ClusterItem {

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