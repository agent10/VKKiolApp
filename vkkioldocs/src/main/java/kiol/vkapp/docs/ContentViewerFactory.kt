package kiol.vkapp.docs

import androidx.fragment.app.Fragment
import kiol.vkapp.commondata.data.VKDocItem.VKDocType.*
import kiol.vkapp.commondata.domain.DocItem
import kiol.vkapp.docs.viewers.ImageViewerFragment
import kiol.vkapp.docs.viewers.VideoViewerFragment
import kiol.vkapp.docs.viewers.textviewer.TextViewerFragment

class ViewerNotAvailable(docItem: DocItem) : Exception("Viewer not available for type: ${docItem.type}")

class ContentViewerFactory {

    @Throws(ViewerNotAvailable::class)
    fun create(docItem: DocItem): Fragment {
        return when (docItem.type) {
            is Text -> TextViewerFragment.create(docItem)
            is Image, is Gif -> ImageViewerFragment.create(docItem)
            is Video -> VideoViewerFragment.create(docItem)
            is Audio -> VideoViewerFragment.create(docItem)
            else -> throw ViewerNotAvailable(docItem)
        }
    }
}