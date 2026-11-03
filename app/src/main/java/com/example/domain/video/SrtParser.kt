package com.example.domain.video

import com.example.data.model.SubtitleCue
import java.util.UUID

object SrtParser {

    /**
     * Converts SubtitleCues list to standard SRT text.
     */
    fun exportToSrt(cues: List<SubtitleCue>): String {
        val sb = StringBuilder()
        cues.sortedBy { it.startMs }.forEachIndexed { index, cue ->
            sb.append(index + 1).append("\n")
            sb.append(formatSrtTimestamp(cue.startMs))
                .append(" --> ")
                .append(formatSrtTimestamp(cue.endMs))
                .append("\n")
            sb.append(cue.text).append("\n\n")
        }
        return sb.toString()
    }

    /**
     * Parses standard SRT text into a list of SubtitleCue objects.
     */
    fun parseFromSrt(srtContent: String, languageCode: String = "hi"): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        val blocks = srtContent.replace("\r\n", "\n").split("\n\n")

        for (block in blocks) {
            val lines = block.lines().filter { it.isNotBlank() }
            if (lines.size < 2) continue

            // Line 0 is usually index (or timestamp if index omitted)
            var timeLineIndex = 0
            if (lines[0].trim().all { it.isDigit() } && lines.size >= 3) {
                timeLineIndex = 1
            }

            val timeLine = lines[timeLineIndex]
            if (!timeLine.contains("-->")) continue

            val timeParts = timeLine.split("-->")
            if (timeParts.size != 2) continue

            val startMs = parseSrtTimestamp(timeParts[0].trim())
            val endMs = parseSrtTimestamp(timeParts[1].trim())
            val text = lines.drop(timeLineIndex + 1).joinToString("\n").trim()

            if (text.isNotEmpty() && endMs > startMs) {
                cues.add(
                    SubtitleCue(
                        id = UUID.randomUUID().toString(),
                        startMs = startMs,
                        endMs = endMs,
                        text = text,
                        originalText = text,
                        languageCode = languageCode
                    )
                )
            }
        }
        return cues
    }

    private fun formatSrtTimestamp(millis: Long): String {
        val totalSec = millis / 1000
        val ms = millis % 1000
        val sec = totalSec % 60
        val min = (totalSec / 60) % 60
        val hours = totalSec / 3600
        return String.format("%02d:%02d:%02d,%03d", hours, min, sec, ms)
    }

    private fun parseSrtTimestamp(timestamp: String): Long {
        return try {
            val parts = timestamp.replace(".", ",").split(",")
            val timeParts = parts[0].split(":")
            val ms = if (parts.size > 1) parts[1].toLongOrNull() ?: 0L else 0L

            val hours = timeParts.getOrNull(0)?.toLongOrNull() ?: 0L
            val minutes = timeParts.getOrNull(1)?.toLongOrNull() ?: 0L
            val seconds = timeParts.getOrNull(2)?.toLongOrNull() ?: 0L

            (hours * 3600 + minutes * 60 + seconds) * 1000L + ms
        } catch (_: Exception) {
            0L
        }
    }
}
