package kiol.vkapp.utils

import android.content.Context
import android.os.Environment
import io.reactivex.Flowable
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.*

class SimpleTxtFileLoader(private val context: Context) {

    fun loadTxtFile(urlFile: String, customFileUUID: String? = null): Flowable<String> {
        val uuidFileName = customFileUUID ?: UUID.nameUUIDFromBytes(urlFile.toByteArray()).toString() + ".vkkiol"
        val cachedFile = File(context.cacheDir, uuidFileName)
        return if (cachedFile.exists()) {
            Flowable.fromCallable { cachedFile.readText() }
        } else {
            Flowable.fromCallable {
                val url = java.net.URL(urlFile)
                val reader = BufferedReader(InputStreamReader(url.openStream()))
                var content = ""
                reader.use { r ->
                    content = r.readText()
                }
                content
            }.doOnNext {
                File(context.cacheDir, uuidFileName).writeText(it)
            }
        }
    }
}