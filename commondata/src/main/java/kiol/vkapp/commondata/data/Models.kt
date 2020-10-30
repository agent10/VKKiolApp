package kiol.vkapp.commondata.data

import android.os.Parcelable
import kiol.vkapp.commondata.domain.PlaceType
import kotlinx.android.parcel.Parcelize


data class VKDocItem(
    val id: Int,
    val owner_id: Int,
    val title: String, val size: Long, val ext: String, val date: Long, val tags: List<String>?, val type: Int,
    val preview: VKPhotoPreview?, val url: String
) {

    sealed class VKDocType(open val ext: String) {
        data class Text(override val ext: String) : VKDocType(ext)
        data class Zip(override val ext: String) : VKDocType(ext)
        data class Gif(override val ext: String) : VKDocType(ext)
        data class Image(override val ext: String) : VKDocType(ext)
        data class Audio(override val ext: String) : VKDocType(ext)
        data class Video(override val ext: String) : VKDocType(ext)
        data class Ebook(override val ext: String) : VKDocType(ext)
        data class Unknown(override val ext: String) : VKDocType(ext)
    }

    fun getType() = when (type) {
        1 -> VKDocType.Text(ext)
        2 -> VKDocType.Zip(ext)
        3 -> VKDocType.Gif(ext)
        4 -> VKDocType.Image(ext)
        5 -> VKDocType.Audio(ext)
        6 -> VKDocType.Video(ext)
        7 -> VKDocType.Ebook(ext)
        else -> VKDocType.Unknown(ext)
    }
}

data class VKResponse<T>(val response: T)

data class VKListContainerResponse<T>(val count: Int, val items: List<T>)

data class VKImagePreview(val src: String, val width: Int, val height: Int, val type: String)

data class VKPhotoPreview(val photo: VKSizesPreview?)

data class VKSizesPreview(val sizes: List<VKImagePreview>)

fun List<VKImagePreview>?.getXSize() = getSize("x")
fun List<VKImagePreview>?.getMSize() = getSize("m")

fun List<VKImagePreview>?.getSize(type: String): String {
    this?.let {
        return it.firstOrNull { it.type == type }?.src.orEmpty()
    }
    return ""
}

data class VKPhoto(
    val text: String,
    val lat: Float = Float.MIN_VALUE,
    val long: Float = Float.MIN_VALUE,
    val photo_130: String = "",
    val sizes:
    List<VKImagePreview>? = null
)

@Parcelize
data class VKGroup(
    val id: Int,
    val name: String,
    val place: VKGroupPlace?, val description: String?, val photo_100: String
) : Parcelable {

    fun createLink(): String {
        var link = "https://www.vk.com/"
        link += "club$id"
        return link
    }
}

@Parcelize
data class VKGroupPlace(
    val title: String, val latitude: Float = Float.MIN_VALUE, val longitude: Float = Float.MIN_VALUE,
    val group_photo: String?, val address: String?
) : Parcelable

