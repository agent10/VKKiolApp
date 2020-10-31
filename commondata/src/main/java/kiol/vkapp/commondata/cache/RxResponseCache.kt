package kiol.vkapp.commondata.cache

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.processors.PublishProcessor
import timber.log.Timber

abstract class RxResponseCache<P, T> : RxRequestDependency<P, T> {

    protected val className: String = this.javaClass.simpleName + "@" + Integer.toHexString(this.hashCode())

    private val dependencies = HashSet<RxRequestDependency<in Any, in Any>>()
    protected var cacheData = HashMap<P?, T>()

    private var isCacheEnabled = true

    private val resetProcessor = PublishProcessor.create<RxResponseCache<P, T>>()


    @Synchronized
    override fun getValue(param: P?): Flowable<T> {
        getCacheDataByParam(param)?.run { return@getValue Flowable.just(this) }

        return Flowable.fromPublisher(resolveDependenciesAndNext(param, fetch(param)))
            .doOnNext { putDataInCacheByParam(param, it) }
            .doOnComplete { Timber.d("$className:getValue: the request is finished"); }
            .doOnError { Timber.e("$className:getValue: ${it.message}"); }
    }

    protected open fun getParamForCache(param: P?): Any? = param

    protected abstract fun fetch(param: P?): Flowable<T>

    fun update(param: P): Completable = Completable.fromPublisher(getValue(param))

    protected fun resolveDependenciesAndNext(externalParam: P?, andThenPublisher: Flowable<T>): Flowable<T> {
        Timber.d("Start resolve dependency for cache: $className")

        return Flowable.fromIterable(dependencies)
            .map { Pair(it, getParamForDependency<Any>(it, externalParam)) }
            .flatMapCompletable { pairRxDependencyAndParam ->
                try {
                    return@flatMapCompletable Completable.fromPublisher(
                        pairRxDependencyAndParam.first.getValue(
                            pairRxDependencyAndParam.second
                        )
                    )
                        .doOnError {
                            Timber.e(
                                "Error occurred during resolve dependency for cache: $className, " +
                                        "dependency: $pairRxDependencyAndParam.first, error: $it"
                            )
                        }
                        .doOnSubscribe {
                            Timber.d("Request dependency: ${pairRxDependencyAndParam.first}, done current cache: $className")
                        }
                } catch (e: Exception) {
                    return@flatMapCompletable Completable.complete()
                }
            }
            .doOnError {
                Timber.e("Error occurred during resolve dependencies for cache: $className, error: $it")
                reset();
            }
            .doOnComplete { Timber.d("All request dependencies resolved for cache: $className") }
            .andThen(andThenPublisher)
            .doOnNext { Timber.d("Received next request for cache. onNext: $it, $className") }
    }

    protected open fun <RP> getParamForDependency(dependency: RxRequestDependency<*, *>, externalParam: P?): RP? = null

    override fun getResetListener(): Flowable<RxResponseCache<P, T>> = resetProcessor

    @Synchronized
    open fun reset() {
        Timber.d("Cache was reset, cache: $className")
        cacheData.clear()
        resetProcessor.onNext(this)
    }

    protected fun hasCacheDataByParam(param: P?) = isCacheEnabled && cacheData.keys.contains(param) && cacheData[param] != null

    protected fun getCacheDataByParam(param: P?): T? = when (isCacheEnabled && hasCacheDataByParam(param)) {
        true -> cacheData[param]
        false -> null
    }

    protected fun putDataInCacheByParam(param: P?, data: T) {
        if (!isCacheEnabled) return
        cacheData[param] = data
    }

    protected fun enableCache(enable: Boolean) {
        this.isCacheEnabled = enable
    }
}
