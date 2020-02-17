package kiol.vkapp.utils

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import com.bumptech.glide.Glide
import kiol.vkapp.R
import kiol.vkapp.commondata.data.VKDocItem
import kiol.vkapp.commondata.domain.DocItem

private fun DocItem.backgroundColor(): Int {
    return when (type) {
        is VKDocItem.VKDocType.Zip -> R.color.doc_type_color_2
        is VKDocItem.VKDocType.Video -> R.color.doc_type_color_3
        is VKDocItem.VKDocType.Ebook -> R.color.doc_type_color_4
        else -> R.color.doc_type_color_1
    }
}

private fun DocItem.foregroundIcon(): Int {
    return when (type) {
        is VKDocItem.VKDocType.Text -> R.drawable.ic_doc_type_text
        is VKDocItem.VKDocType.Audio -> R.drawable.ic_doc_type_audio
        is VKDocItem.VKDocType.Ebook -> R.drawable.ic_doc_type_ebook
        is VKDocItem.VKDocType.Video -> R.drawable.ic_doc_type_video
        is VKDocItem.VKDocType.Zip -> R.drawable.ic_doc_type_zip
        else -> R.drawable.ic_doc_type_other
    }
}

fun ImageView.setVKPreview(docItem: DocItem) {
    val bgdColor = docItem.backgroundColor()
    val icon = docItem.foregroundIcon()

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