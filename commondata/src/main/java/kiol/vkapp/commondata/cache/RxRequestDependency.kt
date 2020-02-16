package kiol.vkapp.commondata.cache;

import io.reactivex.Flowable;

interface RxRequestDependency<P, T> {
    fun getValue(param: P?): Flowable<T>
    fun getResetListener(): Flowable<RxResponseCache<P, T>>
}