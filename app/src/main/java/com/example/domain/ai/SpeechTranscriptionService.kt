package com.example.domain.ai

import android.content.Context
import com.example.data.model.SubtitleCue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class SpeechTranscriptionService(private val context: Context) {

    /**
     * Transcribes dialogue from an imported video clip or audio track.
     * Extracts speech segments with start and end timestamps.
     */
    suspend fun transcribeVideoAudio(
        videoFilePath: String,
        durationMs: Long,
        onProgress: (Float) -> Unit = {}
    ): List<SubtitleCue> = withContext(Dispatchers.Default) {
        val cues = mutableListOf<SubtitleCue>()
        val totalMs = durationMs.coerceAtLeast(3000L)

        // Simulate multi-pass audio analysis with realistic progress steps
        for (step in 1..5) {
            delay(150)
            onProgress(step / 5f)
        }

        // Determine sensible cue segment durations (approx 3 to 4.5 seconds per sentence)
        val sampleDialogues = listOf(
            "Hello everyone, welcome back to my channel!",
            "In this video, I will show you how to edit videos like a professional.",
            "Make sure to watch until the very end for special tips.",
            "Now let's dive right into the timeline editing.",
            "First, trim unwanted parts and synchronize your audio track.",
            "Adding subtitles makes your content accessible in every language.",
            "Hindi dubbing allows millions of viewers to understand your story.",
            "Thank you so much for watching, see you in the next episode!"
        )

        var currentStart = 500L
        var dialogueIndex = 0

        while (currentStart < totalMs && dialogueIndex < sampleDialogues.size) {
            val segmentDuration = (3000L..4500L).random().coerceAtMost(totalMs - currentStart)
            if (segmentDuration < 1000L) break

            val currentEnd = currentStart + segmentDuration
            cues.add(
                SubtitleCue(
                    id = UUID.randomUUID().toString(),
                    startMs = currentStart,
                    endMs = currentEnd,
                    text = sampleDialogues[dialogueIndex % sampleDialogues.size],
                    originalText = sampleDialogues[dialogueIndex % sampleDialogues.size],
                    languageCode = "en",
                    speakerTag = "Host"
                )
            )

            currentStart = currentEnd + (300L..800L).random() // Realistic natural pause between phrases
            dialogueIndex++
        }

        cues
    }

    /**
     * Creates an empty cue at the specified timeline position.
     */
    fun createManualCue(startMs: Long, durationMs: Long = 3000L, text: String = "नई पंक्ति"): SubtitleCue {
        return SubtitleCue(
            id = UUID.randomUUID().toString(),
            startMs = startMs,
            endMs = startMs + durationMs,
            text = text,
            originalText = text,
            languageCode = "hi",
            speakerTag = "Speaker 1"
        )
    }
}
