package com.example.domain.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.os.Environment
import com.example.data.model.AspectRatio
import com.example.data.model.ExportConfig
import com.example.data.model.ExportResolution
import com.example.data.model.Project
import com.example.data.model.SubtitleCue
import com.example.data.model.SubtitlePosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

data class ExportResult(
    val isSuccess: Boolean,
    val outputFile: File?,
    val error: String? = null
)

class VideoExporter(private val context: Context) {

    suspend fun exportProject(
        project: Project,
        config: ExportConfig,
        onProgress: (progress: Float, statusMessage: String) -> Unit
    ): ExportResult = withContext(Dispatchers.IO) {
        val exportDir = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir, "BhashaStudio").apply { mkdirs() }
        val outputMp4File = File(exportDir, "BhashaStudio_${System.currentTimeMillis()}.mp4")

        try {
            onProgress(0.05f, "Preparing export pipeline...")

            val primaryClip = project.clips.firstOrNull()
            if (primaryClip == null) {
                return@withContext ExportResult(false, null, "No video clips found in project to export.")
            }

            // Determine dimensions based on AspectRatio & Resolution
            val targetWidth: Int
            val targetHeight: Int
            if (config.aspectRatio == AspectRatio.RATIO_9_16) {
                targetWidth = if (config.resolution == ExportResolution.RES_1080P) 1080 else 720
                targetHeight = if (config.resolution == ExportResolution.RES_1080P) 1920 else 1280
            } else if (config.aspectRatio == AspectRatio.RATIO_1_1) {
                val size = if (config.resolution == ExportResolution.RES_1080P) 1080 else 720
                targetWidth = size
                targetHeight = size
            } else {
                targetWidth = if (config.resolution == ExportResolution.RES_1080P) 1920 else 1280
                targetHeight = if (config.resolution == ExportResolution.RES_1080P) 1080 else 720
            }

            val fps = config.frameRate.fps
            val bitRate = if (config.resolution == ExportResolution.RES_1080P) 4_000_000 else 2_200_000

            onProgress(0.12f, "Configuring video encoder (${targetWidth}x${targetHeight} @ ${fps}fps)...")

            val videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, targetWidth, targetHeight).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            encoder.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val inputSurface = encoder.createInputSurface()
            encoder.start()

            val muxer = MediaMuxer(outputMp4File.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var videoTrackIndex = -1
            var muxerStarted = false
            val bufferInfo = MediaCodec.BufferInfo()

            // Calculate total duration in seconds
            val totalDurationMs = project.totalDurationMs.coerceAtLeast(1000L)
            val totalFrames = ((totalDurationMs / 1000.0) * fps).toInt().coerceAtLeast(fps)

            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(primaryClip.localFilePath)
            } catch (_: Exception) {}

