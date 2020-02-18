package kiol.vkapp.utils

import android.content.Context
import io.reactivex.Flowable
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.File
import java.util.*


class SimpleTxtFileLoader(private val context: Context) {

    companion object {
        const val fileSuffix = ".vkkiol"
    }

    private val httpClient = OkHttpClient()

    class NoTxtBodyException : Exception("Can't load txt file")

    fun loadTxtFile(urlFile: String, customFileUUID: String? = null): Flowable<List<String>> {
        val uuidFileName = customFileUUID ?: UUID.nameUUIDFromBytes(urlFile.toByteArray()).toString() + fileSuffix
        val cachedFile = File(context.filesDir, uuidFileName)
        return if (cachedFile.exists() && cachedFile.length() != 0L) {
            Flowable.fromCallable { cachedFile.readLines() }
        } else {
            Flowable.fromCallable {
                val request: Request = Request.Builder()
                    .url(urlFile)
                    .build()
                val respBody = httpClient.newCall(request).execute().body()
                respBody?.let {
                    val content = arrayListOf("")
                    val reader = BufferedReader(it.byteStream().reader())
                    reader.use { r ->
                        File(context.filesDir, uuidFileName).writer().use { writer ->
                            r.forEachLine {
                                content.add(it)
                                writer.write(it)
                                writer.write("\n")
                            }
                        }
                    }
                    content.toList()
                } ?: throw NoTxtBodyException()
            }
        }
    }
}