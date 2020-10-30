package kiol.vkapp.map.description

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kiol.vkapp.commondata.domain.Box
import kiol.vkapp.commondata.domain.BoxType.*
import kiol.vkapp.commondata.domain.Place
import kiol.vkapp.commonui.SpaceItemDecoration
import kiol.vkapp.commonui.viewLifecycleLazy
import kiol.vkapp.map.R
import kiol.vkapp.map.databinding.DescriptionDialogBinding
import kiol.vkapp.map.renderers.transformations.BoxCircleCropTransformation
import kiol.vkapp.map.unsubscribe.UnsubscribeDialog

class DescriptionDialog : BottomSheetDialogFragment() {

    interface ImageClickListener {
        fun onDescriptionImageClicked(uri: Uri)
    }

    companion object {
        private const val PLACE_TITLE = "place_title"
        private const val PLACE_ADDRESSS = "place_address"
        private const val PLACE_DESCRIPTION = "place_description"
        private const val PLACE_BOX = "place_box"

        fun create(place: Place): DescriptionDialog {
            return DescriptionDialog().apply {
                arguments = Bundle().apply {
                    putString(PLACE_TITLE, place.title)
                    putString(PLACE_ADDRESSS, place.address)
                    putString(PLACE_DESCRIPTION, place.description)

                    place.customPlaceParams?.let {
                        putParcelable(PLACE_BOX, it.box)
                    }
                }
            }
        }
    }

    private val binding by viewLifecycleLazy {
        DescriptionDialogBinding.bind(requireView())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.description_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = requireArguments()
        val title = args.getString(PLACE_TITLE, "").orEmpty()
        val address = args.getString(PLACE_ADDRESSS, "").orEmpty()
        val desc = args.getString(PLACE_DESCRIPTION, "").orEmpty()
        val box: Box = args.getParcelable(PLACE_BOX)!!

        binding.close.setOnClickListener {
            dismissAllowingStateLoss()
        }

        binding.status.setText(
            when (box.boxType) {
                Ok -> R.string.status_ok
                Fraud -> R.string.status_fraud
                Unknown -> R.string.status_unknown
            }
        )

        setupImage(box)

        binding.title.text = title
        if (address.isEmpty()) {
            binding.addressLine.visibility = View.GONE
        }

        binding.address.text = address

        if (box.boxType == Unknown) {
            binding.stateGroup.visibility = View.GONE
            binding.waitStubText.visibility = View.VISIBLE
            binding.unsubscribeBtn.visibility = View.GONE
        } else {
            binding.stateGroup.visibility = View.VISIBLE
            binding.waitStubText.visibility = View.GONE

            binding.unsubscribeBtn.setOnClickListener {
                handleUnsubscribe(box)
            }
            binding.unsubscribeBtn.visibility = if (box.boxType == Ok) View.GONE else View.VISIBLE

            binding.description.text = desc

            setupGroups(box)
        }
    }

    private fun setupImage(box: Box) {
        val circleColor = when (box.boxType) {
            Ok -> 0xFF4BB34B.toInt()
            Fraud -> 0xFFFF5C5C.toInt()
            Unknown -> 0xFF76787A.toInt()
            else -> 0xFF76787A.toInt()
        }
        binding.image.load(box.photo) {
            crossfade(true)
            transformations(BoxCircleCropTransformation(circleColor))
        }
        binding.image.setOnClickListener {
            handleImageClicked(box)
        }
    }

    private fun setupGroups(box: Box) {
        binding.groupsList.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        binding.groupsList.addItemDecoration(SpaceItemDecoration(resources.getDimensionPixelOffset(R.dimen.groups_margin)))

        binding.groupsList.adapter = GroupsAdapter(box.vkGroups) {
            val link = it.createLink()
            if (link.isNotEmpty()) {
                val chooser = Intent.createChooser(Intent(Intent.ACTION_VIEW, Uri.parse(link)), "")
                startActivity(chooser)
            }
        }
    }

    private fun handleImageClicked(box: Box) {
        dismiss()
        (parentFragment as? ImageClickListener)?.onDescriptionImageClicked(Uri.parse(box.photo))
    }

    private fun handleUnsubscribe(box: Box) {
        dismiss()
        parentFragment?.fragmentManager?.let {
            UnsubscribeDialog.create(box.vkGroups).show(it, "")
        }
    }

    override fun onStart() {
        super.onStart()
        val behavior = BottomSheetBehavior.from(requireView().parent as View)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }
}