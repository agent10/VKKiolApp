package kiol.vkapp.map.map

import android.annotation.SuppressLint
import android.app.Activity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import kiol.vkapp.commondata.domain.places.PlacesUseCase
import kiol.vkapp.map.GMapFragment
import timber.log.Timber

class SimpleLocationProvider(activity: Activity, private val callback: (LatLng) -> Unit) {
    companion object {
        val SPB_LAT_LONG = LatLng(59.9343, 30.3351)
    }

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)

    @SuppressLint("MissingPermission")
    fun provide() {
        val locationTask = fusedLocationClient.lastLocation
        locationTask.addOnFailureListener {
            Timber.e("kiol location failure $it")
            callback(SPB_LAT_LONG)
        }
        locationTask.addOnSuccessListener {
            Timber.d("kiol location success $it")
            callback(if (it != null) LatLng(it.latitude, it.longitude) else SPB_LAT_LONG)
        }
        locationTask.addOnCompleteListener {
            Timber.d("kiol location completed $it")
        }
    }
}