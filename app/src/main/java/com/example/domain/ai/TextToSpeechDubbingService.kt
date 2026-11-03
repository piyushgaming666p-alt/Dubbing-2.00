package com.example.domain.ai

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.example.data.model.AudioTrackItem
import com.example.data.model.AudioTrackType
import com.example.data.model.LanguageCatalog
import com.example.data.model.SubtitleCue
import com.example.data.model.SupportedLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume

data class DubbingVoice(
    val id: String,
    val name: String,
    val languageCode: String,
    val gender: String, // "Male", "Female", "Default"
    val isNetworkRequired: Boolean = false
)

class TextToSpeechDubbingService(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<DubbingVoice>>(emptyList())
    val availableVoices: StateFlow<List<DubbingVoice>> = _availableVoices.asStateFlow()

    private var previewPlayer: MediaPlayer? = null

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            _isInitialized.value = true
            refreshAvailableVoices(LanguageCatalog.HINDI)
        }
    }

    fun refreshAvailableVoices(language: SupportedLanguage) {
        val ttsInstance = tts ?: return
        try {
            val systemVoices = ttsInstance.voices ?: emptySet()
            val filtered = systemVoices.filter { voice ->
                voice.locale.language.equals(language.locale.language, ignoreCase = true)
            }.map { voice ->
                val gender = when {
                    voice.name.contains("female", ignoreCase = true) || voice.name.contains("f0", ignoreCase = true) -> "Female"
                    voice.name.contains("male", ignoreCase = true) || voice.name.contains("m0", ignoreCase = true) -> "Male"
                    else -> "Studio Voice ${voice.name.takeLast(4)}"
                }
                DubbingVoice(
                    id = voice.name,
                    name = "${language.name} - $gender",
                    languageCode = language.code,
                    gender = gender,
                    isNetworkRequired = voice.isNetworkConnectionRequired
                )
            }

            if (filtered.isNotEmpty()) {
                _availableVoices.value = filtered
            } else {
                // Provide preset voice profiles for Hindi / target languages
                _availableVoices.value = listOf(
                    DubbingVoice(
                        id = "hi_female_natural",
                        name = "${language.name} - Studio Aarti (Female)",
                        languageCode = language.code,
                        gender = "Female"
                    ),
                    DubbingVoice(
                        id = "hi_male_cinematic",
                        name = "${language.name} - Studio Rohan (Male)",
                        languageCode = language.code,
                        gender = "Male"
                    ),
                    DubbingVoice(
                        id = "hi_conversational",
                        name = "${language.name} - Conversational Host",
                        languageCode = language.code,
                        gender = "Neutral"
                    )
                )
            }
        } catch (_: Exception) {
            _availableVoices.value = listOf(
                DubbingVoice("default_hi", "${language.name} - Natural Voice", language.code, "Neutral")
            )
        }
    }

    /**
     * Previews speech using the TTS engine directly through device speaker.
     */
    fun previewSpeech(
        text: String,
        language: SupportedLanguage,
        voiceId: String? = null,
        pitch: Float = 1.0f,
        speechRate: Float = 1.0f
    ) {
        val ttsInstance = tts ?: return
        ttsInstance.language = language.locale
        ttsInstance.setPitch(pitch)
        ttsInstance.setSpeechRate(speechRate)

        voiceId?.let { id ->
            ttsInstance.voices?.firstOrNull { it.name == id }?.let { voice ->
                ttsInstance.voice = voice
            }
        }

        val utteranceId = "preview_${System.currentTimeMillis()}"
        ttsInstance.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stopPreview() {
        tts?.stop()
        previewPlayer?.stop()
        previewPlayer?.release()
        previewPlayer = null
    }

    /**
     * Synthesizes real audio file (.wav) on disk for a single subtitle dialogue.
     */
    suspend fun synthesizeSubtitleAudio(
        cue: SubtitleCue,
        language: SupportedLanguage,
        voiceId: String? = null,
        pitch: Float = 1.0f,
        speechRate: Float = 1.0f
    ): AudioTrackItem? = withContext(Dispatchers.IO) {
        val ttsInstance = tts ?: return@withContext null
        if (!_isInitialized.value) return@withContext null

        ttsInstance.language = language.locale
        ttsInstance.setPitch(pitch)
        ttsInstance.setSpeechRate(speechRate)

        voiceId?.let { id ->
            ttsInstance.voices?.firstOrNull { it.name == id }?.let { voice ->
                ttsInstance.voice = voice
            }
        }

        val dubbingDir = File(context.cacheDir, "dubbing").apply { mkdirs() }
        val outputFile = File(dubbingDir, "dub_${cue.id}_${System.currentTimeMillis()}.wav")
        val utteranceId = "synth_${UUID.randomUUID()}"

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }

        val success = suspendCancellableCoroutine<Boolean> { cont ->
            ttsInstance.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(uttId: String?) {}
                override fun onDone(uttId: String?) {
                    if (uttId == utteranceId) {
                        cont.resume(true)
                    }
                }
                override fun onError(uttId: String?) {
                    if (uttId == utteranceId) {
                        cont.resume(false)
                    }
                }
            })

            val status = ttsInstance.synthesizeToFile(cue.text, params, outputFile, utteranceId)
            if (status != TextToSpeech.SUCCESS) {
                cont.resume(false)
            }
        }

        if (success && outputFile.exists() && outputFile.length() > 0) {
            val duration = getAudioFileDurationMs(outputFile)
            AudioTrackItem(
                id = UUID.randomUUID().toString(),
                type = AudioTrackType.DUBBING,
                title = "Hindi Dub: ${cue.text.take(24)}...",
                uriOrPath = outputFile.absolutePath,
                startTimelineMs = cue.startMs,
                durationMs = duration.coerceAtLeast(cue.durationMs),
                volume = 1.0f,
                languageCode = language.code
            )
        } else {
            null
        }
    }

    /**
     * Synthesizes audio for all subtitle cues in the project.
     * Generates synchronized dubbed audio clips along the timeline.
     */
    suspend fun synthesizeFullDubbing(
        cues: List<SubtitleCue>,
        language: SupportedLanguage,
        voiceId: String? = null,
        pitch: Float = 1.0f,
        speechRate: Float = 1.0f,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): List<AudioTrackItem> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<AudioTrackItem>()
        cues.forEachIndexed { index, cue ->
            onProgress(index + 1, cues.size)
            val audioTrack = synthesizeSubtitleAudio(cue, language, voiceId, pitch, speechRate)
            if (audioTrack != null) {
                tracks.add(audioTrack)
            }
        }
        tracks
    }

    private fun getAudioFileDurationMs(file: File): Long {
        return try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val durStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            durStr?.toLongOrNull() ?: 2000L
        } catch (_: Exception) {
            2000L
        }
    }

    fun release() {
        try {
            stopPreview()
            tts?.shutdown()
            tts = null
        } catch (_: Exception) {}
    }
}
