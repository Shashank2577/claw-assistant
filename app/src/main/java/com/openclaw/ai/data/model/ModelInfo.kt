package com.openclaw.ai.data.model

import android.content.Context
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class ModelDataFile(
    val name: String,
    val url: String,
    val downloadFileName: String,
    val sizeInBytes: Long,
)

@Serializable
data class PromptTemplate(
    val title: String, 
    val description: String, 
    val prompt: String
)

enum class ModelType {
    LOCAL,
    CLOUD,
}

enum class ModelDownloadStatusType {
    NOT_DOWNLOADED,
    PARTIALLY_DOWNLOADED,
    IN_PROGRESS,
    UNZIPPING,
    SUCCEEDED,
    FAILED,
}

@Serializable
data class ModelDownloadStatus(
    val status: ModelDownloadStatusType,
    val totalBytes: Long = 0,
    val receivedBytes: Long = 0,
    val errorMessage: String = "",
    val bytesPerSecond: Long = 0,
    val remainingMs: Long = 0,
)

@Serializable
data class ModelInfo(
    val id: String, // Maps to Gallery's 'name'
    val name: String,
    val modelId: String = "", // HF model ID
    val displayName: String = name,
    val info: String = "",
    val type: ModelType,
    val sizeLabel: String = "",
    val url: String = "", // Explicit download URL
    val sizeInBytes: Long = 0L,
    val downloadFileName: String = "_",
    val version: String = "1",
    val extraDataFiles: List<ModelDataFile> = listOf(),
    val isLlm: Boolean = true,
    val runtimeType: RuntimeType = RuntimeType.LITERT_LM,
    
    // UI/Feature flags
    val supportsImage: Boolean = false,
    val supportsAudio: Boolean = false,
    val supportsThinking: Boolean = false,
    val supportsTools: Boolean = false,
    
    // Model Params
    val maxContextLength: Int = 8192,
    val defaultMaxTokens: Int = 4096,
    val defaultTemperature: Float = 1.0f,
    val defaultTopK: Int = 64,
    val defaultTopP: Float = 0.95f,
    
    // Deployment specific
    val isZip: Boolean = false,
    val unzipDir: String = "",
    val localFileRelativeDirPathOverride: String = "",
    val localModelFilePathOverride: String = "",
    val accelerators: List<Accelerator> = listOf(Accelerator.GPU, Accelerator.CPU),
    val visionAccelerator: Accelerator = Accelerator.GPU,
) {
    val isLocal: Boolean get() = type == ModelType.LOCAL
    val isCloud: Boolean get() = type == ModelType.CLOUD

    fun getPath(context: Context, fileName: String = downloadFileName): String {
        if (localModelFilePathOverride.isNotEmpty()) {
            return localModelFilePathOverride
        }

        if (localFileRelativeDirPathOverride.isNotEmpty()) {
            return listOf(
                context.getExternalFilesDir(null)?.absolutePath ?: "",
                localFileRelativeDirPathOverride,
                fileName,
            ).joinToString(File.separator)
        }

        val normalizedName = id.replace(Regex("[^a-zA-Z0-9]"), "_")
        val baseDir = listOf(
            context.getExternalFilesDir(null)?.absolutePath ?: "", 
            normalizedName, 
            version
        ).joinToString(File.separator)
        
        return if (this.isZip && this.unzipDir.isNotEmpty()) {
            listOf(baseDir, this.unzipDir).joinToString(File.separator)
        } else {
            listOf(baseDir, fileName).joinToString(File.separator)
        }
    }
}

object DefaultModels {
    val GEMMA_3N_2B = ModelInfo(
        id = "Gemma-3n-E2B-it-int4",
        name = "Gemma 3n E2B",
        modelId = "google/gemma-3n-E2B-it-litert-preview",
        displayName = "Gemma 3n 2B",
        info = "Preview version of Gemma 3n E2B ready for deployment on Android. Supports text and vision input.",
        type = ModelType.LOCAL,
        sizeLabel = "2B",
        sizeInBytes = 3136226711L,
        url = "https://huggingface.co/google/gemma-3n-E2B-it-litert-preview/resolve/main/gemma-3n-E2B-it-int4.task?download=true",
        downloadFileName = "gemma-3n-E2B-it-int4.task",
        version = "20250520",
        supportsImage = true,
        maxContextLength = 4096,
    )

    val GEMMA_3N_4B = ModelInfo(
        id = "Gemma-3n-E4B-it-int4",
        name = "Gemma 3n E4B",
        modelId = "google/gemma-3n-E4B-it-litert-preview",
        displayName = "Gemma 3n 4B",
        info = "Preview version of Gemma 3n E4B. Larger and more capable on-device reasoning.",
        type = ModelType.LOCAL,
        sizeLabel = "4B",
        sizeInBytes = 4405655031L,
        url = "https://huggingface.co/google/gemma-3n-E4B-it-litert-preview/resolve/main/gemma-3n-E4B-it-int4.task?download=true",
        downloadFileName = "gemma-3n-E4B-it-int4.task",
        version = "20250520",
        supportsImage = true,
        maxContextLength = 4096,
    )

    val GEMMA_3_1B = ModelInfo(
        id = "Gemma3-1B-IT-q4",
        name = "Gemma3 1B IT q4",
        modelId = "litert-community/Gemma3-1B-IT",
        displayName = "Gemma 3 1B",
        info = "Compact 1B model variant with 4-bit quantization.",
        type = ModelType.LOCAL,
        sizeLabel = "1B",
        sizeInBytes = 554661246L,
        url = "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/Gemma3-1B-IT_multi-prefill-seq_q4_ekv2048.task?download=true",
        downloadFileName = "Gemma3-1B-IT_multi-prefill-seq_q4_ekv2048.task",
        version = "20250514",
        maxContextLength = 2048,
    )

    val GEMINI_FLASH = ModelInfo(
        id = "gemini-2.0-flash",
        name = "Gemini 2.0 Flash",
        displayName = "Gemini Flash",
        info = "Cloud-based model optimized for speed and multimodal efficiency.",
        type = ModelType.CLOUD,
        supportsImage = true,
        supportsAudio = true,
        supportsTools = true,
        maxContextLength = 1_000_000,
    )

    val GEMINI_PRO = ModelInfo(
        id = "gemini-2.0-pro",
        name = "Gemini 2.0 Pro",
        displayName = "Gemini Pro",
        info = "Our most capable cloud model for complex reasoning and long context.",
        type = ModelType.CLOUD,
        supportsImage = true,
        supportsAudio = true,
        supportsTools = true,
        maxContextLength = 2_000_000,
    )

    val ALL = listOf(GEMMA_3N_2B, GEMMA_3N_4B, GEMMA_3_1B, GEMINI_FLASH, GEMINI_PRO)
    val LOCAL_MODELS = ALL.filter { it.isLocal }
    val CLOUD_MODELS = ALL.filter { it.isCloud }
}
