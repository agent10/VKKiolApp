package kiol.vkapp.docs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.processors.PublishProcessor
import kiol.vkapp.commondata.SimpleResourceProvider
import kiol.vkapp.commondata.domain.DocItem
import kiol.vkapp.commondata.domain.docs.DocsUseCase
import kiol.vkapp.commonui.plusAssign
import kiol.vkapp.commonui.plusAssign
import kiol.vkapp.docs.utils.DocsDownloadManager
import timber.log.Timber

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val docsUseCase = DocsUseCase(SimpleResourceProvider(app))
    private val docsDownloadManager = DocsDownloadManager(app)

    private val compositeDisposable = CompositeDisposable()

    private val _uiEvents: PublishProcessor<UIEvent> = PublishProcessor.create()
    val uiEvents: Flowable<UIEvent> = _uiEvents

    sealed class UIEvent {
        data class DocsLoaded(val items: List<DocItem>, val initial: Boolean, val hasMore: Boolean) : UIEvent()
        data class DocsError(val initial: Boolean, val e: Throwable) : UIEvent()
    }

    init {
        docsDownloadManager.register()
    }

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.clear()
        docsDownloadManager.unregister()
    }

    fun loadDocs() {
        compositeDisposable += (docsUseCase.getDocs().observeOn(AndroidSchedulers.mainThread()).subscribe({
            _uiEvents.onNext(UIEvent.DocsLoaded(it, true, docsUseCase.canLoadMore()))
            Timber.d("getDocs success $it")
        }, {
            _uiEvents.onNext(UIEvent.DocsError(true, it))
            Timber.e("getDocs failed $it")
        }))
    }

    fun loadMoreDocs(): Boolean {
        if (docsUseCase.canLoadMore()) {
            compositeDisposable += (docsUseCase.loadMore().observeOn(AndroidSchedulers.mainThread()).subscribe({
                _uiEvents.onNext(UIEvent.DocsLoaded(it, false, docsUseCase.canLoadMore()))
                Timber.d("getDocs success $it")
            }, {
                _uiEvents.onNext(UIEvent.DocsError(false, it))
                Timber.e("getDocs failed $it")
            }))
            return true
        }
        return false
    }

    fun downloadDoc(docItem: DocItem) {
        docsDownloadManager.download(docItem)
    }

    fun renameDoc(newTitle: String, docItem: DocItem, docs: List<DocItem>): List<DocItem> {
        val ndi = docItem.copy(docTitle = newTitle)
        docsUseCase.updateDocName(ndi)
        return docs.toMutableList().apply {
            set(indexOf(docItem), ndi)
        }
    }

    fun removeDoc(docItem: DocItem, docs: List<DocItem>): List<DocItem> {
        docsUseCase.removeDoc(docItem)
        return docs.toMutableList().apply {
            remove(docItem)
        }
    }
}