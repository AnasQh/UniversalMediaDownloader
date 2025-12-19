package com.universal.mediadownloader.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.universal.mediadownloader.core.SniffedMedia
import com.universal.mediadownloader.data.local.DownloadDatabase
import com.universal.mediadownloader.data.repository.DownloadRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DownloadRepository
    
    init {
        val dao = DownloadDatabase.getDatabase(application).downloadDao()
        repository = DownloadRepository(dao, application)
    }

    val allDownloads = repository.allDownloads

    private val _detectedMedia = MutableStateFlow<SniffedMedia?>(null)
    val detectedMedia: StateFlow<SniffedMedia?> = _detectedMedia.asStateFlow()

    fun onMediaDetected(media: SniffedMedia) {
        _detectedMedia.value = media
    }

    fun dismissSheet() {
        _detectedMedia.value = null
    }

    fun startDownload(media: SniffedMedia) {
        viewModelScope.launch {
            repository.startDownload(media.url, media.name + ".mp4", media.mimeType) // Simple naming for now
            dismissSheet()
        }
    }
}
