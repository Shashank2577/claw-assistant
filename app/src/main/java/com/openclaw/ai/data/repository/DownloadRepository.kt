package com.openclaw.ai.data.repository

import com.openclaw.ai.data.model.ModelDownloadStatus
import com.openclaw.ai.data.model.ModelInfo
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface DownloadRepository {

    fun downloadModel(model: ModelInfo)

    fun cancelDownload(modelId: String)

    fun getDownloadStatus(modelId: String): Flow<ModelDownloadStatus>
}
