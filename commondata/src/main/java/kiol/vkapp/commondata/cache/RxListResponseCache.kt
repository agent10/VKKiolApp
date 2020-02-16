package kiol.vkapp.commondata.cache

import io.reactivex.Flowable

abstract class RxListResponseCache<Param, Item, ItemCollection : MutableCollection<Item>> :
    RxResponseCache<Param, ItemCollection>() {

    private val isDataFull = HashMap<Param?, Boolean?>()

    @Volatile
    private var isLoadingMore = false

    @Synchronized
    private fun getValue(param: Param?, offset: Long, count: Long): Flowable<ItemCollection> {
        fun getNewDataDefault() = getNewData(null, param, offset, count)

        return if (hasCacheDataByParam(param)) {
            when (isDataValid(param, offset, count)) {
                true -> Flowable.just(getCacheDataByParam(param) ?: return getNewDataDefault())
                false -> getNewData(getCacheDataByParam(param), param, offset, count)
            }
        } else getNewDataDefault()
    }

    @Synchronized
    private fun getNewData(currentData: ItemCollection?, param: Param?, offset: Long, count: Long) =
        Flowable.fromPublisher(resolveDependenciesAndNext(param, combine(currentData, param, offset, count)))
            .doOnNext {
                putDataInCacheByParam(param, it)
            }

    @Synchronized
    private fun isDataValid(param: Param?, offset: Long, count: Long) = getDataCountInCache(param) >= offset + count

    private fun combine(currentData: ItemCollection?, param: Param?, offset: Long, count: Long): Flowable<ItemCollection> {
        isLoadingMore = true
        return fetch(param, offset, count).doOnNext {
            isLoadingMore = false
            if (it.size < count) {
                onLoadedNotEqualsCount(param, it.size, count)
            }
        }.flatMap { newData ->
            currentData?.addAll(newData)
            when (offset != 0L && currentData != null) {
                true -> return@flatMap Flowable.just(currentData)
                false -> return@flatMap Flowable.just(newData)
            }
        }
    }

    override fun fetch(param: Param?): Flowable<ItemCollection> {
        throw RuntimeException("Use other fetch method")
    }

    fun getInitialValue(param: Param?, minCount: Long): Flowable<ItemCollection> {
        val count = getDataCountInCache(param)
        val finalCount = if (count >= minCount) count else minCount
        return getValue(param, 0, finalCount)
    }

    fun getMoreItems(param: Param?, nextCount: Long): Flowable<ItemCollection> {
        return if (hasCacheDataByParam(param)) getValue(param, getDataCountInCache(param), nextCount)
        else getInitialValue(param, nextCount)
    }

    @Synchronized
    fun getDataCountInCache(param: Param?) = getCacheDataByParam(param)?.size?.toLong() ?: 0L

    protected abstract fun fetch(param: Param?, offset: Long, count: Long): Flowable<ItemCollection>

    protected fun onLoadedNotEqualsCount(param: Param?, loaded: Int, count: Long) {
        isDataFull[param] = true
    }

    fun canLoadMore(param: Param?): Boolean {
        return !isLoadingMore && isDataFull[param]?.not() ?: true
    }

    @Synchronized
    override fun reset() {
        super.reset()
        isLoadingMore = false
        isDataFull.clear()
    }
}