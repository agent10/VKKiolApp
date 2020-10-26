package kiol.vkapp.map

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
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
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kiol.vkapp.commondata.data.VKGroup
import kiol.vkapp.commondata.domain.Place
import kiol.vkapp.commondata.domain.PlaceType
import kiol.vkapp.commondata.domain.groups.GroupsUseCase
import kiol.vkapp.commonui.viewLifecycleLazy
import kiol.vkapp.map.databinding.UnsubscribeDialogBinding

class UnsubscribeGroupsAdapter(val groups: List<VKGroup>, private val click: (VKGroup, List<VKGroup>) -> Unit) : RecyclerView
.Adapter<UnsubscribeGroupsAdapter.VH>
    () {

    class VH(item: View) : RecyclerView.ViewHolder(item)

    private val selected = hashSetOf<VKGroup>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(LayoutInflater.from(parent.context).inflate(R.layout.vk_group_item_layout, parent, false))
    }

    override fun getItemCount() = groups.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val group = groups[position]
        holder.itemView.setOnClickListener {
            if (selected.contains(group)) {
                holder.itemView.background = ColorDrawable(Color.TRANSPARENT)
                selected -= group
            } else {
                holder.itemView.background = ColorDrawable(Color.BLUE)
                selected += group
            }
            click(groups[position], selected.toList())
        }
        val title = holder.itemView.findViewById<TextView>(R.id.title)
        title.text = groups[position].name

        holder.itemView.findViewById<ImageView>(R.id.image).load(groups[position].photo_100) {
            crossfade(true)
            transformations(CircleCropTransformation())
        }
    }
}

class UnsubscribeDialog : BottomSheetDialogFragment() {

    companion object {

        fun create(): UnsubscribeDialog {
            return UnsubscribeDialog()
        }
    }

    private val binding by viewLifecycleLazy {
        UnsubscribeDialogBinding.bind(requireView())
    }

    private val groupsUseCase = GroupsUseCase()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.unsubscribe_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.groups.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)

        binding.unsubscribeBtn.isEnabled = false
        binding.unsubscribeBtn.setOnClickListener {

        }
        val d = groupsUseCase.getGroups().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe({
            binding.groups.adapter = UnsubscribeGroupsAdapter(it) { _, selected ->
                binding.unsubscribeBtn.isEnabled = selected.isNotEmpty()
                if (selected.isNotEmpty()) {
                    binding.unsubscribeBtn.text = "Unsubscribe (${selected.size})"
                } else {
                    binding.unsubscribeBtn.text = "Unsubscribe"
                }
            }
        }, {

        })
    }
}