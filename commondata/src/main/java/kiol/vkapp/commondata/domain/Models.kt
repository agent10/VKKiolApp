package kiol.vkapp.commondata.domain

import kiol.vkapp.commondata.data.VKDocItem
import kiol.vkapp.commondata.data.VKSizesPreview

data class DocItem(
    val id: Int,
    val owner_id: Int, val docTitle: String, val docInfo: String, val tags: String, val type: VKDocItem.VKDocType, val images:
    VKSizesPreview?
)