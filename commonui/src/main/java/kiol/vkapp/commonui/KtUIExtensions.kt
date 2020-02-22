package kiol.vkapp.commonui

import android.content.res.Resources

val Int.dp: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()
val Int.px: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

val Int.dpF: Float
    get() = (this / Resources.getSystem().displayMetrics.density)
val Int.pxF: Float
    get() = (this * Resources.getSystem().displayMetrics.density)