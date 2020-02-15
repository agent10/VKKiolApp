package kiol.vkapp.commondata.data


data class VKDocItem(
    val id: Int,
    val owner_id: Int,
    val title: String, val size: Long, val ext: String, val date: Long, val tags: List<String>?, val type: Int,
    val preview: VKPhotoPreview?
) {
    enum class VKDocType {
        Text, Zip, Gif, Image, Audio, Video, Ebook, Unknown
    }

    fun getType() = when (type) {
        1 -> VKDocType.Text
        2 -> VKDocType.Zip
        3 -> VKDocType.Gif
        4 -> VKDocType.Image
        5 -> VKDocType.Audio
        6 -> VKDocType.Video
        7 -> VKDocType.Ebook
        else -> VKDocType.Unknown
    }
}

data class VKResponse<T>(val response: T)

data class VKListContainerResponse<T>(val count: Int, val items: List<T>)

data class VKImagePreview(val src: String, val width: Int, val height: Int)

data class VKPhotoPreview(val photo: VKSizesPreview?)

data class VKSizesPreview(val sizes: List<VKImagePreview>)