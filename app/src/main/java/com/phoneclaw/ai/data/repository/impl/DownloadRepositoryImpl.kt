package com.phoneclaw.ai.data.repository.impl

import android.content.Context
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.phoneclaw.ai.data.*
import com.phoneclaw.ai.data.repository.DownloadRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadRepository: com.phoneclaw.ai.data.DownloadRepository
) : DownloadRepository {

    override fun downloadModel(model: Model) {
        val task = Task(id = "chat", label = "Chat", description = "LLM Chat", models = mutableListOf(model), category = Category.LLM)
        downloadRepository.downloadModel(task, model) { /* handled by download repo */ }
    }

    override fun cancelDownload(modelName: String) {
        // Implementation
    }

    override fun getDownloadStatus(modelId: String): Flow<ModelDownloadStatus> {
        return WorkManager.getInstance(context)
            .getWorkInfosByTagFlow(modelId)
            .map { workInfoList ->
                val workInfo = workInfoList.firstOrNull()
                if (workInfo == null) {
                    ModelDownloadStatus(status = ModelDownloadStatusType.NOT_DOWNLOADED)
                } else {
                    val progress = workInfo.progress
                    val statusType = when (workInfo.state) {
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
                        status = statusType,
                        receivedBytes = progress.getLong(KEY_MODEL_DOWNLOAD_RECEIVED_BYTES, 0L),
                        totalBytes = progress.getLong(KEY_MODEL_TOTAL_BYTES, 0L),
                        errorMessage = progress.getString(KEY_MODEL_DOWNLOAD_ERROR_MESSAGE) ?: "",
                        bytesPerSecond = progress.getLong(KEY_MODEL_DOWNLOAD_RATE, 0L),
                        remainingMs = progress.getLong(KEY_MODEL_DOWNLOAD_REMAINING_MS, 0L),
                    )
                }
            }
    }
}
