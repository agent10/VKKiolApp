package kiol.vkapp.map.map

import android.content.Context
import android.location.Geocoder
import com.google.android.gms.maps.model.LatLng
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

fun Context.getPrettyAddress(latLng: LatLng, callback: (String) -> Unit) {
    val d = Flowable.defer {
        val geocoder = Geocoder(this).getFromLocation(latLng.latitude, latLng.longitude, 1)
        Flowable.just(geocoder.firstOrNull()?.getAddressLine(0).orEmpty())
    }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).onErrorReturnItem("").subscribe({
        callback(it)
    }, {
        Timber.e("Error during geocoding = $it")
    })
}