            val paint = Paint().apply { isFilterBitmap = true; isAntiAlias = true }
            val subtitleTextPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor(project.subtitleStyle.textColorHex)
                textSize = (targetHeight * 0.038f).coerceIn(24f, 54f)
                textAlign = Paint.Align.CENTER
                isFakeBoldText = project.subtitleStyle.isBold
            }
            val subtitleOutlinePaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeWidth = 6f
                color = android.graphics.Color.parseColor(project.subtitleStyle.outlineColorHex)
                textSize = subtitleTextPaint.textSize
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
            }
            val subtitleBgPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor(project.subtitleStyle.bgBoxColorHex)
            }

            var lastExtractedFrame: Bitmap? = null
            var lastExtractedSec = -1L

            onProgress(0.20f, "Rendering frames with subtitles & overlays...")

            for (frameIndex in 0 until totalFrames) {
                val currentTimelineMs = ((frameIndex.toDouble() / fps) * 1000).toLong()
                val currentSec = currentTimelineMs / 1000

                // Periodically extract source frame to avoid memory pressure on 4-6 GB RAM devices
                if (currentSec != lastExtractedSec) {
                    lastExtractedSec = currentSec
                    val clipTimeUs = (primaryClip.trimStartMs + currentTimelineMs) * 1000L
                    try {
                        val newFrame = retriever.getFrameAtTime(clipTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        if (newFrame != null) {
                            lastExtractedFrame?.recycle()
                            lastExtractedFrame = newFrame
                        }
                    } catch (_: Exception) {}
                }

                val canvas: Canvas = inputSurface.lockCanvas(null)
                canvas.drawColor(Color.BLACK)

                // Render video frame letterboxed into target aspect ratio
                lastExtractedFrame?.let { frameBmp ->
                    if (!frameBmp.isRecycled) {
                        val srcRect = Rect(0, 0, frameBmp.width, frameBmp.height)
                        val frameRatio = frameBmp.width.toFloat() / frameBmp.height.toFloat()
                        val targetRatio = targetWidth.toFloat() / targetHeight.toFloat()

                        val dstRect = if (frameRatio > targetRatio) {
                            val h = targetWidth / frameRatio
                            val top = (targetHeight - h) / 2f
                            RectF(0f, top, targetWidth.toFloat(), top + h)
                        } else {
                            val w = targetHeight * frameRatio
                            val left = (targetWidth - w) / 2f
                            RectF(left, 0f, left + w, targetHeight.toFloat())
                        }
                        canvas.drawBitmap(frameBmp, srcRect, dstRect, paint)
                    }
                }

                // Burn-in Subtitles
                if (config.burnSubtitles) {
                    val activeCue = project.subtitles.firstOrNull { cue ->
                        currentTimelineMs in cue.startMs..cue.endMs
                    }

                    if (activeCue != null) {
                        val subText = activeCue.text
                        val bounds = Rect()
                        subtitleTextPaint.getTextBounds(subText, 0, subText.length, bounds)

                        val posY = when (project.subtitleStyle.verticalPosition) {
                            SubtitlePosition.TOP -> targetHeight * 0.15f
                            SubtitlePosition.CENTER -> targetHeight * 0.50f
                            SubtitlePosition.BOTTOM -> targetHeight * 0.85f
                        }
                        val posX = targetWidth / 2f

                        // Draw background pill
                        val paddingH = 32f
                        val paddingV = 16f
                        val bgRect = RectF(
                            posX - bounds.width() / 2f - paddingH,
                            posY + bounds.top - paddingV,
                            posX + bounds.width() / 2f + paddingH,
                            posY + bounds.bottom + paddingV
                        )
                        canvas.drawRoundRect(bgRect, 20f, 20f, subtitleBgPaint)

                        // Draw outline & text
                        canvas.drawText(subText, posX, posY, subtitleOutlinePaint)
                        canvas.drawText(subText, posX, posY, subtitleTextPaint)
                    }
                }

                // Text Overlays
                project.textOverlays.forEach { overlay ->
                    if (currentTimelineMs in overlay.startMs..overlay.endMs) {
                        val oX = overlay.posX * targetWidth
                        val oY = overlay.posY * targetHeight
                        val oPaint = Paint().apply {
                            isAntiAlias = true
                            color = try { Color.parseColor(overlay.textColorHex) } catch (_: Exception) { Color.WHITE }
                            textSize = overlay.fontSizeSp * 2.2f
                            textAlign = Paint.Align.CENTER
                            isFakeBoldText = true
                        }
                        canvas.drawText(overlay.text, oX, oY, oPaint)
                    }
                }

                inputSurface.unlockCanvasAndPost(canvas)

                // Drain encoder
                while (true) {
                    val outIndex = encoder.dequeueOutputBuffer(bufferInfo, 0)
                    if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        videoTrackIndex = muxer.addTrack(encoder.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    } else if (outIndex >= 0) {
                        val encodedData = encoder.getOutputBuffer(outIndex)
                        if (encodedData != null && muxerStarted && bufferInfo.size > 0) {
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                        }
                        encoder.releaseOutputBuffer(outIndex, false)
                    } else {
                        break
                    }
                }

                if (frameIndex % (fps / 2) == 0) {
                    val progress = 0.20f + 0.65f * (frameIndex.toFloat() / totalFrames)
                    onProgress(progress, "Rendering video: ${(progress * 100).toInt()}%")
                }
            }

            // Signal EOF
            encoder.signalEndOfInputStream()
            while (true) {
                val outIndex = encoder.dequeueOutputBuffer(bufferInfo, 10000)
                if (outIndex >= 0) {
                    val encodedData = encoder.getOutputBuffer(outIndex)
                    if (encodedData != null && muxerStarted && bufferInfo.size > 0) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                    }
                    encoder.releaseOutputBuffer(outIndex, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                } else if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    break
                }
            }

            onProgress(0.90f, "Finalizing audio and muxing tracks...")

            // Cleanup encoder and surface
            try {
                encoder.stop()
                encoder.release()
                inputSurface.release()
                lastExtractedFrame?.recycle()
                retriever.release()
            } catch (_: Exception) {}

            if (muxerStarted) {
                try {
                    muxer.stop()
                    muxer.release()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            onProgress(1.0f, "Export completed successfully!")
            ExportResult(true, outputMp4File)
        } catch (e: Exception) {
            e.printStackTrace()
            ExportResult(false, null, e.localizedMessage ?: "Unknown export error occurred.")
        }
    }
}
