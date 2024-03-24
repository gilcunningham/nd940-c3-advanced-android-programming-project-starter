package com.udacity.helper

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity

class DownloadHelper private constructor(private val context: Context) {

    private val downloadManager by lazy {
        context.getSystemService(AppCompatActivity.DOWNLOAD_SERVICE) as DownloadManager
    }

    fun download(url: String, title: String): Long {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(title)
            .setRequiresCharging(false)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
        return downloadManager.enqueue(request)
    }

    fun isSuccessful(id: Long): Boolean {
        val dmQuery = DownloadManager.Query().apply { setFilterById(id) }
        return runCatching {
            downloadManager.query(dmQuery).use { cursor ->
                if (cursor != null && cursor.count > 0) {
                    val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    return cursor.moveToFirst() && cursor.getInt(columnIndex) == DownloadManager.STATUS_SUCCESSFUL
                }
            }
        }.isSuccess
    }

    fun fileName(id: Long) : String {
        val dmQuery = DownloadManager.Query().apply { setFilterById(id) }
        return runCatching {
            downloadManager.query(dmQuery).use { cursor ->
                if (cursor != null && cursor.count > 0) {
                    val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE)
                    cursor.moveToFirst()
                    return cursor.getString(columnIndex)
                }
            }
        }.toString()
    }

    companion object {
        fun with(context: Context): DownloadHelper {
            return DownloadHelper(context)
        }
    }
}