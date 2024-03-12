package com.udacity

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity

class DownloadHelper private constructor(private val context: Context) {

    private val downloadManager by lazy {
        context.getSystemService(AppCompatActivity.DOWNLOAD_SERVICE) as DownloadManager
    }

    fun isSuccessful(id: Long) : Boolean {
        val dmQuery = DownloadManager.Query().apply { setFilterById(id) }
        dmQuery.setFilterById(id)
        return runCatching {
            downloadManager.query(dmQuery).use { cursor ->
                if (cursor != null && cursor.count > 0) {
                    val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    return if (cursor.moveToFirst() && cursor.getInt(columnIndex) == DownloadManager.STATUS_SUCCESSFUL) {
                        println("SUCCESS")
                        true
                    } else {
                        println("FAIL")
                        false
                    }
                }
            }
        }.isSuccess
    }

    fun download(url: String): Long {
        val request =
            DownloadManager.Request(Uri.parse(url))
                .setTitle(context.getString(R.string.notification_title))
                .setDescription(context.getString(R.string.notification_description))
                .setRequiresCharging(false)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
        return downloadManager.enqueue(request)
    }

    companion object {
        fun with(context: Context) : DownloadHelper {
            return DownloadHelper(context)
        }
    }
}