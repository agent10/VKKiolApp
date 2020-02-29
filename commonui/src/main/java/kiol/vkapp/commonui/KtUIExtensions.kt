package kiol.vkapp.commonui

import android.content.res.Resources
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

val Int.dp: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()
val Int.px: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

val Int.dpF: Float
    get() = (this / Resources.getSystem().displayMetrics.density)
val Int.pxF: Float
    get() = (this * Resources.getSystem().displayMetrics.density)

operator fun CompositeDisposable.plusAssign(d: Disposable) {
    this.add(d)
}