package com.udacity

import android.app.DownloadManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import com.udacity.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    //private lateinit var binding: ActivityMainBinding


    private var downloadId: Long = 0
    private lateinit var downloadOptions: RadioGroup
    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            //val id = DownloadManager.STATUS_FAILED
            val id: Long = intent?.getLongExtra(
                DownloadManager.EXTRA_DOWNLOAD_ID, -1
            ) ?: -1

            println("*** onReceive() - $id | $downloadId")

            if (downloadId == id) {
                val downloadOk = DownloadHelper.with(this@MainActivity).isSuccessful(downloadId)
                println("*** downloadOk: $downloadOk")

                loadingButton.finishDownload()
            }
        }
    }

    private lateinit var loadingButton: LoadingButton


    private lateinit var notificationManager: NotificationManager
    private lateinit var pendingIntent: PendingIntent
    private lateinit var action: NotificationCompat.Action

    private var optionIndex = -1

    private val downloadManager by lazy { getSystemService(DOWNLOAD_SERVICE) as DownloadManager }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        setupViews(binding)
        registerReceiver(downloadReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    private fun setupViews(binding: ActivityMainBinding) {
        downloadOptions = binding.contentMain.downloadOptions.apply {
            setOnCheckedChangeListener { _, index ->
                optionIndex = when (index) {
                    R.id.download_option1 -> 0
                    R.id.download_option2 -> 1
                    R.id.download_option3 -> 2
                    else -> -1
                }
            }
        }
        loadingButton = binding.contentMain.downloadButton.apply {
            //setOnClickListener { downloadSelectedFile() }
        }
    }

    private fun checkInvalidDownload() =
        if (optionIndex == INVALID_DOWNLOAD_ID) {
            Toast.makeText(
                this, R.string.message_no_download_selected, Toast.LENGTH_SHORT
            ).show()
            true
        } else {
            false
        }

    private fun downloadSelectedFile() {
        if (checkInvalidDownload()) {
            return
        }
        downloadId = DownloadHelper.with(this).download(URLS[optionIndex])
    }

    companion object {
        private const val CHANNEL_ID = "channelId"
        private val URLS = listOf(
            "https://github.com/udacity/nd940-c3-advanced-android-programming-project-starter/archive/master.zip",
            "https://github.com/bumptech/glide/archive/master.zip",
            "https://github.com/square/retrofit/archive/master.zi"
        )
        private const val INVALID_DOWNLOAD_ID = -1
    }
}