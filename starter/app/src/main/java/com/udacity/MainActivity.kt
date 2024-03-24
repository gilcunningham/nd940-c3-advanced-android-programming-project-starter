package com.udacity

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.udacity.databinding.ActivityMainBinding
import com.udacity.helper.DownloadHelper
import com.udacity.helper.NotificationHelper

class MainActivity : AppCompatActivity() {

    private var downloadId: Long = 0
    private lateinit var downloadOptions: RadioGroup
    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id: Long = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) ?: -1
            if (downloadId == id) {
                loadingButton.stop()
                showNotification()
            }
        }
    }
    private lateinit var loadingButton: LoadingButton
    private var optionIndex = -1

    private fun checkInvalidDownload() = if (optionIndex == INVALID_DOWNLOAD_ID) {
        Toast.makeText(
            this,
            R.string.toast_message_select_download,
            Toast.LENGTH_SHORT
        ).show()
        loadingButton.stop()
        true
    } else {
        false
    }

    private fun downloadSelectedFile() {
        if (checkInvalidDownload()) {
            return
        }
        downloadId = DownloadHelper.with(this)
            .download(URLS[optionIndex], getString(TITLES[optionIndex]))
    }

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
            setOnClickListener { downloadSelectedFile() }
        }
    }

    private fun showNotification() {
        val contentIntent = DetailActivity.makeIntent(this, downloadId)
        NotificationHelper.with(this).sendNotification(contentIntent)
    }

    companion object {
        private val URLS = listOf(
            "https://github.com/bumptech/glide/archive/master.zip",
            "https://github.com/udacity/nd940-c3-advanced-android-programming-project-starter/archive/master.zip",
            "https://github.com/square/retrofit/archive/master.zi" // broken
        )
        private val TITLES = listOf(
            R.string.download_option1,
            R.string.download_option2,
            R.string.download_option3,
        )
        private const val INVALID_DOWNLOAD_ID = -1
    }
}