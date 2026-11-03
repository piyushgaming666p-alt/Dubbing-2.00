package com.example.ui.editor.components

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.AspectRatio
import com.example.data.model.Project
import com.example.data.model.SubtitlePosition
import com.example.ui.theme.StudioBorder
import java.io.File

@Composable
fun VideoPlayerView(
    project: Project,
    currentTimeMs: Long,
    isPlaying: Boolean,
    onVideoClick: () -> Unit,
    onPlayerReady: (MediaPlayer) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryClip = project.clips.firstOrNull()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black)
            .testTag("video_player_container"),
        contentAlignment = Alignment.Center
    ) {
        val aspect = project.aspectRatio.ratio

        Box(
            modifier = Modifier
                .aspectRatio(aspect)
                .widthIn(max = 600.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                .background(Color(0xFF0F0C1B))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onVideoClick
                ),
            contentAlignment = Alignment.Center
        ) {
            if (primaryClip != null && File(primaryClip.localFilePath).exists()) {
                TextureVideoSurface(
                    filePath = primaryClip.localFilePath,
                    currentTimeMs = currentTimeMs,
                    isPlaying = isPlaying,
                    volume = if (project.isOriginalVideoMuted) 0f else project.originalVideoVolume,
                    rotationDegrees = primaryClip.rotationDegrees,
                    onPlayerReady = onPlayerReady,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = "Import a video or load sample to start editing",
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Subtitle Live Overlay
            val activeCue = project.subtitles.firstOrNull { cue ->
                currentTimeMs in cue.startMs..cue.endMs
            }

            if (activeCue != null) {
                val style = project.subtitleStyle
                val alignment = when (style.verticalPosition) {
                    SubtitlePosition.TOP -> Alignment.TopCenter
                    SubtitlePosition.CENTER -> Alignment.Center
                    SubtitlePosition.BOTTOM -> Alignment.BottomCenter
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    contentAlignment = alignment
                ) {
                    val textColor = try {
                        Color(android.graphics.Color.parseColor(style.textColorHex))
                    } catch (_: Exception) { Color.Yellow }

                    val bgColor = try {
                        Color(android.graphics.Color.parseColor(style.bgBoxColorHex))
                    } catch (_: Exception) { Color(0x99000000) }

                    Text(
                        text = activeCue.text,
                        color = textColor,
                        fontSize = style.fontSizeSp.sp,
                        fontWeight = if (style.isBold) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color.Black,
                                blurRadius = 8f
                            )
                        ),
                        modifier = Modifier
                            .background(bgColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .testTag("subtitle_overlay_text")
                    )
                }
            }

            // Text Overlays
            project.textOverlays.forEach { overlay ->
                if (currentTimeMs in overlay.startMs..overlay.endMs) {
                    val textColor = try {
                        Color(android.graphics.Color.parseColor(overlay.textColorHex))
                    } catch (_: Exception) { Color.White }
                    val bgColor = try {
                        Color(android.graphics.Color.parseColor(overlay.bgColorHex))
                    } catch (_: Exception) { Color(0x80000000) }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset {
                                IntOffset(
                                    x = ((overlay.posX - 0.5f) * 300).dp.roundToPx(),
                                    y = ((overlay.posY - 0.5f) * 400).dp.roundToPx()
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = overlay.text,
                            color = textColor,
                            fontSize = overlay.fontSizeSp.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(bgColor, RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TextureVideoSurface(
    filePath: String,
    currentTimeMs: Long,
    isPlaying: Boolean,
    volume: Float,
    rotationDegrees: Int,
    onPlayerReady: (MediaPlayer) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mediaPlayer = remember { MediaPlayer() }

    DisposableEffect(filePath) {
        try {
            mediaPlayer.reset()
            mediaPlayer.setDataSource(filePath)
            mediaPlayer.setVolume(volume, volume)
            mediaPlayer.isLooping = true
            mediaPlayer.prepare()
            onPlayerReady(mediaPlayer)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        onDispose {
            try {
                mediaPlayer.stop()
                mediaPlayer.release()
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(volume) {
        try {
            mediaPlayer.setVolume(volume, volume)
        } catch (_: Exception) {}
    }

    AndroidView(
        factory = { ctx ->
            TextureView(ctx).apply {
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
                        try {
                            mediaPlayer.setSurface(Surface(st))
                            mediaPlayer.seekTo(currentTimeMs.toInt())
                            if (isPlaying) mediaPlayer.start()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {}
                    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                        try {
                            mediaPlayer.setSurface(null)
                        } catch (_: Exception) {}
                        return true
                    }
                    override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                }
                rotation = rotationDegrees.toFloat()
            }
        },
        update = { view ->
            view.rotation = rotationDegrees.toFloat()
        },
        modifier = modifier
    )
}
