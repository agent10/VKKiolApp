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
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.api.load
import com.bumptech.glide.Glide
import kiol.vkapp.commondata.data.VKDocItem
import kiol.vkapp.commondata.domain.DocItem

fun ImageView.setVKPreview2(docItem: DocItem) {
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
//                load(it.src)
            }
        }
    } ?: run {
        scaleType = ImageView.ScaleType.CENTER_INSIDE
        setImageResource(icon)
    }
}

class DocsAdapter(val onBind: (DocViewHolder, DocItem) -> Unit) : ListAdapter<DocItem, DocsAdapter.DocViewHolder>(diffUtil) {

    companion object {
        private val diffUtil = object : DiffUtil.ItemCallback<DocItem>() {
            override fun areItemsTheSame(oldItem: DocItem, newItem: DocItem) = oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: DocItem, newItem: DocItem): Boolean {
                val a = oldItem.equals(newItem)
                return a
            }
        }
    }

    class DocViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val titleTv = v.findViewById<TextView>(R.id.docTitleTv)
        val imageTv = v.findViewById<ImageView>(R.id.docImageIv)
        val infoTv = v.findViewById<TextView>(R.id.docInfoTv)
        val tagsTv = v.findViewById<TextView>(R.id.docTagsTv)
        val menuBtn = v.findViewById<ImageButton>(R.id.docMenuBtn)
    }

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocViewHolder {
        return DocViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.doc_item_layout, parent, false))
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).id.toLong()
    }

    override fun onBindViewHolder(holder: DocViewHolder, position: Int) {
        val item = getItem(position)

        holder.imageTv.setVKPreview2(item)
        holder.imageTv.clipToOutline = true

        holder.titleTv.text = item.docTitle
        holder.infoTv.text = item.docInfo
        if (item.tags.isEmpty()) {
            holder.tagsTv.visibility = View.GONE
        } else {
            holder.tagsTv.text = item.tags
            holder.tagsTv.visibility = View.VISIBLE
        }

        onBind(holder, item)
    }
}