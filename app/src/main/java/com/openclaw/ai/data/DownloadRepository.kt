package com.openclaw.ai.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.openclaw.ai.AppLifecycleProvider
import com.openclaw.ai.MainActivity
import com.openclaw.ai.R
import com.openclaw.ai.worker.DownloadWorker
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

private const val TAG = "DownloadRepository"
private const val MODEL_NAME_TAG = "modelName"

/** Repository for managing model downloads. */
interface DownloadRepository {
  /**
   * Downloads the model.
   *
   * @param task the task that the model belongs to.
   * @param model the model to be downloaded.
   * @param onStatusUpdated callback when the status of the download is updated.
   * @return the id of the work.
   */
  fun downloadModel(task: Task, model: Model, onStatusUpdated: (ModelDownloadStatus) -> Unit): UUID

  /**
   * Cancels the model download.
   *
   * @param model the model download to be cancelled.
   */
  fun cancelDownload(model: Model)

  /** Cancels all downloads. */
  fun cancelAll(onComplete: () -> Unit)

  /**
   * Observes the download of the model.
   *
   * @param workerId the id of the work.
   * @param model the model to be observed.
   * @param onStatusUpdated callback when the status of the download is updated.
   */
  fun observeDownload(workerId: UUID, model: Model, onStatusUpdated: (ModelDownloadStatus) -> Unit)
}

/** Default implementation of [DownloadRepository]. */
class DefaultDownloadRepository(
  private val context: Context,
  private val lifecycleProvider: AppLifecycleProvider,
) : DownloadRepository {
  private val workManager = WorkManager.getInstance(context)

  // Mapping from model name to the current status of the download.
  private val modelStatuses = MutableStateFlow<Map<String, ModelDownloadStatus>>(mapOf())

  override fun downloadModel(
    task: Task,
    model: Model,
    onStatusUpdated: (ModelDownloadStatus) -> Unit
  ): UUID {
    Log.i(TAG, "Downloading model ${model.name}")

    val extraDataUrls = model.extraDataFiles.map { it.url }.joinToString(separator = ",")
    val extraDataDownloadFileNames =
      model.extraDataFiles.map { it.downloadFileName }.joinToString(separator = ",")

    val inputData =
      Data.Builder()
        .putString(KEY_MODEL_URL, model.url)
        .putString(KEY_MODEL_NAME, model.name)
        .putString(KEY_MODEL_COMMIT_HASH, model.version)
        .putString(KEY_MODEL_DOWNLOAD_MODEL_DIR, model.normalizedName)
        .putString(KEY_MODEL_DOWNLOAD_FILE_NAME, model.downloadFileName)
        .putLong(KEY_MODEL_TOTAL_BYTES, model.sizeInBytes)
        .putString(KEY_MODEL_EXTRA_DATA_URLS, extraDataUrls)
        .putString(KEY_MODEL_EXTRA_DATA_DOWNLOAD_FILE_NAMES, extraDataDownloadFileNames)
        .putBoolean(KEY_MODEL_IS_ZIP, model.isZip)
        .putString(KEY_MODEL_UNZIPPED_DIR, model.unzipDir)
        .build()

    val downloadWorkRequest =
      OneTimeWorkRequestBuilder<DownloadWorker>()
        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        .setInputData(inputData)
        .addTag("$MODEL_NAME_TAG:${model.name}")
        .build()

    workManager.enqueueUniqueWork(model.name, ExistingWorkPolicy.KEEP, downloadWorkRequest)

    observeDownload(downloadWorkRequest.id, model, onStatusUpdated)

    return downloadWorkRequest.id
  }

  override fun cancelDownload(model: Model) {
    Log.i(TAG, "Cancelling download for model ${model.name}")
    workManager.cancelUniqueWork(model.name)
  }

  override fun cancelAll(onComplete: () -> Unit) {
    Log.i(TAG, "Cancelling all downloads")
    workManager.cancelAllWorkByTag(MODEL_NAME_TAG)
    onComplete()
  }

  override fun observeDownload(
    workerId: UUID,
    model: Model,
    onStatusUpdated: (ModelDownloadStatus) -> Unit
  ) {
    workManager.getWorkInfoByIdLiveData(workerId).observeForever { workInfo ->
      if (workInfo != null) {
        val status =
          when (workInfo.state) {
            WorkInfo.State.ENQUEUED -> ModelDownloadStatus(status = ModelDownloadStatusType.IN_PROGRESS)
            WorkInfo.State.RUNNING -> {
              val progress = workInfo.progress
              val receivedBytes = progress.getLong(KEY_MODEL_DOWNLOAD_RECEIVED_BYTES, 0L)
              val bytesPerSecond = progress.getLong(KEY_MODEL_DOWNLOAD_RATE, 0L)
              val remainingMs = progress.getLong(KEY_MODEL_DOWNLOAD_REMAINING_MS, 0L)
              val isUnzipping = progress.getBoolean(KEY_MODEL_START_UNZIPPING, false)
              ModelDownloadStatus(
                status =
                  if (isUnzipping) ModelDownloadStatusType.UNZIPPING
                  else ModelDownloadStatusType.IN_PROGRESS,
                totalBytes = model.sizeInBytes,
                receivedBytes = receivedBytes,
                bytesPerSecond = bytesPerSecond,
                remainingMs = remainingMs,
              )
            }
            WorkInfo.State.SUCCEEDED -> {
              if (!lifecycleProvider.isAppInForeground) {
                showDownloadCompleteNotification(model)
              }
              ModelDownloadStatus(status = ModelDownloadStatusType.SUCCEEDED)
            }
            WorkInfo.State.FAILED -> {
              val errorMessage =
                workInfo.outputData.getString(KEY_MODEL_DOWNLOAD_ERROR_MESSAGE) ?: "Unknown error"
              ModelDownloadStatus(status = ModelDownloadStatusType.FAILED, errorMessage = errorMessage)
            }
            WorkInfo.State.CANCELLED ->
              ModelDownloadStatus(status = ModelDownloadStatusType.NOT_DOWNLOADED)
            else -> ModelDownloadStatus(status = ModelDownloadStatusType.NOT_DOWNLOADED)
          }
        modelStatuses.update { it + (model.name to status) }
        onStatusUpdated(status)
      }
    }
  }

  private fun showDownloadCompleteNotification(model: Model) {
    val channelId = "model_download_channel"
    val notificationId = model.name.hashCode()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val name = "Model Downloads"
      val descriptionText = "Notifications for model download status"
      val importance = NotificationManager.IMPORTANCE_DEFAULT
      val channel =
        NotificationChannel(channelId, name, importance).apply { description = descriptionText }
      val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      notificationManager.createNotificationChannel(channel)
    }

    val intent =
      Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
      }
    val pendingIntent: PendingIntent =
      PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

    val builder =
      NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.stat_sys_download_done)
        .setContentTitle("Download Complete")
        .setContentText("Model ${model.displayName} has been downloaded.")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)

    with(NotificationManagerCompat.from(context)) {
      if (
        ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
          PackageManager.PERMISSION_GRANTED
      ) {
        return
      }
      notify(notificationId, builder.build())
    }
  }
}
