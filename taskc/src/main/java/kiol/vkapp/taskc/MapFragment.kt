package kiol.vkapp.taskc

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.*
import com.yandex.mapkit.mapview.MapView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kiol.vkapp.commondata.domain.Place
import kiol.vkapp.commondata.domain.PlaceType
import kiol.vkapp.commondata.domain.places.PlacesUseCase
import timber.log.Timber

class MapFragment : Fragment(R.layout.map_fragment_layout), ClusterListener, MapObjectTapListener {

    private lateinit var mapview: MapView

    private lateinit var clusterizedCollection: ClusterizedPlacemarkCollection

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapview = view.findViewById(R.id.mapview) as MapView
        clusterizedCollection = mapview.map.mapObjects.addClusterizedPlacemarkCollection(this)
        clusterizedCollection.addTapListener(this)

        val d = PlacesUseCase().getPlaces(PlaceType.Groups).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                Timber.d("Groups: $it")
                it.forEach {
                    val point = Point(it.latitude.toDouble(), it.longitude.toDouble())
                    val placemarkMapObject = clusterizedCollection.addEmptyPlacemark(point)
                    placemarkMapObject.userData = it
                    loadPlacemarkImage(requireContext(), it, placemarkMapObject)
                    clusterizedCollection.clusterPlacemarks(60.0, 15)
                }
            }, {
                Timber.e("Groups error: $it")
            })
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        mapview.onStart()
    }

    override fun onStop() {
        mapview.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    override fun onClusterAdded(cluster: Cluster) {
        cluster.appearance.setIcon(
            TextImageProvider(requireContext(), cluster.size.toString())
        )
    }

    override fun onMapObjectTap(p0: MapObject, p1: Point): Boolean {
        mapview.map.move(
            CameraPosition(p1, mapview.map.cameraPosition.zoom, 0f, 0f),
            Animation(Animation.Type.SMOOTH, 0.1f),
            null
        )
        p0.userData?.let {
            DescriptionDialog.create(it as Place).show(childFragmentManager, null)
        }
        return true
    }
}