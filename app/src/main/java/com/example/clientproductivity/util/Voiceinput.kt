package com.example.clientproductivity.util

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.Locale

enum class VoiceInputState { IDLE, LISTENING, ERROR }

@Composable
fun rememberVoiceInputController(
    onResult: (ParsedTaskInput) -> Unit,
    onError: (() -> Unit)? = null
): VoiceInputController {
    val context = LocalContext.current
    val controller = remember {
        VoiceInputController(context, onResult, onError)
    }
    DisposableEffect(Unit) {
        onDispose { controller.destroy() }
    }
    return controller
}

class VoiceInputController(
    private val context: android.content.Context,
    private val onResult: (ParsedTaskInput) -> Unit,
    private val onError: (() -> Unit)?
) {
    var state by mutableStateOf(VoiceInputState.IDLE)
        private set

    private var recognizer: SpeechRecognizer? = null

    fun start() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            state = VoiceInputState.ERROR
            onError?.invoke()
            return
        }

        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    state = VoiceInputState.LISTENING
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val transcript = matches?.firstOrNull() ?: ""
                    state = VoiceInputState.IDLE
                    if (transcript.isNotBlank()) {
                        onResult(VoiceParser.parse(transcript))
                    }
                }
                override fun onError(error: Int) {
                    state = VoiceInputState.IDLE
                    onError?.invoke()
                }
                override fun onBeginningOfSpeech() {}
                override fun onEndOfSpeech() { state = VoiceInputState.IDLE }
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
                override fun onRmsChanged(rmsdB: Float) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
        }
        recognizer?.startListening(intent)
    }

    fun destroy() {
        recognizer?.destroy()
        recognizer = null
    }
}

@Composable
fun VoiceInputButton(
    controller: VoiceInputController,
    tint: Color = MaterialTheme.colorScheme.primary,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    val isListening = controller.state == VoiceInputState.LISTENING
    val isError = controller.state == VoiceInputState.ERROR

    val infiniteTransition = rememberInfiniteTransition(label = "micPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "micScale"
    )

    IconButton(
        onClick = { if (!isListening) controller.start() },
        modifier = modifier
    ) {
        Icon(
            imageVector = if (isError) Icons.Default.MicOff else Icons.Default.Mic,
            contentDescription = if (isListening) "Listening…" else "Voice input",
            tint = when {
                isListening -> Color(0xFFD32F2F)
                isError -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                else -> tint
            },
            modifier = Modifier
                .size(24.dp)
                .then(if (isListening) Modifier.scale(scale) else Modifier)
        )
    }
}