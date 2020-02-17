package kiol.vkapp

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kiol.vkapp.commondata.data.VKDocItem
import kiol.vkapp.commondata.domain.DocItem

fun ImageView.setVKPreview(docItem: DocItem) {
    val bgdColor = when (docItem.type) {
        is VKDocItem.VKDocType.Text, is VKDocItem.VKDocType.Audio, is VKDocItem.VKDocType.Unknown, is VKDocItem.VKDocType.Image, is VKDocItem.VKDocType.Gif -> R.color.doc_type_color_1
        is VKDocItem.VKDocType.Zip -> R.color.doc_type_color_2
        is VKDocItem.VKDocType.Video -> R.color.doc_type_color_3
        is VKDocItem.VKDocType.Ebook -> R.color.doc_type_color_4
    }

    val icon = when (docItem.type) {
        is VKDocItem.VKDocType.Text -> R.drawable.ic_doc_type_text
        is VKDocItem.VKDocType.Audio -> R.drawable.ic_doc_type_audio
        is VKDocItem.VKDocType.Ebook -> R.drawable.ic_doc_type_ebook
        is VKDocItem.VKDocType.Video -> R.drawable.ic_doc_type_video
        is VKDocItem.VKDocType.Zip -> R.drawable.ic_doc_type_zip
        else -> R.drawable.ic_doc_type_other
    }

    val shapeDrawable = ContextCompat.getDrawable(context, R.drawable.doc_image_bgd) as GradientDrawable
    shapeDrawable.color = ColorStateList.valueOf(ContextCompat.getColor(context, bgdColor))
    background = shapeDrawable

    setImageDrawable(null)
    val vkPreviews = docItem.images
    vkPreviews?.let {
        doOnLayout {
            val tw = measuredWidth
            val th = measuredHeight

            scaleType = ImageView.ScaleType.CENTER_CROP

            var best = vkPreviews.sizes.firstOrNull()
            vkPreviews.sizes.forEach {
                val ib = best
                if (ib == null) {
                    best = it
                } else {
                    if (it.width >= it.height) {
                        if (it.width <= tw && it.width > ib.width) best = it
                    } else {
                        if (it.height <= th && it.height > ib.height) best = it
                    }
                }
            }

            best?.let {
                Glide.with(this).load(it.src).into(this)
            }
        }
    } ?: run {
        scaleType = ImageView.ScaleType.CENTER_INSIDE
        setImageResource(icon)
    }
}

class DocsAdapter(
    recyclerView: RecyclerView,
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
        val titleTv = v.findViewById<TextView>(R.id.docTitleTv)
        val imageTv = v.findViewById<ImageView>(R.id.docImageIv)
        val infoTv = v.findViewById<TextView>(R.id.docInfoTv)
        val tagsTv = v.findViewById<TextView>(R.id.docTagsTv)
        val tagsBadgeImg = v.findViewById<ImageView>(R.id.docTagsImg)
        val menuBtn = v.findViewById<ImageButton>(R.id.docMenuBtn)
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
        } else if (holder is ProgressViewHolder) {

        }
    }
}