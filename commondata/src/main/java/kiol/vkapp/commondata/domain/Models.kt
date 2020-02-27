package kiol.vkapp.commondata.domain

import kiol.vkapp.commondata.data.*

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
    val id: Int,
    val placeType: PlaceType,
    val latitude: Float,
    val longitude: Float,
    val title: String,
    val address: String,
    val description: String,
    val photo: String,
    val sizes: List<VKImagePreview>? = null
) {
    fun createLink(): String {
        var link = "https://www.vk.com/"
        link += if (placeType == PlaceType.Groups) {
            "club$id"
        } else {
            "event$id"
        }
        return link
    }
}

fun VKGroup.convert(placeType: PlaceType): Place {
    return Place(
        id,
        placeType,
        place.latitude, place.longitude,
        place.title,
        place.address.orEmpty(),
        description.orEmpty(),
        place.group_photo.orEmpty()
    )
}

fun VKPhoto.convert(placeType: PlaceType): Place {
    return Place(-1, placeType, lat, long, "", "", text, sizes.getMSize(), sizes)
}