package kiol.vkapp.utils

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

operator fun CompositeDisposable.plusAssign(d: Disposable) {
    this.add(d)
}