package kiol.vkapp.commondata.domain

import kiol.vkapp.commondata.data.VKDocItem
import kiol.vkapp.commondata.data.VKSizesPreview

data class DocItem(
    val id: Int,
    val owner_id: Int, val docTitle: String, val docInfo: String, val tags: String, val type: VKDocItem.VKDocType, val images:
    VKSizesPreview?, val contentUrl: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DocItem

        if (id != other.id) return false
        if (owner_id != other.owner_id) return false
        if (docTitle != other.docTitle) return false
        if (docInfo != other.docInfo) return false
        if (tags != other.tags) return false
        if (type != other.type) return false
        if (images != other.images) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + owner_id
        result = 31 * result + docTitle.hashCode()
        result = 31 * result + docInfo.hashCode()
        result = 31 * result + tags.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + (images?.hashCode() ?: 0)
        return result
    }
}

enum class PlaceType {
    Groups, Events, Photos
}

data class Place(
    val placeType: PlaceType,
    val latitude: Float,
    val longitude: Float,
    val title: String,
    val address: String,
    val description: String,
    val photo: String
)