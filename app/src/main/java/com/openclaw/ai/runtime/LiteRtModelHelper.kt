package com.openclaw.ai.runtime

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.ai.edge.litertlm.*
import com.openclaw.ai.data.model.*
import kotlinx.coroutines.CoroutineScope
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "LiteRtModelHelper"

private data class LlmModelInstance(val engine: Engine, var conversation: Conversation)

@Singleton
class LiteRtModelHelper @Inject constructor() : LlmModelHelper {

    // Keyed by model id.
    private val instances: MutableMap<String, LlmModelInstance> = mutableMapOf()
    private val cleanUpListeners: MutableMap<String, CleanUpListener> = mutableMapOf()

    @OptIn(ExperimentalApi::class)
    override fun initialize(
        context: Context,
        model: ModelInfo,
        supportImage: Boolean,
        supportAudio: Boolean,
        onDone: (String) -> Unit,
        systemInstruction: String?,
        tools: List<ToolDefinition>,
        coroutineScope: CoroutineScope?,
    ) {
        val modelPath = model.getPath(context)
        Log.d(TAG, "Initializing model '${model.id}' from path: $modelPath")

        // Map accelerators to LiteRT backends
        val preferredAccelerator = model.accelerators.firstOrNull() ?: Accelerator.CPU
        val preferredBackend = when (preferredAccelerator) {
            Accelerator.CPU -> Backend.CPU()
            Accelerator.GPU -> Backend.GPU()
            Accelerator.NPU -> Backend.NPU(nativeLibraryDir = context.applicationInfo.nativeLibraryDir)
        }

        val visionBackend = when (model.visionAccelerator) {
            Accelerator.CPU -> Backend.CPU()
            Accelerator.GPU -> Backend.GPU()
            Accelerator.NPU -> Backend.NPU(nativeLibraryDir = context.applicationInfo.nativeLibraryDir)
        }

        val engineConfig = EngineConfig(
            modelPath = modelPath,
            backend = preferredBackend,
            visionBackend = if (supportImage) visionBackend else null,
            audioBackend = if (supportAudio) Backend.CPU() else null,
            maxNumTokens = model.defaultMaxTokens,
            cacheDir = if (modelPath.startsWith("/data/local/tmp")) context.getExternalFilesDir(null)?.absolutePath else null
        )

        try {
            val engine = Engine(engineConfig)
            engine.initialize()

            val systemContents = systemInstruction?.let {
                Contents.of(listOf(Content.Text(it)))
            }

            val conversation = engine.createConversation(
                ConversationConfig(
                    samplerConfig = if (preferredBackend is Backend.NPU) null else SamplerConfig(
                        topK = model.defaultTopK,
                        topP = model.defaultTopP.toDouble(),
                        temperature = model.defaultTemperature.toDouble(),
                    ),
                    systemInstruction = systemContents,
                )
            )

            instances[model.id] = LlmModelInstance(engine = engine, conversation = conversation)
            Log.d(TAG, "Initialized model '${model.id}' successfully with backend: $preferredBackend")
            onDone("")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize model '${model.id}'", e)
            onDone(e.message ?: "Unknown error initializing model")
        }
    }

    @OptIn(ExperimentalApi::class)
    override fun resetConversation(
        model: ModelInfo,
        supportImage: Boolean,
        supportAudio: Boolean,
        systemInstruction: String?,
        tools: List<ToolDefinition>,
    ) {
        try {
            val instance = instances[model.id] ?: return
            instance.conversation.close()

            val systemContents = systemInstruction?.let {
                Contents.of(listOf(Content.Text(it)))
            }

            val preferredAccelerator = model.accelerators.firstOrNull() ?: Accelerator.CPU
            
            val newConversation = instance.engine.createConversation(
                ConversationConfig(
                    samplerConfig = if (preferredAccelerator == Accelerator.NPU) null else SamplerConfig(
                        topK = model.defaultTopK,
                        topP = model.defaultTopP.toDouble(),
                        temperature = model.defaultTemperature.toDouble(),
                    ),
                    systemInstruction = systemContents,
                )
            )
            instance.conversation = newConversation
            Log.d(TAG, "Reset conversation for model '${model.id}'")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reset conversation for model '${model.id}'", e)
        }
    }

    override fun cleanUp(model: ModelInfo, onDone: () -> Unit) {
        val instance = instances[model.id] ?: run {
            onDone()
            return
        }

        try {
            instance.conversation.close()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to close conversation: ${e.message}")
        }

        try {
            instance.engine.close()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to close engine: ${e.message}")
        }

        cleanUpListeners.remove(model.id)?.invoke()
        instances.remove(model.id)
        Log.d(TAG, "Cleaned up model '${model.id}'")
        onDone()
    }

    override fun runInference(
        model: ModelInfo,
        input: String,
        resultListener: ResultListener,
        cleanUpListener: CleanUpListener,
        onError: (message: String) -> Unit,
        images: List<Bitmap>,
        audioClips: List<ByteArray>,
        coroutineScope: CoroutineScope?,
        extraContext: Map<String, String>?,
    ) {
        val instance = instances[model.id]
        if (instance == null) {
            onError("LlmModelInstance is not initialized for model '${model.id}'.")
            return
        }

        cleanUpListeners.putIfAbsent(model.id, cleanUpListener)

        val contents = mutableListOf<Content>()
        for (image in images) {
            contents.add(Content.ImageBytes(image.toPngByteArray()))
        }
        for (audioClip in audioClips) {
            contents.add(Content.AudioBytes(audioClip))
        }
        if (input.trim().isNotEmpty()) {
            contents.add(Content.Text(input))
        }

        instance.conversation.sendMessageAsync(
            Contents.of(contents),
            object : MessageCallback {
                override fun onMessage(message: Message) {
                    resultListener(message.toString(), false, message.channels["thought"])
                }

                override fun onDone() {
                    resultListener("", true, null)
                }

                override fun onError(throwable: Throwable) {
                    if (throwable is CancellationException) {
                        Log.i(TAG, "Inference cancelled for model '${model.id}'.")
                        resultListener("", true, null)
                    } else {
                        Log.e(TAG, "Inference error for model '${model.id}'", throwable)
                        onError("Error: ${throwable.message}")
                    }
                }
            },
            extraContext ?: emptyMap(),
        )
    }

    override fun stopResponse(model: ModelInfo) {
        instances[model.id]?.conversation?.cancelProcess()
    }

    private fun Bitmap.toPngByteArray(): ByteArray {
        val stream = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
}
