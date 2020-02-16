package kiol.vkapp.commondata.domain.docs

import android.text.format.DateFormat
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import kiol.vkapp.commondata.data.VKDocItem
import kiol.vkapp.commondata.domain.DocItem
import timber.log.Timber
import java.util.*
import kotlin.math.abs

class DocsUseCase {

    private companion object {
        const val PAGE_SIZE = 10L
        const val INITIAL_SIZE = 10L
    }

    private val docsCache = DocsCache()

    fun getDocs(): Flowable<List<DocItem>> {
        Timber.d("Try getDocs")
        docsCache.reset()

        return docsCache.getInitialValue(null, INITIAL_SIZE).map {
            it.map(::mapper)
        }.subscribeOn(Schedulers.io())
    }

    fun loadMore(): Flowable<List<DocItem>> {
        Timber.d("Try loadMore docs")
        return docsCache.getMoreItems(null, PAGE_SIZE).map {
            it.map(::mapper)
        }.subscribeOn(Schedulers.io())
    }

    fun canLoadMore() = docsCache.canLoadMore(null)

    private fun mapper(vkDoc: VKDocItem) = DocItem(
        vkDoc.id, vkDoc.owner_id,
        vkDoc.title,
        "${vkDoc.ext
            .toUpperCase()} · ${humanReadableByteCountSI(vkDoc.size)} · ${humanReadableTimestamp(vkDoc.date * 1000)}",
        vkDoc.tags
            ?.joinToString(", ") ?: "",
        vkDoc.getType(),
        vkDoc.preview?.photo, vkDoc.url
    )

    fun removeDoc(docItem: DocItem) {
        docsCache.removeDoc(docItem.id, docItem.owner_id)
    }

    fun updateDocName(docItem: DocItem) {
        docsCache.updateDocName(docItem.id, docItem.owner_id, docItem.docTitle)
    }

    private fun humanReadableTimestamp(timestamp: Long): String {
        val nowTime: Calendar = Calendar.getInstance()
        val neededTime: Calendar = Calendar.getInstance()
        neededTime.timeInMillis = timestamp

        return if (neededTime.get(Calendar.YEAR) == nowTime.get(Calendar.YEAR)) {
            when {
                nowTime.get(Calendar.DATE) == neededTime.get(Calendar.DATE) -> {
                    "Сегодня"
                }
                nowTime.get(Calendar.DATE) - neededTime.get(Calendar.DATE) == 1 -> {
                    "Вчера"
                }
                else -> {
                    DateFormat.format("dd MMMM", neededTime).toString()
                }
            }
        } else {
            DateFormat.format("yyyy dd MMMM", neededTime).toString()
        }
    }

    private fun humanReadableByteCountSI(bytes: Long): String? {
        val s = if (bytes < 0) "-" else ""
        var b =
            if (bytes == Long.MIN_VALUE) Long.MAX_VALUE else abs(bytes)
        return if (b < 1000L) "$bytes Б" else if (b < 999950L) String.format(
            "%s%.1f КБ",
            s,
            b / 1e3
        ) else if (1000.let { b /= it; b } < 999950L) String.format(
            "%s%.1f МБ",
            s,
            b / 1e3
        ) else if (1000.let { b /= it; b } < 999950L) String.format(
            "%s%.1f ГБ",
            s,
            b / 1e3
        ) else if (1000.let { b /= it; b } < 999950L) String.format(
            "%s%.1f ТБ",
            s,
            b / 1e3
        ) else if (1000.let { b /= it; b } < 999950L) String.format(
            "%s%.1f ПБ",
            s,
            b / 1e3
        ) else String.format("%s%.1f ЭБ", s, b / 1e6)
    }
}