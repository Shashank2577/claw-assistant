/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.openclaw.ai.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.openclaw.ai.AppLifecycleProvider
import com.openclaw.ai.MainActivity
import com.openclaw.ai.R
import com.openclaw.ai.worker.DownloadWorker
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val TAG = "AGDownloadRepo"

interface DownloadRepository {
  fun downloadModel(
    task: Task,
    model: Model,
    onStatusUpdated: (status: ModelDownloadStatus) -> Unit,
  )

  fun cancelDownload(model: Model)
}

class DefaultDownloadRepository(
  private val context: Context,
  private val lifecycleProvider: AppLifecycleProvider,
) : DownloadRepository {

  private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

  override fun downloadModel(
    task: Task,
    model: Model,
    onStatusUpdated: (status: ModelDownloadStatus) -> Unit,
  ) {
    Log.d(TAG, "Download model: ${model.name}")

    val workManager = WorkManager.getInstance(context)

    val constraints =
      Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    val inputData =
      Data.Builder()
        .putString(KEY_MODEL_NAME, model.name)
        .putString(KEY_MODEL_URL, model.url)
        .putLong(KEY_MODEL_TOTAL_BYTES, model.sizeInBytes)
        .build()

    val downloadWorkRequest: OneTimeWorkRequest =
      OneTimeWorkRequestBuilder<DownloadWorker>()
        .setConstraints(constraints)
        .setInputData(inputData)
        .setBackoffCriteria(
          BackoffPolicy.LINEAR,
          WorkRequest.MIN_BACKOFF_MILLIS,
          TimeUnit.MILLISECONDS,
        )
        .addTag(model.name)
        .build()

    workManager.enqueueUniqueWork(
      model.name,
      ExistingWorkPolicy.KEEP,
      downloadWorkRequest,
    )

    repositoryScope.launch {
      workManager
        .getWorkInfoByIdFlow(downloadWorkRequest.id)
        .map { workInfo ->
          if (workInfo == null) {
            ModelDownloadStatus(status = ModelDownloadStatusType.NOT_DOWNLOADED)
          } else {
            val progress = workInfo.progress
            val status =
              when (workInfo.state) {
                WorkInfo.State.SUCCEEDED -> ModelDownloadStatusType.SUCCEEDED
                WorkInfo.State.FAILED -> ModelDownloadStatusType.FAILED
                WorkInfo.State.CANCELLED -> ModelDownloadStatusType.NOT_DOWNLOADED
                WorkInfo.State.RUNNING -> {
                  if (progress.getBoolean(KEY_MODEL_START_UNZIPPING, false)) {
                    ModelDownloadStatusType.UNZIPPING
                  } else {
                    ModelDownloadStatusType.IN_PROGRESS
                  }
                }
                else -> ModelDownloadStatusType.NOT_DOWNLOADED
              }
            ModelDownloadStatus(
              status = status,
              receivedBytes = progress.getLong(KEY_MODEL_DOWNLOAD_RECEIVED_BYTES, 0L),
              totalBytes = progress.getLong(KEY_MODEL_TOTAL_BYTES, model.sizeInBytes),
              errorMessage = progress.getString(KEY_MODEL_DOWNLOAD_ERROR_MESSAGE) ?: "",
              bytesPerSecond = progress.getLong(KEY_MODEL_DOWNLOAD_RATE, 0L),
            )
          }
        }
        .distinctUntilChanged()
        .collect { status ->
          onStatusUpdated(status)
          if (status.status == ModelDownloadStatusType.SUCCEEDED) {
            showDownloadCompleteNotification(model)
          }
        }
    }
  }

  override fun cancelDownload(model: Model) {
    Log.d(TAG, "Cancel download for: ${model.name}")
    WorkManager.getInstance(context).cancelUniqueWork(model.name)
  }

  private fun showDownloadCompleteNotification(model: Model) {
    val channelId = "model_download_channel"
    val notificationId = model.name.hashCode()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val name = "Model Download"
      val descriptionText = "Notifications for model downloads"
      val importance = NotificationManager.IMPORTANCE_DEFAULT
      val channel = NotificationChannel(channelId, name, importance).apply {
        description = descriptionText
      }
      val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      notificationManager.createNotificationChannel(channel)
    }

    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val pendingIntent: PendingIntent = PendingIntent.getActivity(
      context, 0, intent, PendingIntent.FLAG_IMMUTABLE
    )

    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle("Model Download Complete")
        .setContentText("${model.displayName} has been downloaded and is ready to use.")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)

    with(NotificationManagerCompat.from(context)) {
      if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
        notify(notificationId, builder.build())
      }
    }
  }
}
