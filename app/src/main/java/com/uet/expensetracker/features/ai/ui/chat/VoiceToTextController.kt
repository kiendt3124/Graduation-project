package com.uet.expensetracker.features.ai.ui.chat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Small wrapper around Android SpeechRecognizer to support:
 * - start/stop listening
 * - partial result streaming
 *
 * Keep UI concerns outside; use callbacks.
 */
class VoiceToTextController(
    context: Context,
    private val onPartial: (String) -> Unit,
    private val onFinal: (String) -> Unit,
    private val onError: (String) -> Unit,
) {
    private val appContext = context.applicationContext
    private var speechRecognizer: SpeechRecognizer? = null

    val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(appContext)

    fun start(locale: String? = null) {
        if (!isAvailable) {
            onError("Voice-to-text không khả dụng trên thiết bị này")
            return
        }

        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(appContext).apply {
                setRecognitionListener(listener)
            }
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
            // Prefer device locale if not specified.
            if (!locale.isNullOrBlank()) {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
            }
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            onError("Không thể bắt đầu ghi âm: ${e.message}")
        }
    }

    fun stop() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {
            // ignore
        }
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {
            // ignore
        } finally {
            speechRecognizer = null
        }
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}

        override fun onError(error: Int) {
            // Do not spam; map a few common errors to friendly Vietnamese.
            val msg = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Lỗi âm thanh"
                SpeechRecognizer.ERROR_CLIENT -> "Voice-to-text đã dừng"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Thiếu quyền micro"
                SpeechRecognizer.ERROR_NETWORK -> "Lỗi mạng khi nhận giọng nói"
                SpeechRecognizer.ERROR_NO_MATCH -> "Không nhận diện được"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Không nghe thấy giọng nói"
                else -> "Voice-to-text lỗi ($error)"
            }
            onError(msg)
        }

        override fun onResults(results: Bundle?) {
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()
            if (text.isNotBlank()) onFinal(text)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val text = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()
            if (text.isNotBlank()) onPartial(text)
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}


