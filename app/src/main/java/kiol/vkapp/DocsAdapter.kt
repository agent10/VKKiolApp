package kiol.vkapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kiol.vkapp.commondata.domain.DocItem
import kiol.vkapp.utils.setVKPreview

class DocsAdapter(
    val onBind: (DocViewHolder, DocItem) -> Unit,
    val loadMore: () -> Unit
) : ListAdapter<DocItem, RecyclerView.ViewHolder>(diffUtil) {

    companion object {
        private val diffUtil = object : DiffUtil.ItemCallback<DocItem>() {
            override fun areItemsTheSame(oldItem: DocItem, newItem: DocItem) = oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: DocItem, newItem: DocItem): Boolean {
                return oldItem == newItem
            }
        }

        private const val VIEW_TYPE_ITEM = 0
        private const val VIEW_TYPE_LOADING = 1

        private const val LOAD_MORE_THRESHOLD = 10
    }

    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            if (dy > 0) {
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val lastPos = layoutManager.findLastVisibleItemPosition()
                val count = layoutManager.itemCount

                if (count - lastPos <= LOAD_MORE_THRESHOLD) {
                    loadMore.invoke()
                }
            }
        }
    }

    class DocViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val titleTv: TextView = v.findViewById(R.id.docTitleTv)
        val imageTv: ImageView = v.findViewById(R.id.docImageIv)
        val infoTv: TextView = v.findViewById(R.id.docInfoTv)
        val tagsTv: TextView = v.findViewById(R.id.docTagsTv)
        val tagsBadgeImg: ImageView = v.findViewById(R.id.docTagsImg)
        val menuBtn: ImageButton = v.findViewById(R.id.docMenuBtn)
    }

    class ProgressViewHolder(v: View) : RecyclerView.ViewHolder(v)

    var refreshing = false
        set(value) {
            field = value
            if (value) {
                notifyItemChanged(itemCount)
            } else {
                notifyItemRemoved(itemCount)
            }
        }

    init {
        setHasStableIds(true)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        recyclerView.addOnScrollListener(scrollListener)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        recyclerView.removeOnScrollListener(scrollListener)
    }

    override fun getItemViewType(position: Int): Int {
        return if (refreshing && position == itemCount - 1) VIEW_TYPE_LOADING else VIEW_TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_ITEM -> DocViewHolder(inflater.inflate(R.layout.doc_item_layout, parent, false))
            VIEW_TYPE_LOADING -> ProgressViewHolder(inflater.inflate(R.layout.doc_item_loading_layout, parent, false))
            else -> throw IllegalArgumentException("View type not found")
        }
    }

    override fun getItemCount(): Int {
        return if (refreshing) super.getItemCount() + 1 else super.getItemCount()
    }

    override fun getItemId(position: Int): Long {
        return if (refreshing && position == itemCount - 1) -1 else getItem(position).id.toLong()
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is DocViewHolder) {
            val item = getItem(position)

            holder.imageTv.setVKPreview(item)
            holder.imageTv.clipToOutline = true

            holder.titleTv.text = item.docTitle
            holder.infoTv.text = item.docInfo
            if (item.tags.isEmpty()) {
                holder.tagsBadgeImg.visibility = View.GONE
                holder.tagsTv.visibility = View.GONE
            } else {
                holder.tagsTv.text = item.tags
                holder.tagsTv.visibility = View.VISIBLE
                holder.tagsBadgeImg.visibility = View.VISIBLE
            }

            onBind(holder, item)
        }
    }
}