package kiol.vkapp.taskc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kiol.vkapp.commondata.domain.Place

class DescriptionDialog : BottomSheetDialogFragment() {

    companion object {
        private const val PLACE_TITLE = "place_title"
        private const val PLACE_ADDRESSS = "place_address"
        private const val PLACE_DESCRIPTION = "place_description"

        fun create(place: Place.GroupPlace): DescriptionDialog {
            return DescriptionDialog().apply {
                arguments = Bundle().apply {
                    putString(PLACE_TITLE, "Temp")
                    putString(PLACE_ADDRESSS, place.address)
                    putString(PLACE_DESCRIPTION, place.description)
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

        val titleTv = view.findViewById<TextView>(R.id.title)
        val addressTv = view.findViewById<TextView>(R.id.address)
        val descTv = view.findViewById<TextView>(R.id.description)

        titleTv.text = title
        addressTv.text = address
        descTv.text = desc
    }
}