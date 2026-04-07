package com.openclaw.ai.data.repository.impl

import android.content.Context
import android.util.Log
import androidx.work.*
import com.openclaw.ai.data.model.*
import com.openclaw.ai.data.repository.DownloadRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.openclaw.ai.worker.DownloadWorker
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "DownloadRepository"
private const val MODEL_NAME_TAG = "modelName"

@Singleton
class DownloadRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : DownloadRepository {

    private val workManager = WorkManager.getInstance(context)

    override fun downloadModel(model: ModelInfo) {
        val normalizedName = model.id.replace(Regex("[^a-zA-Z0-9]"), "_")
        
        val inputData = Data.Builder()
            .putString(KEY_MODEL_NAME, model.id)
            .putString(KEY_MODEL_URL, model.url)
            .putString(KEY_MODEL_COMMIT_HASH, model.version)
            .putString(KEY_MODEL_DOWNLOAD_MODEL_DIR, normalizedName)
            .putString(KEY_MODEL_DOWNLOAD_FILE_NAME, model.downloadFileName)
            .putBoolean(KEY_MODEL_IS_ZIP, model.isZip)
            .putString(KEY_MODEL_UNZIPPED_DIR, model.unzipDir)
            .putLong(KEY_MODEL_TOTAL_BYTES, model.sizeInBytes)
            
        if (model.extraDataFiles.isNotEmpty()) {
            inputData.putString(KEY_MODEL_EXTRA_DATA_URLS, model.extraDataFiles.joinToString(",") { it.url })
            inputData.putString(
                KEY_MODEL_EXTRA_DATA_DOWNLOAD_FILE_NAMES,
                model.extraDataFiles.joinToString(",") { it.downloadFileName }
            )
        }
        
        val downloadWorkRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(inputData.build())
            .addTag("$MODEL_NAME_TAG:${model.id}")
            .build()

        workManager.enqueueUniqueWork(
            model.id,
            ExistingWorkPolicy.REPLACE,
            downloadWorkRequest
        )
        
        Log.d(TAG, "Enqueued download for model: ${model.id}")
    }

    override fun cancelDownload(modelId: String) {
        workManager.cancelUniqueWork(modelId)
    }

    override fun getDownloadStatus(modelId: String): Flow<ModelDownloadStatus> {
        return workManager.getWorkInfosForUniqueWorkFlow(modelId).map { workInfos ->
            val workInfo = workInfos.firstOrNull() ?: return@map ModelDownloadStatus(ModelDownloadStatusType.NOT_DOWNLOADED)
            
            when (workInfo.state) {
                WorkInfo.State.ENQUEUED -> ModelDownloadStatus(ModelDownloadStatusType.PARTIALLY_DOWNLOADED)
                WorkInfo.State.RUNNING -> {
                    val received = workInfo.progress.getLong(KEY_MODEL_DOWNLOAD_RECEIVED_BYTES, 0L)
                    val rate = workInfo.progress.getLong(KEY_MODEL_DOWNLOAD_RATE, 0L)
                    val remaining = workInfo.progress.getLong(KEY_MODEL_DOWNLOAD_REMAINING_MS, 0L)
                    val unzipping = workInfo.progress.getBoolean(KEY_MODEL_START_UNZIPPING, false)
                    
                    ModelDownloadStatus(
                        status = if (unzipping) ModelDownloadStatusType.UNZIPPING else ModelDownloadStatusType.IN_PROGRESS,
                        totalBytes = workInfo.progress.getLong(KEY_MODEL_TOTAL_BYTES, 0L),
                        receivedBytes = received,
                        bytesPerSecond = rate,
                        remainingMs = remaining
                    )
                }
                WorkInfo.State.SUCCEEDED -> ModelDownloadStatus(ModelDownloadStatusType.SUCCEEDED)
                WorkInfo.State.FAILED -> {
                    val error = workInfo.outputData.getString(KEY_MODEL_DOWNLOAD_ERROR_MESSAGE) ?: "Unknown error"
                    ModelDownloadStatus(ModelDownloadStatusType.FAILED, errorMessage = error)
                }
                WorkInfo.State.CANCELLED -> ModelDownloadStatus(ModelDownloadStatusType.NOT_DOWNLOADED)
                else -> ModelDownloadStatus(ModelDownloadStatusType.NOT_DOWNLOADED)
            }
        }
    }
}
