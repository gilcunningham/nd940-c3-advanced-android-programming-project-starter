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
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.lifecycle.lifecycleScope
import com.udacity.databinding.ActivityMainBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var downloadID: Long = 0

    private lateinit var notificationManager: NotificationManager
    private lateinit var pendingIntent: PendingIntent
    private lateinit var action: NotificationCompat.Action

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

        binding.contentMain.showerButton.setOnClickListener {
            //val star = binding.contentMain.star
            doTest(binding.contentMain.customButton) //, star)
        }
        binding.contentMain.showerButton2.setOnClickListener {
            //val star = binding.contentMain.star
            doTest2(binding.contentMain.customButton) //, star)
        }
        binding.contentMain.showerButton3.setOnClickListener {
            //val star = binding.contentMain.star
            doTest3(binding.contentMain.customButton) //, star)
        }

    }

    fun doTest(animatedProgressIndicator : AnimatedProgressIndicator) { //}, star: ImageView)  {
        lifecycleScope.launch {
            animatedProgressIndicator.startProgress()
        }
    }

    fun doTest2(animatedProgressIndicator : AnimatedProgressIndicator) { //}, star: ImageView)  {
        lifecycleScope.launch {

            val percents = arrayOf(0, 15, 30, 34, 65, 70, 75, 89, 91, 100)
            for (percent in percents) {
                animatedProgressIndicator.updateProgress()
                delay(500)
            }

        }
    }

    fun doTest3(animatedProgressIndicator : AnimatedProgressIndicator) { //}, star: ImageView)  {
        lifecycleScope.launch {
            animatedProgressIndicator.finishProgress()
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
        }
    }

    private fun download() {
        val request =
            DownloadManager.Request(Uri.parse(URL))
                .setTitle(getString(R.string.app_name))
                .setDescription(getString(R.string.app_description))
                .setRequiresCharging(false)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

        val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        downloadID =
            downloadManager.enqueue(request)// enqueue puts the download request in the queue.
    }

    companion object {
        private const val URL =
            "https://github.com/udacity/nd940-c3-advanced-android-programming-project-starter/archive/master.zip"
        private const val CHANNEL_ID = "channelId"
    }
}