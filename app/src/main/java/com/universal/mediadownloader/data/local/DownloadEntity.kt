package com.universal.mediadownloader.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DownloadStatus {
    PENDING, DOWNLOADING, PAUSED, COMPLETED, FAILED
}

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val fileName: String,
    val filePath: String,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val progress: Int = 0,
    val totalSize: Long = 0,
    val downloadedSize: Long = 0,
    val mimeType: String,
    val timestamp: Long = System.currentTimeMillis()
)
