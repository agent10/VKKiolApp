package kiol.vkapp.docs.utils

import android.app.Application
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import kiol.vkapp.commondata.domain.DocItem
import kiol.vkapp.docs.R

class DocsDownloadManager(private val app: Application) {

    private val downloadIdsList = arrayListOf<Pair<Long, String>>()

    private val onDownloadComplete: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            val item = downloadIdsList.firstOrNull { it.first == id }
            if (item != null) {
                Toast.makeText(app, "${app.getString(R.string.dm_completed)}\n${item.second}", Toast.LENGTH_SHORT).show()
                downloadIdsList.removeAll {
                    it.first == item.first
                }
            }
        }
    }

    fun download(docItem: DocItem) {
        val request =
            DownloadManager.Request(Uri.parse(docItem.contentUrl))
                .setTitle(docItem.docTitle)
                .setDescription(app.getString(R.string.dm_title))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, docItem.docTitle)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(false)

        val id = (app.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
        downloadIdsList += Pair(id, docItem.docTitle)
    }

    fun register() {
        app.registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

    }

    fun unregister() {
        app.unregisterReceiver(onDownloadComplete)
    }
}