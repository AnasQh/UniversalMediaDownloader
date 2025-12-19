package com.universal.mediadownloader.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.universal.mediadownloader.R
import com.universal.mediadownloader.data.local.DownloadDatabase
import com.universal.mediadownloader.data.local.DownloadStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class DownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val downloadDao = DownloadDatabase.getDatabase(context).downloadDao()
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result {
        val downloadId = inputData.getLong("download_id", -1)
        val url = inputData.getString("url") ?: return Result.failure()
        val fileName = inputData.getString("file_name") ?: "video.mp4"

        if (downloadId == -1L) return Result.failure()

        val download = downloadDao.getDownloadById(downloadId) ?: return Result.failure()

        // Create Notification Channel
        createNotificationChannel()

        // Start Foreground
        setForeground(createForegroundInfo(fileName, 0))

        // Update DB to DOWNLOADING
        downloadDao.updateDownload(download.copy(status = DownloadStatus.DOWNLOADING))

        return withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) throw Exception("Failed to connect")

                val body = response.body ?: throw Exception("No body")
                val contentLength = body.contentLength()
                
                // Update total size
                downloadDao.updateDownload(download.copy(totalSize = contentLength, status = DownloadStatus.DOWNLOADING))

                val file = File(applicationContext.getExternalFilesDir(null), fileName)
                val outputStream = FileOutputStream(file)
                val inputStream: InputStream = body.byteStream()

                val buffer = ByteArray(8 * 1024)
                var bytesRead: Int
                var totalBytesRead: Long = 0

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    if (isStopped) {
                        outputStream.close()
                        inputStream.close()
                        return@withContext Result.failure() // Or retry
                    }
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    
                    // Update progress periodically (e.g., every 1%)
                    // For simplicity, updating notification here
                    // Ideally, throttle DB updates
                    if (contentLength > 0) {
                        val progress = ((totalBytesRead * 100) / contentLength).toInt()
                         setForeground(createForegroundInfo(fileName, progress))
                    }
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                downloadDao.updateDownload(
                    download.copy(
                        status = DownloadStatus.COMPLETED,
                        filePath = file.absolutePath,
                        progress = 100,
                        downloadedSize = totalBytesRead
                    )
                )

                Result.success()
            } catch (e: Exception) {
                e.printStackTrace()
                downloadDao.updateDownload(download.copy(status = DownloadStatus.FAILED))
                Result.failure()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "downloads",
                "Downloads",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createForegroundInfo(fileName: String, progress: Int): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, "downloads")
            .setContentTitle("Downloading $fileName")
            .setContentText("$progress%")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .build()
        return ForegroundInfo(1, notification)
    }
}
