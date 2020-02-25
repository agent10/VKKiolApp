package kiol.vkapp.taskc

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.*
import com.yandex.mapkit.mapview.MapView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kiol.vkapp.commondata.domain.Place
import kiol.vkapp.commondata.domain.PlaceType
import kiol.vkapp.commondata.domain.places.PlacesUseCase
import timber.log.Timber

class MapFragment : Fragment(R.layout.map_fragment_layout), ClusterListener, MapObjectTapListener {

    private lateinit var mapview: MapView

    private lateinit var tabs: TabLayout

    private lateinit var clusterizedCollection: ClusterizedPlacemarkCollection

    private val placesUseCase = PlacesUseCase()

    private var disposable: Disposable? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tabs = view.findViewById(R.id.tabs)
        mapview = view.findViewById(R.id.mapview)

        clusterizedCollection = mapview.map.mapObjects.addClusterizedPlacemarkCollection(this)
        clusterizedCollection.addTapListener(this)

        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> updateMap(PlaceType.Events)
                    1 -> updateMap(PlaceType.Photos)
                    2 -> updateMap(PlaceType.Groups)
                    else -> Timber.e("Unknown tab")
                }

            }
        })

        updateMap(PlaceType.Events)
    }

    private fun updateMap(placeType: PlaceType) {
        disposable?.dispose()

        clusterizedCollection.clear()
        disposable = placesUseCase.getPlaces(placeType).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                Timber.d("Groups: $it")
                if (it.isEmpty()) {
                    Toast.makeText(requireContext(), "Ничего не найдено", Toast.LENGTH_SHORT).show()
                }
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
            Animation(Animation.Type.SMOOTH, 0.5f),
            null
        )
        p0.userData?.let {
            val p = it as Place

            if (p.placeType == PlaceType.Photos) {
                childFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.viewer_fragment_open_enter,
                        R.anim.viewer_fragment_open_enter,
                        R.anim.viewer_fragment_open_exit,
                        R.anim.viewer_fragment_open_exit
                    ).replace(
                        R.id.contentViewer, ImageViewerFragment.create(p)
                    ).addToBackStack(null)
                    .commitAllowingStateLoss()
            } else {
                DescriptionDialog.create(it).show(childFragmentManager, null)
            }
        }
        return true
    }
}