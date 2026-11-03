package com.example.domain.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import com.example.data.model.AudioTrackItem
import com.example.data.model.AudioTrackType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class VoiceOverRecorder(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var recordStartTime: Long = 0L
    private var timerJob: Job? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordedDurationMs = MutableStateFlow(0L)
    val recordedDurationMs: StateFlow<Long> = _recordedDurationMs.asStateFlow()

    private val _currentAmplitude = MutableStateFlow(0f)
    val currentAmplitude: StateFlow<Float> = _currentAmplitude.asStateFlow()

    fun startRecording(timelineStartMs: Long, scope: CoroutineScope): Boolean {
        if (_isRecording.value) return false

        val dir = File(context.cacheDir, "voiceovers").apply { mkdirs() }
        val file = File(dir, "vo_${System.currentTimeMillis()}.m4a")
        currentOutputFile = file

        return try {
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            recordStartTime = System.currentTimeMillis()
            _isRecording.value = true

            timerJob?.cancel()
            timerJob = scope.launch(Dispatchers.Default) {
                while (isActive && _isRecording.value) {
                    _recordedDurationMs.value = System.currentTimeMillis() - recordStartTime
                    try {
                        val maxAmp = mediaRecorder?.maxAmplitude ?: 0
                        _currentAmplitude.value = (maxAmp / 32767f).coerceIn(0f, 1f)
                    } catch (_: Exception) {}
                    delay(100)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            stopRecording(timelineStartMs)
            false
        }
    }

    fun stopRecording(timelineStartMs: Long): AudioTrackItem? {
        timerJob?.cancel()
        timerJob = null

        val duration = _recordedDurationMs.value
        _isRecording.value = false
        _currentAmplitude.value = 0f
        _recordedDurationMs.value = 0L

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder = null
        }

        val file = currentOutputFile
        if (file != null && file.exists() && file.length() > 0 && duration > 500L) {
            return AudioTrackItem(
                id = UUID.randomUUID().toString(),
                type = AudioTrackType.VOICEOVER,
                title = "Voice-over (${duration / 1000}s)",
                uriOrPath = file.absolutePath,
                startTimelineMs = timelineStartMs,
                durationMs = duration,
                volume = 1.0f
            )
        }
        return null
    }

    fun cancelRecording() {
        timerJob?.cancel()
        timerJob = null
        _isRecording.value = false
        _recordedDurationMs.value = 0L
        _currentAmplitude.value = 0f
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (_: Exception) {}
        mediaRecorder = null
        currentOutputFile?.delete()
        currentOutputFile = null
    }
}
