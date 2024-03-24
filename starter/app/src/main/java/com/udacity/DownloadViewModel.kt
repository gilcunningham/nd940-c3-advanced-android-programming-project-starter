package com.udacity

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.udacity.helper.DownloadHelper

class DownloadViewModel(isSuccess: Boolean, fileName: String) : ViewModel() {

    val downloadStatus: LiveData<Int> = MutableLiveData(
        if (isSuccess) {
            R.string.detail_download_status_success
        } else {
            R.string.detail_download_status_failure
        }
    )

    val textStyle : LiveData<Int> = MutableLiveData(
        if (isSuccess) {
            R.style.AppTextStyle
        } else {
            R.style.AppTextStyle_Failed
        }
    )
    val fileName: LiveData<String> = MutableLiveData(fileName)

    class Factory(
        private val context: Context,
        private val downloadId: Long
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val helper = DownloadHelper.with(context)
            val fileName = helper.fileName(downloadId)
            val downloadSuccess = helper.isSuccessful(downloadId)
            return DownloadViewModel(downloadSuccess, fileName) as T
        }
    }
}