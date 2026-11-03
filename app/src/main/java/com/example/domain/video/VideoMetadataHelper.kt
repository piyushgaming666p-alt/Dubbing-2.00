package com.example.domain.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import com.example.data.model.VideoClip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.UUID

class VideoMetadataHelper(private val context: Context) {

    /**
     * Copies a user-picked content URI to the app's internal cache directory.
     * Prevents URI permission expiration and guarantees fast local file access.
     */
    suspend fun importVideoFromUri(uri: Uri, fileName: String? = null): VideoClip? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val importDir = File(context.cacheDir, "imported_videos").apply { mkdirs() }
        val name = fileName ?: "clip_${System.currentTimeMillis()}.mp4"
        val destinationFile = File(importDir, name)

        try {
            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null

            extractVideoMetadata(destinationFile, uri.toString(), name)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Extracts duration, resolution, and rotation using MediaMetadataRetriever.
     */
    fun extractVideoMetadata(file: File, originalUri: String, displayName: String): VideoClip {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)

            val durationMs = durationStr?.toLongOrNull() ?: 5000L
            val width = widthStr?.toIntOrNull() ?: 1280
            val height = heightStr?.toIntOrNull() ?: 720
            val rotation = rotationStr?.toIntOrNull() ?: 0

            VideoClip(
                id = UUID.randomUUID().toString(),
                name = displayName,
                uriString = originalUri,
                localFilePath = file.absolutePath,
                durationMs = durationMs,
                trimStartMs = 0L,
                trimEndMs = durationMs,
                width = width,
                height = height,
                rotationDegrees = rotation
            )
        } catch (_: Exception) {
            VideoClip(
                id = UUID.randomUUID().toString(),
                name = displayName,
                uriString = originalUri,
                localFilePath = file.absolutePath,
                durationMs = 8000L,
                trimStartMs = 0L,
                trimEndMs = 8000L
            )
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    /**
     * Retrieves a thumbnail frame bitmap at specified timestamp in ms.
     */
    suspend fun getFrameThumbnail(filePath: String, timeMs: Long): Bitmap? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(filePath)
            retriever.getFrameAtTime(
                timeMs * 1000L,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )
        } catch (_: Exception) {
            null
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    /**
     * Generates a sample MP4 video on device so user can immediately test
     * video editing, timeline trimming, subtitles, and Hindi dubbing.
     */
    suspend fun generateSampleStudioVideo(title: String = "Bhasha Studio Sample"): VideoClip = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "sample_videos").apply { mkdirs() }
        val sampleFile = File(dir, "sample_${System.currentTimeMillis()}.mp4")

        val width = 720
        val height = 1280
        val fps = 30
        val durationSeconds = 12
        val totalFrames = fps * durationSeconds
        val bitRate = 2_000_000

        try {
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val inputSurface = codec.createInputSurface()
            codec.start()

            val muxer = MediaMuxer(sampleFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var trackIndex = -1
            var muxerStarted = false
            val bufferInfo = MediaCodec.BufferInfo()

            val paint = Paint().apply { isAntiAlias = true }
            val textPaint = Paint().apply {
                isAntiAlias = true
                color = Color.WHITE
                textSize = 48f
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
            }
            val subTextPaint = Paint().apply {
                isAntiAlias = true
                color = Color.LTGRAY
                textSize = 32f
                textAlign = Paint.Align.CENTER
            }

            for (frame in 0 until totalFrames) {
                val canvas: Canvas = inputSurface.lockCanvas(null)
                val progress = frame.toFloat() / totalFrames

                // Gradient background: Deep Purple to Dark Violet
                val r = (15 + 40 * progress).toInt()
                val g = (12 + 15 * progress).toInt()
                val b = (35 + 80 * progress).toInt()
                canvas.drawColor(Color.rgb(r, g, b))

                // Animated glowing play circle
                paint.color = Color.argb(120, 168, 85, 247)
                canvas.drawCircle(width / 2f, height / 2f - 60f, 160f + 20f * kotlin.math.sin(progress * 10f), paint)
                paint.color = Color.rgb(168, 85, 247)
                canvas.drawCircle(width / 2f, height / 2f - 60f, 130f, paint)

                // White Play Triangle
                paint.color = Color.WHITE
                val path = android.graphics.Path().apply {
                    moveTo(width / 2f - 30f, height / 2f - 110f)
                    lineTo(width / 2f + 50f, height / 2f - 60f)
                    lineTo(width / 2f - 30f, height / 2f - 10f)
                    close()
                }
                canvas.drawPath(path, paint)

                // Captions on sample video
                val currentSec = frame / fps
                canvas.drawText("Bhasha Studio ✨", width / 2f, height / 2f + 160f, textPaint)
                val statusText = when (currentSec) {
                    in 0..3 -> "1. Dialogue Speech (English)"
                    in 4..7 -> "2. AI Auto Subtitles (English)"
                    in 8..10 -> "3. Hindi Voice Dubbing (हिन्दी)"
                    else -> "4. Export Ready!"
                }
                canvas.drawText(statusText, width / 2f, height / 2f + 230f, subTextPaint)
                canvas.drawText("Time: 00:${String.format("%02d", currentSec)}", width / 2f, height - 120f, subTextPaint)

                inputSurface.unlockCanvasAndPost(canvas)

                // Drain encoder
                while (true) {
                    val outIndex = codec.dequeueOutputBuffer(bufferInfo, 0)
                    if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        trackIndex = muxer.addTrack(codec.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    } else if (outIndex >= 0) {
                        val encodedData = codec.getOutputBuffer(outIndex)
                        if (encodedData != null && muxerStarted && bufferInfo.size > 0) {
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                        }
                        codec.releaseOutputBuffer(outIndex, false)
                    } else {
                        break
                    }
                }
            }

            codec.signalEndOfInputStream()
            while (true) {
                val outIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
                if (outIndex >= 0) {
                    val encodedData = codec.getOutputBuffer(outIndex)
                    if (encodedData != null && muxerStarted && bufferInfo.size > 0) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                    }
                    codec.releaseOutputBuffer(outIndex, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                } else if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    break
                }
            }

            codec.stop()
            codec.release()
            inputSurface.release()
            if (muxerStarted) {
                muxer.stop()
                muxer.release()
            }

            VideoClip(
                id = UUID.randomUUID().toString(),
                name = title,
                uriString = sampleFile.toURI().toString(),
                localFilePath = sampleFile.absolutePath,
                durationMs = durationSeconds * 1000L,
                trimStartMs = 0L,
                trimEndMs = durationSeconds * 1000L,
                width = width,
                height = height
            )
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback placeholder
            VideoClip(
                id = UUID.randomUUID().toString(),
                name = title,
                uriString = sampleFile.toURI().toString(),
                localFilePath = sampleFile.absolutePath,
                durationMs = 12000L,
                trimStartMs = 0L,
                trimEndMs = 12000L,
                width = width,
                height = height
            )
        }
    }
}
