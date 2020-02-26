package kiol.vkapp.taskc

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kiol.vkapp.commondata.domain.Place
import kiol.vkapp.commondata.domain.PlaceType

class DescriptionDialog : BottomSheetDialogFragment() {

    companion object {
        private const val PLACE_LINK = "place_link"
        private const val PLACE_TITLE = "place_title"
        private const val PLACE_ADDRESSS = "place_address"
        private const val PLACE_DESCRIPTION = "place_description"
        private const val PLACE_SITE = "place_site"

        fun create(place: Place): DescriptionDialog {
            return DescriptionDialog().apply {
                arguments = Bundle().apply {
                    var link = "https://www.vk.com/"
                    link += if (place.placeType == PlaceType.Groups) {
                        "club${place.id}"
                    } else {
                        "event${place.id}"
                    }

                    putString(PLACE_LINK, link)
                    putString(PLACE_TITLE, place.title)
                    putString(PLACE_ADDRESSS, place.address)
                    putString(PLACE_DESCRIPTION, place.description)
                    putString(PLACE_SITE, place.site)
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.description_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title = arguments?.getString(PLACE_TITLE, "Unknown").orEmpty()
        val address = arguments?.getString(PLACE_ADDRESSS, "Unknown").orEmpty()
        val desc = arguments?.getString(PLACE_DESCRIPTION, "Unknown").orEmpty()
        val site = arguments?.getString(PLACE_SITE, "Empty").orEmpty()
        val link = arguments?.getString(PLACE_LINK, "").orEmpty()

        val addressLine = view.findViewById<ViewGroup>(R.id.addressLine)

        if (address.isEmpty()) {
            addressLine.visibility = View.GONE
        }

        val titleTv = view.findViewById<TextView>(R.id.title)
        val addressTv = view.findViewById<TextView>(R.id.address)
        val descTv = view.findViewById<TextView>(R.id.description)
        val btn = view.findViewById<Button>(R.id.httpOpenBtn)

        titleTv.text = title
        addressTv.text = address
        descTv.text = desc

        btn.setOnClickListener {
            if (link.isNotEmpty()) {
                val chooser = Intent.createChooser(Intent(Intent.ACTION_VIEW, Uri.parse(link)), "")
                startActivity(chooser)
            }
        }
    }
}