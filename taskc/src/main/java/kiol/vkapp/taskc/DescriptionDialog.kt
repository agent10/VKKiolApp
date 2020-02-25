package kiol.vkapp.taskc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kiol.vkapp.commondata.domain.Place

class DescriptionDialog : BottomSheetDialogFragment() {

    companion object {
        private const val PLACE_TITLE = "place_title"
        private const val PLACE_ADDRESSS = "place_address"
        private const val PLACE_DESCRIPTION = "place_description"
        private const val PLACE_SITE = "place_site"

        fun create(place: Place): DescriptionDialog {
            return DescriptionDialog().apply {
                arguments = Bundle().apply {
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

        val titleTv = view.findViewById<TextView>(R.id.title)
        val addressTv = view.findViewById<TextView>(R.id.address)
        val descTv = view.findViewById<TextView>(R.id.description)
        val btn = view.findViewById<Button>(R.id.httpOpenBtn)

        titleTv.text = title
        addressTv.text = address
        descTv.text = desc

        btn.setOnClickListener {
            Toast.makeText(requireContext(), "Site clicked $site", Toast.LENGTH_SHORT).show()
        }
    }
}