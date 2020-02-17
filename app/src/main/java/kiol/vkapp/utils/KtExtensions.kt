package kiol.vkapp.utils

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import timber.log.Timber

operator fun CompositeDisposable.plusAssign(d: Disposable) {
    this.add(d)
}

fun <T> measureBlock(tag: String = "", block: () -> T): T {
    val t = System.currentTimeMillis()
    Timber.d("$tag time measuring started")
    val ret = block()
    Timber.d("$tag time measured: ${System.currentTimeMillis() - t}")
    return ret
}