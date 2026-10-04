package com.mytv.remote.voice

import android.content.Context
import android.content.Intent
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

/**
 * Thin wrapper around on-device speech recognition. Results are handed
 * back as plain text; RemoteClient.sendSearch() turns that into a
 * search app-link sent to the TV. No cloud voice service of our own is
 * involved — this is the same on-device recognizer Android's keyboard
 * mic button uses.
 */
class VoiceController(context: Context) {

    private val recognizer: SpeechRecognizer? =
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else null

    fun isAvailable(): Boolean = recognizer != null

    fun listen(onResult: (String) -> Unit, onError: (String) -> Unit) {
        val r = recognizer ?: return onError("Speech recognition not available on this device")

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        r.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: android.os.Bundle) {
                val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()
                if (text != null) onResult(text) else onError("Didn't catch that")
            }

            override fun onError(error: Int) = onError("Recognition error ($error)")

            override fun onReadyForSpeech(params: android.os.Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: android.os.Bundle?) {}
            override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
        })

        r.startListening(intent)
    }

    fun destroy() {
        recognizer?.destroy()
    }
}
