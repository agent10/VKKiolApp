package kiol.vkapp.taskc

import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import android.widget.ImageView
import kiol.vkapp.commonui.px
import kiol.vkapp.commonui.pxF

class PlaceImageView(context: Context?) : ImageView(context) {
    init {
        layoutParams = ViewGroup.LayoutParams(48.px, 48.px)
        scaleType = ScaleType.CENTER_CROP
        setBackgroundColor(Color.RED)
    }
}