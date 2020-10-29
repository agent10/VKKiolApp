package kiol.vkapp.map.unsubscribe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import kiol.vkapp.commondata.data.VKGroup
import kiol.vkapp.map.R

private val diffCallback = object : DiffUtil.ItemCallback<VKGroup>() {
    override fun areItemsTheSame(oldItem: VKGroup, newItem: VKGroup): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: VKGroup, newItem: VKGroup): Boolean {
        return oldItem == newItem
    }
}

class UnsubscribeGroupsAdapter(groups: List<VKGroup>, private val click: (VKGroup, List<VKGroup>) -> Unit) :
    ListAdapter<VKGroup, UnsubscribeGroupsAdapter.VH>(diffCallback) {

    class VH(item: View) : RecyclerView.ViewHolder(item) {
        val image: ImageView = item.findViewById(R.id.image)
        val title: TextView = item.findViewById(R.id.title)
    }

    private val selected = hashSetOf<VKGroup>()

    init {
        submitList(groups)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.vk_group_item_layout, parent, false)
        )
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val group = currentList[position]

        holder.image.isSelected = selected.contains(group)
        holder.itemView.setOnClickListener {
            if (selected.contains(group)) {
                holder.image.isSelected = false
                selected -= group
            } else {
                holder.image.isSelected = true
                selected += group
            }
            click(group, selected.toList())
        }
        holder.title.text = group.name
        holder.image.load(group.photo_100) {
            crossfade(true)
            transformations(CircleCropTransformation())
        }
    }

    fun removeSelectedGroups(): List<VKGroup> {
        val currList = currentList.toMutableList()
        val selectedList = selected.toList()
        selected.clear()
        val diff = currList - selectedList
        submitList(diff)
        return diff
    }
}
