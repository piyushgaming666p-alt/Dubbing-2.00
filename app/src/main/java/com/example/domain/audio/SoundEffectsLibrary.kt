package com.example.domain.audio

import android.content.Context
import com.example.data.model.AudioTrackItem
import com.example.data.model.AudioTrackType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import kotlin.math.PI
import kotlin.math.sin

data class SoundPreset(
    val id: String,
    val title: String,
    val category: String, // "BGM", "Transition", "Impact", "Ambient"
    val durationMs: Long,
    val iconName: String,
    val description: String
)

class SoundEffectsLibrary(private val context: Context) {

    val presets: List<SoundPreset> = listOf(
        SoundPreset("bgm_lofi", "Lo-Fi Studio Chill", "BGM", 15000L, "music_note", "Relaxing chill beat for vlogs"),
        SoundPreset("bgm_cinematic", "Cinematic Swell", "BGM", 12000L, "auto_awesome", "Emotional cinematic build-up"),
        SoundPreset("bgm_upbeat", "Energize Beat", "BGM", 10000L, "bolt", "Fast-paced vlog background beat"),
        SoundPreset("sfx_whoosh", "Transition Whoosh", "Transition", 1200L, "speed", "Sleek cut transition effect"),
        SoundPreset("sfx_bell", "Notification Chime", "Impact", 1500L, "notifications", "Crisp high-bell accent"),
        SoundPreset("sfx_applause", "Audience Cheer", "Impact", 4000L, "celebration", "Crowd appreciation applause"),
        SoundPreset("sfx_sub_bass", "Deep Bass Drop", "Impact", 2500L, "graphic_eq", "Heavy bass transition hit")
    )

    /**
     * Generates or retrieves a valid WAV audio file for the requested preset.
     */
    suspend fun getAudioFileForPreset(preset: SoundPreset): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "audio_library").apply { mkdirs() }
        val file = File(dir, "${preset.id}.wav")
        if (file.exists() && file.length() > 1000L) {
            return@withContext file
        }

        // Synthesize valid 44100Hz 16-bit PCM WAV file
        val sampleRate = 44100
        val durationSec = preset.durationMs / 1000.0
        val totalSamples = (sampleRate * durationSec).toInt()
        val shortBuffer = ShortArray(totalSamples)

        when (preset.id) {
            "sfx_whoosh" -> {
                // Modulated noise sweep
                for (i in 0 until totalSamples) {
                    val progress = i.toDouble() / totalSamples
                    val freq = 200.0 + 800.0 * sin(progress * PI)
                    val envelope = sin(progress * PI)
                    val noise = (Math.random() * 2.0 - 1.0) * 0.4
                    val tone = sin(2.0 * PI * freq * i / sampleRate) * 0.6
                    shortBuffer[i] = ((tone + noise) * envelope * 22000).toInt().toShort()
                }
            }
            "sfx_bell" -> {
                // Chime with exponential decay
                val freqs = listOf(1046.5, 1318.5, 1567.9) // C6, E6, G6
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    val decay = kotlin.math.exp(-3.5 * t)
                    var sample = 0.0
                    for (f in freqs) {
                        sample += sin(2.0 * PI * f * t) * (1.0 / freqs.size)
                    }
                    shortBuffer[i] = (sample * decay * 26000).toInt().toShort()
                }
            }
            "sfx_sub_bass" -> {
                for (i in 0 until totalSamples) {
                    val progress = i.toDouble() / totalSamples
                    val freq = 120.0 - (progress * 70.0) // 120Hz down to 50Hz
                    val decay = kotlin.math.exp(-1.5 * progress)
                    val sample = sin(2.0 * PI * freq * i / sampleRate) * decay
                    shortBuffer[i] = (sample * 28000).toInt().toShort()
                }
            }
            "sfx_applause" -> {
                // Dense applause grain texture
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    val env = (1.0 - kotlin.math.exp(-4.0 * t)) * kotlin.math.exp(-0.4 * t)
                    val burst = if (Math.random() > 0.85) (Math.random() * 2.0 - 1.0) else 0.0
                    val noise = (Math.random() * 2.0 - 1.0) * 0.3
                    shortBuffer[i] = ((burst + noise) * env * 24000).toInt().toShort()
                }
            }
            else -> {
                // Musical progression for BGM (Lo-fi / Cinematic / Upbeat)
                val baseFreqs = if (preset.id == "bgm_cinematic") {
                    listOf(130.81, 164.81, 196.0, 246.94) // C-E-G-B
                } else {
                    listOf(146.83, 174.61, 220.0, 261.63) // Dm7 lo-fi
                }

                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    val beatIndex = ((t * 1.5).toInt()) % baseFreqs.size
                    val freq = baseFreqs[beatIndex]
                    val beatProgress = (t * 1.5) - (t * 1.5).toInt()
                    val env = kotlin.math.exp(-2.0 * beatProgress)
                    val melody = sin(2.0 * PI * freq * t) * 0.6 + sin(2.0 * PI * freq * 2.0 * t) * 0.2
                    shortBuffer[i] = (melody * env * 20000).toInt().toShort()
                }
            }
        }

        writePcmToWav(file, shortBuffer, sampleRate)
        file
    }

    suspend fun createTrackItem(preset: SoundPreset, startTimelineMs: Long): AudioTrackItem {
        val file = getAudioFileForPreset(preset)
        val trackType = if (preset.category == "BGM") AudioTrackType.BGM else AudioTrackType.BGM
        return AudioTrackItem(
            id = UUID.randomUUID().toString(),
            type = trackType,
            title = preset.title,
            uriOrPath = file.absolutePath,
            startTimelineMs = startTimelineMs,
            durationMs = preset.durationMs,
            volume = if (preset.category == "BGM") 0.45f else 0.85f
        )
    }

    private fun writePcmToWav(file: File, samples: ShortArray, sampleRate: Int) {
        val byteData = ByteArray(samples.size * 2)
        val buffer = ByteBuffer.wrap(byteData).order(ByteOrder.LITTLE_ENDIAN)
        for (sample in samples) {
            buffer.putShort(sample)
        }

        val totalDataLen = byteData.size + 36
        val byteRate = sampleRate * 1 * 2

        FileOutputStream(file).use { out ->
            // RIFF header
            out.write("RIFF".toByteArray())
            out.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(totalDataLen).array())
            out.write("WAVE".toByteArray())
            // fmt chunk
            out.write("fmt ".toByteArray())
            out.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(16).array())
            out.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(1.toShort()).array()) // PCM
            out.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(1.toShort()).array()) // 1 channel
            out.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(sampleRate).array())
            out.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(byteRate).array())
            out.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(2.toShort()).array()) // block align
            out.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(16.toShort()).array()) // bits per sample
            // data chunk
            out.write("data".toByteArray())
            out.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(byteData.size).array())
            out.write(byteData)
        }
    }
}
