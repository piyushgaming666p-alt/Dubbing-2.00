package com.example.ui.export

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.example.data.model.AspectRatio
import com.example.data.model.ExportConfig
import com.example.data.model.ExportFrameRate
import com.example.data.model.ExportResolution
import com.example.data.model.Project
import com.example.domain.video.ExportResult
import com.example.ui.theme.AudioDubColor
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceCard
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary

@Composable
fun ExportDialog(
    project: Project,
    isExporting: Boolean,
    exportProgress: Float,
    exportStatusMessage: String,
    lastExportResult: ExportResult?,
    onStartExport: (ExportConfig) -> Unit,
    onDismissResult: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var selectedResolution by remember { mutableStateOf(ExportResolution.RES_720P) }
    var selectedFps by remember { mutableStateOf(ExportFrameRate.FPS_30) }
    var selectedAspect by remember { mutableStateOf(project.aspectRatio) }
    var burnSubtitles by remember { mutableStateOf(true) }
    var includeDubbing by remember { mutableStateOf(true) }
    var includeBgm by remember { mutableStateOf(true) }
    var muteOriginalVideo by remember { mutableStateOf(project.isOriginalVideoMuted) }

    Dialog(onDismissRequest = { if (!isExporting) onDismiss() }) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = StudioSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
                    .testTag("export_dialog")
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Export MP4 Video",
                        color = StudioTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (!isExporting) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = StudioTextSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // If result is ready, show outcome
                if (lastExportResult != null) {
                    if (lastExportResult.isSuccess && lastExportResult.outputFile != null) {
                        ExportSuccessView(
                            file = lastExportResult.outputFile,
                            onClose = {
                                onDismissResult()
                                onDismiss()
                            }
                        )
                    } else {
                        ExportErrorView(
                            error = lastExportResult.error ?: "Failed to export video",
                            onRetry = onDismissResult
                        )
                    }
                } else if (isExporting) {
                    // Exporting progress view
                    ExportProgressView(
                        progress = exportProgress,
                        statusMessage = exportStatusMessage
                    )
                } else {
                    // Settings View
                    // Resolution Selection
                    Text("Resolution Presets", color = StudioTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExportResolution.values().forEach { res ->
                            val isSelected = selectedResolution == res
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) PurplePrimary else StudioSurfaceCard)
                                    .border(1.dp, if (isSelected) Color.White else StudioBorder, RoundedCornerShape(8.dp))
                                    .clickable { selectedResolution = res }
                                    .padding(vertical = 10.dp, horizontal = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (res == ExportResolution.RES_720P) "720p HD" else "1080p FHD",
                                        color = if (isSelected) Color.White else StudioTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (res == ExportResolution.RES_720P) "Low RAM Safe" else "Crisp Studio",
                                        color = if (isSelected) Color.White.copy(alpha = 0.8f) else StudioTextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Frame Rate Selection
                    Text("Frame Rate", color = StudioTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExportFrameRate.values().forEach { rate ->
                            val isSelected = selectedFps == rate
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) PurplePrimary else StudioSurfaceCard)
                                    .border(1.dp, if (isSelected) Color.White else StudioBorder, RoundedCornerShape(8.dp))
                                    .clickable { selectedFps = rate }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${rate.fps} FPS",
                                    color = if (isSelected) Color.White else StudioTextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Export switches
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = StudioSurfaceCard),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            ExportToggleRow(
                                title = "Burn-in Multilingual Subtitles",
                                subtitle = "Renders styled subtitles directly into the video frames",
                                checked = burnSubtitles,
                                onCheckedChange = { burnSubtitles = it }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            ExportToggleRow(
                                title = "Include Hindi AI Dubbing",
                                subtitle = "Muxes synthesized Hindi voice track",
                                checked = includeDubbing,
                                onCheckedChange = { includeDubbing = it }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            ExportToggleRow(
                                title = "Include Background Music & SFX",
                                subtitle = "Mixes BGM and sound effects",
                                checked = includeBgm,
                                onCheckedChange = { includeBgm = it }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            val config = ExportConfig(
                                resolution = selectedResolution,
                                frameRate = selectedFps,
                                aspectRatio = selectedAspect,
                                burnSubtitles = burnSubtitles,
                                includeDubbing = includeDubbing,
                                includeBgm = includeBgm,
                                muteOriginalVideo = muteOriginalVideo
                            )
                            onStartExport(config)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("confirm_export_button")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export MP4 Video", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ExportToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = StudioTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = StudioTextMuted, fontSize = 10.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PurplePrimary
            )
        )
    }
}

@Composable
fun ExportProgressView(
    progress: Float,
    statusMessage: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(72.dp),
            color = PurplePrimary,
            strokeWidth = 6.dp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "${(progress * 100).toInt()}%",
            color = StudioTextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = statusMessage,
            color = StudioTextSecondary,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Optimized for 4–6 GB RAM memory pipeline",
            color = StudioTextMuted,
            fontSize = 11.sp
        )
    }
}

@Composable
fun ExportSuccessView(
    file: java.io.File,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = AudioDubColor,
            modifier = Modifier.size(60.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Export Completed!",
            color = StudioTextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = file.name,
            color = StudioTextSecondary,
            fontSize = 12.sp
        )
        Text(
            text = "Size: ${String.format("%.1f", file.length() / (1024.0 * 1024.0))} MB",
            color = StudioTextMuted,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    try {
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                        val playIntent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "video/mp4")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(playIntent)
                    } catch (_: Exception) {
                        Toast.makeText(context, "Saved to ${file.absolutePath}", Toast.LENGTH_LONG).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Play Video")
            }

            Button(
                onClick = {
                    try {
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "video/mp4"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Exported Video"))
                    } catch (_: Exception) {
                        Toast.makeText(context, "Saved to: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceCard),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, tint = StudioTextPrimary)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share", color = StudioTextPrimary)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = onClose,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Done", color = StudioTextSecondary)
        }
    }
}

@Composable
fun ExportErrorView(
    error: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            tint = Color(0xFFEF4444),
            modifier = Modifier.size(54.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text("Export Encountered an Issue", color = StudioTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(error, color = StudioTextSecondary, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("Adjust Settings & Retry")
        }
    }
}
