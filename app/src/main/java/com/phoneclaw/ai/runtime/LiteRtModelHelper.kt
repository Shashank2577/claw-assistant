package com.phoneclaw.ai.runtime

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.ai.edge.litertlm.*
import com.phoneclaw.ai.data.*
import kotlinx.coroutines.CoroutineScope
import java.io.ByteArrayOutputStream
import java.util.concurrent.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "LiteRtModelHelper"

private data class LlmModelInstance(val engine: Engine, var conversation: Conversation)

@Singleton
class LiteRtModelHelper @Inject constructor() : LlmModelHelper {

    private val instances: MutableMap<String, LlmModelInstance> = mutableMapOf()
    private val cleanUpListeners: MutableMap<String, CleanUpListener> = mutableMapOf()

    @OptIn(ExperimentalApi::class)
    override fun initialize(
        context: Context,
        model: Model,
        supportImage: Boolean,
        supportAudio: Boolean,
        onDone: (String) -> Unit,
        systemInstruction: Contents?,
        tools: List<ToolProvider>,
        enableConversationConstrainedDecoding: Boolean,
        coroutineScope: CoroutineScope?,
    ) {
        val modelPath = model.getPath(context)
        Log.d(TAG, "Initializing model '${model.name}' from path: $modelPath")

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
            maxNumTokens = model.llmMaxToken,
        )

        try {
            val engine = Engine(engineConfig)
            engine.initialize()

            val conversation = engine.createConversation(
                ConversationConfig(
                    samplerConfig = if (preferredBackend is Backend.NPU) null else SamplerConfig(
                        topK = DEFAULT_TOPK,
                        topP = DEFAULT_TOPP.toDouble(),
                        temperature = DEFAULT_TEMPERATURE.toDouble(),
                    ),
                    systemInstruction = systemInstruction,
                    tools = tools,
                )
            )

            instances[model.name] = LlmModelInstance(engine = engine, conversation = conversation)
            Log.d(TAG, "Initialized model '${model.name}' successfully.")
            onDone("")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize model '${model.name}'", e)
            onDone(e.message ?: "Unknown error")
        }
    }

    @OptIn(ExperimentalApi::class)
    override fun resetConversation(
        model: Model,
        supportImage: Boolean,
        supportAudio: Boolean,
        systemInstruction: Contents?,
        tools: List<ToolProvider>,
        enableConversationConstrainedDecoding: Boolean,
    ) {
        try {
            val instance = instances[model.name] ?: return
            instance.conversation.close()

            val preferredAccelerator = model.accelerators.firstOrNull() ?: Accelerator.CPU
            
            val newConversation = instance.engine.createConversation(
                ConversationConfig(
                    samplerConfig = if (preferredAccelerator == Accelerator.NPU) null else SamplerConfig(
                        topK = DEFAULT_TOPK,
                        topP = DEFAULT_TOPP.toDouble(),
                        temperature = DEFAULT_TEMPERATURE.toDouble(),
                    ),
                    systemInstruction = systemInstruction,
                    tools = tools,
                )
            )
            instance.conversation = newConversation
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reset conversation for model '${model.name}'", e)
        }
    }

    override fun cleanUp(model: Model, onDone: () -> Unit) {
        val instance = instances[model.name] ?: run {
            onDone()
            return
        }

        try {
            instance.conversation.close()
            instance.engine.close()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clean up: ${e.message}")
        }

        cleanUpListeners.remove(model.name)?.invoke()
        instances.remove(model.name)
        onDone()
    }

    override fun runInference(
        model: Model,
        input: String,
        resultListener: ResultListener,
        cleanUpListener: CleanUpListener,
        onError: (message: String) -> Unit,
        images: List<Bitmap>,
        audioClips: List<ByteArray>,
        coroutineScope: CoroutineScope?,
        extraContext: Map<String, String>?,
    ) {
        val instance = instances[model.name]
        if (instance == null) {
            onError("Model '${model.name}' not initialized.")
            return
        }

        cleanUpListeners.putIfAbsent(model.name, cleanUpListener)

        val contentsList = mutableListOf<Content>()
        for (image in images) {
            contentsList.add(Content.ImageBytes(image.toPngByteArray()))
        }
        for (audioClip in audioClips) {
            contentsList.add(Content.AudioBytes(audioClip))
        }
        if (input.trim().isNotEmpty()) {
            contentsList.add(Content.Text(input))
        }

        instance.conversation.sendMessageAsync(
            Contents.of(contentsList),
            object : MessageCallback {
                override fun onMessage(message: Message) {
                    resultListener(message.toString(), false, message.channels["thought"])
                }

                override fun onDone() {
                    resultListener("", true, null)
                }

                override fun onError(throwable: Throwable) {
                    if (throwable is CancellationException) {
                        resultListener("", true, null)
                    } else {
                        Log.e(TAG, "Inference error", throwable)
                        onError("Error: ${throwable.message}")
                    }
                }
            },
            extraContext ?: emptyMap(),
        )
    }

    override fun stopResponse(model: Model) {
        instances[model.name]?.conversation?.cancelProcess()
    }

    private fun Bitmap.toPngByteArray(): ByteArray {
        val stream = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
}
