package kiol.vkapp.commondata.data


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

data class VKImagePreview(val src: String, val width: Int, val height: Int)

data class VKPhotoPreview(val photo: VKSizesPreview?)

data class VKSizesPreview(val sizes: List<VKImagePreview>)