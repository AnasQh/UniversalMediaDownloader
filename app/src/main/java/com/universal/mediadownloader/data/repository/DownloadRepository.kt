package com.universal.mediadownloader.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.universal.mediadownloader.data.local.DownloadDao
import com.universal.mediadownloader.data.local.DownloadEntity
import com.universal.mediadownloader.data.worker.DownloadWorker
import kotlinx.coroutines.flow.Flow

class DownloadRepository(
    private val downloadDao: DownloadDao,
    private val context: Context
) {
    val allDownloads: Flow<List<DownloadEntity>> = downloadDao.getAllDownloads()

    suspend fun startDownload(url: String, fileName: String, mimeType: String) {
        val download = DownloadEntity(
            url = url,
            fileName = fileName,
            filePath = "", // Set in worker or before
            mimeType = mimeType
        )
        val id = downloadDao.insertDownload(download)

        val inputData = Data.Builder()
            .putLong("download_id", id)
            .putString("url", url)
            .putString("file_name", fileName)
            .build()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .addTag("download_$id")
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }

    suspend fun pauseDownload(id: Long) {
        WorkManager.getInstance(context).cancelAllWorkByTag("download_$id")
        // Update status to PAUSED in DB
        val download = downloadDao.getDownloadById(id)
        if (download != null) {
            // downloadDao.updateDownload(download.copy(status = DownloadStatus.PAUSED))
            // The worker cancellation should handle status update or we do it here
        }
    }
}
