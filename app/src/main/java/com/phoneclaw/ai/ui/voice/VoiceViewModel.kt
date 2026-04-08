package com.phoneclaw.ai.ui.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoneclaw.ai.data.repository.ConversationRepository
import com.phoneclaw.ai.data.repository.ModelRepository
import com.phoneclaw.ai.runtime.LlmModelHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

enum class VoiceState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
}

data class VoiceUiState(
    val voiceState: VoiceState = VoiceState.IDLE,
    val isMuted: Boolean = false,
    val isRecording: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class VoiceViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelRepository: ModelRepository,
    private val conversationRepository: ConversationRepository,
    private val llmModelHelper: LlmModelHelper,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceUiState())
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    private val _transcript = MutableStateFlow("")
    val transcript: StateFlow<String> = _transcript.asStateFlow()

    private val _aiResponse = MutableStateFlow("")
    val aiResponse: StateFlow<String> = _aiResponse.asStateFlow()

    val isRecording: StateFlow<Boolean> = _uiState
        .map { it.isRecording }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    val activeModelName: StateFlow<String?> = modelRepository.activeModel
        .map { it?.displayName }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var ttsReady = false

    init {
        initTts()
    }

    private fun initTts() {
        textToSpeech = TextToSpeech(context) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) {
                textToSpeech?.language = Locale.getDefault()
            }
        }
    }

    fun startListening() {
        if (_uiState.value.isMuted) return
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Speech recognition is not available on this device",
            )
            return
        }

        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _uiState.value = _uiState.value.copy(
                        voiceState = VoiceState.LISTENING,
                        isRecording = true,
                    )
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _uiState.value = _uiState.value.copy(
                        voiceState = VoiceState.PROCESSING,
                        isRecording = false,
                    )
                }

                override fun onError(error: Int) {
                    _uiState.value = _uiState.value.copy(
                        voiceState = VoiceState.IDLE,
                        isRecording = false,
                    )
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: return
                    _transcript.value = text
                    processTranscript(text)
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val partial = matches?.firstOrNull() ?: return
                    _transcript.value = partial
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _uiState.value = _uiState.value.copy(
            voiceState = VoiceState.PROCESSING,
            isRecording = false,
        )
    }

    fun processTranscript(text: String) {
        if (text.isBlank()) {
            _uiState.value = _uiState.value.copy(voiceState = VoiceState.IDLE)
            return
        }

        val activeModel = modelRepository.activeModel.value ?: run {
            _uiState.value = _uiState.value.copy(
                voiceState = VoiceState.IDLE,
                errorMessage = "No model selected",
            )
            return
        }

        _uiState.value = _uiState.value.copy(voiceState = VoiceState.PROCESSING)
        _aiResponse.value = ""

        viewModelScope.launch {
            llmModelHelper.runInference(
                model = activeModel,
                input = text,
                resultListener = { partial, done, _ ->
                    _aiResponse.value = _aiResponse.value + partial
                    if (done) {
                        val fullResponse = _aiResponse.value
                        speakResponse(fullResponse)
                    }
                },
                cleanUpListener = {},
                onError = { error ->
                    _uiState.value = _uiState.value.copy(
                        voiceState = VoiceState.IDLE,
                        errorMessage = error,
                    )
                },
                coroutineScope = viewModelScope,
            )
        }
    }

    private fun speakResponse(text: String) {
        if (_uiState.value.isMuted || !ttsReady) {
            _uiState.value = _uiState.value.copy(voiceState = VoiceState.IDLE)
            return
        }

        _uiState.value = _uiState.value.copy(voiceState = VoiceState.SPEAKING)

        textToSpeech?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                _uiState.value = _uiState.value.copy(voiceState = VoiceState.IDLE)
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _uiState.value = _uiState.value.copy(voiceState = VoiceState.IDLE)
            }
        })

        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "voice_response")
    }

    fun toggleMute() {
        val muted = !_uiState.value.isMuted
        _uiState.value = _uiState.value.copy(isMuted = muted)
        if (muted) {
            textToSpeech?.stop()
            if (_uiState.value.voiceState == VoiceState.SPEAKING) {
                _uiState.value = _uiState.value.copy(voiceState = VoiceState.IDLE)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun endConversation() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        textToSpeech?.stop()
        _uiState.value = _uiState.value.copy(voiceState = VoiceState.IDLE, isRecording = false)
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer?.destroy()
        speechRecognizer = null
        textToSpeech?.shutdown()
        textToSpeech = null
    }
}
