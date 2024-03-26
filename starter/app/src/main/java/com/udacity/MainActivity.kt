package com.udacity

import android.Manifest
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.udacity.databinding.ActivityMainBinding
import com.udacity.helper.DownloadHelper
import com.udacity.helper.NotificationHelper

class MainActivity : AppCompatActivity() {

    private var downloadId: Long = 0
    private lateinit var downloadOptions: RadioGroup
    private lateinit var rootView: View
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

    // note: credit to this post for providing concise solution
    // https://medium.com/@shaikabdullafaizal/android-13-notification-runtime-permission-f91bec2fc256
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(
                this, R.string.notifications_permission_granted, Toast.LENGTH_SHORT
            ).show()
        } else {
            Snackbar.make(
                rootView,
                getString(R.string.notifications_permission_required),
                Snackbar.LENGTH_INDEFINITE
            ).setAction(getString(R.string.notifications_permission_goto_settings)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val settingsIntent: Intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                    startActivity(settingsIntent)
                }
            }.show()
        }
    }

    fun checkNotificationsPermissions() {
        if (!NotificationHelper.with(this).areNotificationsEnabled()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun downloadSelectedFile() {
        if (checkInvalidDownload()) {
            return
        }
        downloadId = DownloadHelper.with(this).download(
            URLS[optionIndex], getString(TITLES[optionIndex])
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        rootView = binding.root
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        setupViews(binding)
        registerReceiver(downloadReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        checkNotificationsPermissions()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(downloadReceiver)
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