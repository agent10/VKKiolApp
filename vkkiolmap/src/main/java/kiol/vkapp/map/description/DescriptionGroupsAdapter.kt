package kiol.vkapp.map.description

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import kiol.vkapp.commondata.data.VKGroup
import kiol.vkapp.map.R

class GroupsAdapter(private val groups: List<VKGroup>, private val click: (VKGroup) -> Unit) : RecyclerView.Adapter<GroupsAdapter.VH>() {

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