package com.example.data.model

import java.util.UUID

enum class AspectRatio(val label: String, val widthRatio: Float, val heightRatio: Float) {
    RATIO_16_9("16:9 (Landscape)", 16f, 9f),
    RATIO_9_16("9:16 (Shorts/Reels)", 9f, 16f),
    RATIO_1_1("1:1 (Square)", 1f, 1f),
    RATIO_4_5("4:5 (Portrait)", 4f, 5f),
    RATIO_21_9("21:9 (Cinematic)", 21f, 9f);

    val ratio: Float get() = widthRatio / heightRatio
}

data class VideoClip(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val uriString: String,
    val localFilePath: String,
    val durationMs: Long,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = durationMs,
    val speed: Float = 1.0f,
    val rotationDegrees: Int = 0,
    val volume: Float = 1.0f,
    val width: Int = 1920,
    val height: Int = 1080
) {
    /**
     * Effective duration considering trim and playback speed.
     */
    val effectiveDurationMs: Long
        get() {
            val trimmed = (trimEndMs - trimStartMs).coerceAtLeast(0L)
            return (trimmed / speed).toLong()
        }
}

data class SubtitleCue(
    val id: String = UUID.randomUUID().toString(),
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val originalText: String = text,
    val languageCode: String = "hi",
    val speakerTag: String = "Speaker 1"
) {
    val durationMs: Long get() = (endMs - startMs).coerceAtLeast(0L)
}

enum class AudioTrackType {
    BGM,
    DUBBING,
    VOICEOVER
}

data class AudioTrackItem(
    val id: String = UUID.randomUUID().toString(),
    val type: AudioTrackType,
    val title: String,
    val uriOrPath: String,
    val startTimelineMs: Long,
    val durationMs: Long,
    val volume: Float = 1.0f,
    val fadeInMs: Long = 0L,
    val fadeOutMs: Long = 0L,
    val isMuted: Boolean = false,
    val languageCode: String? = null
)

data class TextOverlay(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val startMs: Long,
    val endMs: Long,
    val posX: Float = 0.5f, // Normalized 0..1
    val posY: Float = 0.8f, // Normalized 0..1
    val fontSizeSp: Float = 20f,
    val textColorHex: String = "#FFFFFF",
    val bgColorHex: String = "#80000000"
)

data class SubtitleStyle(
    val fontSizeSp: Float = 18f,
    val textColorHex: String = "#FFFF00", // CapCut vibrant yellow default
    val outlineColorHex: String = "#000000",
    val outlineWidthDp: Float = 2f,
    val bgBoxColorHex: String = "#99000000",
    val verticalPosition: SubtitlePosition = SubtitlePosition.BOTTOM,
    val isBold: Boolean = true
)

enum class SubtitlePosition {
    TOP,
    CENTER,
    BOTTOM
}

enum class ExportResolution(val label: String, val width: Int, val height: Int) {
    RES_720P("720p HD (Fast & Low RAM)", 1280, 720),
    RES_1080P("1080p Full HD (Crisp)", 1920, 1080)
}

enum class ExportFrameRate(val label: String, val fps: Int) {
    FPS_24("24 FPS (Cinematic)", 24),
    FPS_30("30 FPS (Standard)", 30),
    FPS_60("60 FPS (Ultra Smooth)", 60)
}

data class ExportConfig(
    val resolution: ExportResolution = ExportResolution.RES_720P,
    val frameRate: ExportFrameRate = ExportFrameRate.FPS_30,
    val aspectRatio: AspectRatio = AspectRatio.RATIO_9_16,
    val burnSubtitles: Boolean = true,
    val includeDubbing: Boolean = true,
    val includeBgm: Boolean = true,
    val muteOriginalVideo: Boolean = false,
    val targetLanguage: String = "hi"
)

data class Project(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Untitled Studio Project",
    val createdAt: Long = System.currentTimeMillis(),
    val lastModifiedAt: Long = System.currentTimeMillis(),
    val aspectRatio: AspectRatio = AspectRatio.RATIO_9_16,
    val clips: List<VideoClip> = emptyList(),
    val subtitles: List<SubtitleCue> = emptyList(),
    val audioTracks: List<AudioTrackItem> = emptyList(),
    val textOverlays: List<TextOverlay> = emptyList(),
    val subtitleStyle: SubtitleStyle = SubtitleStyle(),
    val originalLanguage: String = "en",
    val targetLanguage: String = "hi",
    val originalVideoVolume: Float = 1.0f,
    val isOriginalVideoMuted: Boolean = false
) {
    val totalDurationMs: Long
        get() {
            val videoDuration = clips.sumOf { it.effectiveDurationMs }
            val audioDuration = audioTracks.maxOfOrNull { it.startTimelineMs + it.durationMs } ?: 0L
            val subtitleDuration = subtitles.maxOfOrNull { it.endMs } ?: 0L
            return maxOf(videoDuration, audioDuration, subtitleDuration).coerceAtLeast(1000L)
        }
}
