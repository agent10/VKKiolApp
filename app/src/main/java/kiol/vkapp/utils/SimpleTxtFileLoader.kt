package kiol.vkapp.utils

import android.content.Context
import io.reactivex.Flowable
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.*


class SimpleTxtFileLoader(private val context: Context) {

    private val httpClient = OkHttpClient()

    class NoTxtBodyException : Exception("Can't load txt file")

    fun loadTxtFile(urlFile: String, customFileUUID: String? = null): Flowable<List<String>> {
        val uuidFileName = customFileUUID ?: UUID.nameUUIDFromBytes(urlFile.toByteArray()).toString() + ".vkkiol"
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

    private fun <T> measureBlock(tag: String = "", block: () -> T): T {
        val t = System.currentTimeMillis()
        Timber.d("$tag time measuring started")
        val ret = block()
        Timber.d("$tag time measured: ${System.currentTimeMillis() - t}")
        return ret
    }
}