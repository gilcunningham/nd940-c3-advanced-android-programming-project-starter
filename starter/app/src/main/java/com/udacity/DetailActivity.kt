package com.udacity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.udacity.databinding.ActivityDetailBinding

class DetailActivity : AppCompatActivity() {

    private val viewModel : DownloadViewModel by lazy {
        val downloadId = intent.getLongExtra(DOWNLOAD_ID_EXTRA, 0)
        ViewModelProvider(
            this, DownloadViewModel.Factory(this, downloadId)
        )[DownloadViewModel::class.java]
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        setupView(binding)
    }

    private fun setupView(binding: ActivityDetailBinding) {
        binding.apply {
            contentDetail.viewModel = viewModel
            contentDetail.okButton.setOnClickListener {
                this@DetailActivity.onBackPressed()
            }
        }
    }

    companion object {
        fun makeIntent(context: Context, downloadId: Long): Intent {
            val intent = Intent(context, DetailActivity::class.java)
            intent.putExtra(DOWNLOAD_ID_EXTRA, downloadId)
            return intent
        }

        private const val DOWNLOAD_ID_EXTRA = "DOWNLOAD_ID_EXTRA"
    }
}
