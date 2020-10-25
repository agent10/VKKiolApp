package kiol.vkapp.map

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kiol.vkapp.commondata.data.VKGroup
import kiol.vkapp.commondata.domain.Place
import kiol.vkapp.commondata.domain.PlaceType

class GroupsAdapter(val groups: List<VKGroup>, private val click: (VKGroup) -> Unit) : RecyclerView.Adapter<GroupsAdapter.VH>() {

    class VH(item: View) : RecyclerView.ViewHolder(item)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(LayoutInflater.from(parent.context).inflate(R.layout.vk_group_item_layout, parent, false))
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
        private const val PLACE_LINK = "place_link"
        private const val PLACE_TITLE = "place_title"
        private const val PLACE_IMAGE = "place_title"
        private const val PLACE_ADDRESSS = "place_address"
        private const val PLACE_DESCRIPTION = "place_description"
        private const val PLACE_VKGROUPS = "place_vkgroups"

        fun create(place: Place): DescriptionDialog {
            return DescriptionDialog().apply {
                arguments = Bundle().apply {
                    putString(PLACE_LINK, place.createLink())
                    putString(PLACE_TITLE, place.title)
                    putString(PLACE_IMAGE, place.photo)
                    putString(PLACE_ADDRESSS, place.address)
                    putString(PLACE_DESCRIPTION, place.description)

                    place.customPlaceParams?.let {
                        val list = arrayListOf<VKGroup>()
                        list.addAll(it.vkGroups)
                        putParcelableArrayList(PLACE_VKGROUPS, list)
                    }
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.description_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title = arguments?.getString(PLACE_TITLE, "").orEmpty()
        val address = arguments?.getString(PLACE_ADDRESSS, "").orEmpty()
        val desc = arguments?.getString(PLACE_DESCRIPTION, "").orEmpty()
        val link = arguments?.getString(PLACE_LINK, "").orEmpty()
        val imageLink = arguments?.getString(PLACE_IMAGE, "").orEmpty()
        val groups = arguments?.getParcelableArrayList<VKGroup>(PLACE_VKGROUPS).orEmpty()

        val addressLine = view.findViewById<ViewGroup>(R.id.addressLine)
        val titleTv = view.findViewById<TextView>(R.id.title)
        val addressTv = view.findViewById<TextView>(R.id.address)
        val descTv = view.findViewById<TextView>(R.id.description)
        val btn = view.findViewById<Button>(R.id.httpOpenBtn)
        val unsubscribeBtn = view.findViewById<Button>(R.id.unsubscribeBtn)
        val image = view.findViewById<ImageView>(R.id.image)
        val groupsList = view.findViewById<RecyclerView>(R.id.groupsList)

        view.findViewById<View>(R.id.close).setOnClickListener {
            dismissAllowingStateLoss()
        }

        unsubscribeBtn.setOnClickListener {
            dismiss()
            parentFragment?.fragmentManager?.let {
                UnsubscribeDialog.create().show(it, "")
            }
        }

        if (address.isEmpty()) {
            addressLine.visibility = View.GONE
        }

        titleTv.text = title
        addressTv.text = address
        descTv.text = desc
        image.load(imageLink)

        groupsList.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        groupsList.adapter = GroupsAdapter(groups) {}

        btn.setOnClickListener {
            if (link.isNotEmpty()) {
                val chooser = Intent.createChooser(Intent(Intent.ACTION_VIEW, Uri.parse(link)), "")
                startActivity(chooser)
            }
        }
    }
}