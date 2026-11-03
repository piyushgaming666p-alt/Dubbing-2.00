package com.example.ui.audio

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.AudioTrackItem
import com.example.data.model.AudioTrackType
import com.example.data.model.Project
import com.example.domain.audio.SoundEffectsLibrary
import com.example.domain.audio.SoundPreset
import com.example.ui.theme.AudioBgmColor
import com.example.ui.theme.AudioDubColor
import com.example.ui.theme.AudioVoiceoverColor
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurfaceCard
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.VideoTrackColor

@Composable
fun AudioMixerPanel(
    project: Project,
    sfxLibrary: SoundEffectsLibrary,
    isRecordingVoiceover: Boolean,
    recordedDurationMs: Long,
    recordingAmplitude: Float,
    onSetOriginalVideoVolume: (Float) -> Unit,
    onToggleMuteOriginalVideo: () -> Unit,
    onAddSoundPreset: (SoundPreset) -> Unit,
    onStartVoiceOver: () -> Unit,
    onStopVoiceOver: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onStartVoiceOver()
        } else {
            Toast.makeText(context, "Microphone permission is required for voice-over recording", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioSurfaceVariant)
            .border(1.dp, StudioBorder, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("audio_mixer_panel")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = AudioBgmColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Audio Studio & Sound Mixer",
                        color = StudioTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Independent multi-track volume balance and voice-over",
                        color = StudioTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = StudioTextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Live Voice-over Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = StudioSurfaceCard),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = AudioVoiceoverColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Record Voice-Over",
                            color = StudioTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (isRecordingVoiceover) {
                        val sec = recordedDurationMs / 1000
                        val ms = (recordedDurationMs % 1000) / 100
                        Text(
                            text = String.format("REC 00:%02d.%d", sec, ms),
                            color = Color(0xFFF43F5E),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                AnimatedVisibility(visible = isRecordingVoiceover) {
                    // Amplitude visualizer bar
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF33202A))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = recordingAmplitude.coerceIn(0.05f, 1f))
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFF43F5E))
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        if (isRecordingVoiceover) {
                            onStopVoiceOver()
                        } else {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasPermission) {
                                onStartVoiceOver()
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecordingVoiceover) Color(0xFFEF4444) else AudioVoiceoverColor
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("record_voiceover_button")
                ) {
                    Icon(
                        imageVector = if (isRecordingVoiceover) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = null,
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isRecordingVoiceover) "Stop & Add to Track" else "Tap to Record Voice-Over",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Track Volumes
        Text(
            text = "Track Volume Balances",
            color = StudioTextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Track 1: Original Video Audio
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = StudioSurfaceCard),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(VideoTrackColor))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Original Video Sound", color = StudioTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (project.isOriginalVideoMuted) "MUTED" else "${(project.originalVideoVolume * 100).toInt()}%",
                            color = if (project.isOriginalVideoMuted) Color(0xFFF43F5E) else VideoTrackColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = !project.isOriginalVideoMuted,
                            onCheckedChange = { onToggleMuteOriginalVideo() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = VideoTrackColor
                            )
                        )
                    }
                }
                Slider(
                    value = project.originalVideoVolume,
                    onValueChange = onSetOriginalVideoVolume,
                    valueRange = 0f..2f,
                    enabled = !project.isOriginalVideoMuted,
                    colors = SliderDefaults.colors(thumbColor = VideoTrackColor, activeTrackColor = VideoTrackColor)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Sound Effects & BGM Library
        Text(
            text = "Royalty-Free BGM & Sound FX Library",
            color = StudioTextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(sfxLibrary.presets) { preset ->
                SoundPresetCard(preset = preset, onAdd = { onAddSoundPreset(preset) })
            }
        }
    }
}

@Composable
fun SoundPresetCard(preset: SoundPreset, onAdd: () -> Unit) {
    Card(
        modifier = Modifier
            .width(150.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onAdd),
        colors = CardDefaults.cardColors(containerColor = StudioSurfaceCard),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = preset.category,
                    color = if (preset.category == "BGM") AudioBgmColor else PurplePrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("${preset.durationMs / 1000}s", color = StudioTextMuted, fontSize = 10.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = preset.title,
                color = StudioTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1E1730))
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Track", color = PurplePrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
