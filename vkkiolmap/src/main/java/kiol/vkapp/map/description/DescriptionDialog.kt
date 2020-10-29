package kiol.vkapp.map.description

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kiol.vkapp.commondata.data.VKGroup
import kiol.vkapp.commondata.domain.Box
import kiol.vkapp.commondata.domain.BoxType.*
import kiol.vkapp.commondata.domain.Place
import kiol.vkapp.commonui.SpaceItemDecoration
import kiol.vkapp.commonui.viewLifecycleLazy
import kiol.vkapp.map.R
import kiol.vkapp.map.databinding.DescriptionDialogBinding
import kiol.vkapp.map.unsubscribe.UnsubscribeDialog

class GroupsAdapter(val groups: List<VKGroup>, private val click: (VKGroup) -> Unit) : RecyclerView.Adapter<GroupsAdapter.VH>() {

    class VH(item: View) : RecyclerView.ViewHolder(item)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.vk_group_item_layout, parent, false)
        )
    }

    override fun getItemCount() = groups.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.itemView.setOnClickListener {
            click(groups[position])
        }
        val title = holder.itemView.findViewById<TextView>(R.id.title)
        title.text = groups[position].name

        holder.itemView.findViewById<ImageView>(R.id.image).load(groups[position].photo_100) {
            crossfade(true)
            transformations(CircleCropTransformation())
        }
    }
}

class DescriptionDialog : BottomSheetDialogFragment() {

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

        activity?.window?.navigationBarColor = Color.RED

        val args = requireArguments()
        val title = args.getString(PLACE_TITLE, "").orEmpty()
        val address = args.getString(PLACE_ADDRESSS, "").orEmpty()
        val desc = args.getString(PLACE_DESCRIPTION, "").orEmpty()
        val box: Box = args.getParcelable(PLACE_BOX)!!

        val addressLine = view.findViewById<ViewGroup>(R.id.addressLine)
        val titleTv = view.findViewById<TextView>(R.id.title)
        val addressTv = view.findViewById<TextView>(R.id.address)
        val descTv = view.findViewById<TextView>(R.id.description)
        val image = view.findViewById<ImageView>(R.id.image)
        val groupsList = view.findViewById<RecyclerView>(R.id.groupsList)

        view.findViewById<View>(R.id.close).setOnClickListener {
            dismissAllowingStateLoss()
        }

        binding.status.setText(
            when (box.boxType) {
                Ok -> R.string.status_ok
                Fraud -> R.string.status_fraud
                Unknown -> R.string.status_unknown
            }
        )

        image.load(box.photo) {
            crossfade(true)
            transformations(CircleCropTransformation())
        }
        titleTv.text = title

        if (box.boxType == Unknown) {
            binding.stateGroup.visibility = View.GONE
            binding.waitStubText.visibility = View.VISIBLE
            binding.unsubscribeBtn.visibility = View.GONE
        } else {
            binding.stateGroup.visibility = View.VISIBLE
            binding.waitStubText.visibility = View.GONE

            binding.unsubscribeBtn.setOnClickListener {
                dismiss()
                parentFragment?.fragmentManager?.let {
                    UnsubscribeDialog.create(box.vkGroups).show(it, "")
                }
            }
            binding.unsubscribeBtn.visibility = if (box.boxType == Ok) View.GONE else View.VISIBLE

            if (address.isEmpty()) {
                addressLine.visibility = View.GONE
            }

            addressTv.text = address
            descTv.text = desc

            groupsList.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
            groupsList.addItemDecoration(SpaceItemDecoration(64))

            groupsList.adapter = GroupsAdapter(box.vkGroups) {
                val link = it.createLink()
                if (link.isNotEmpty()) {
                    val chooser = Intent.createChooser(Intent(Intent.ACTION_VIEW, Uri.parse(link)), "")
                    startActivity(chooser)
                }
            }
        }
    }
}