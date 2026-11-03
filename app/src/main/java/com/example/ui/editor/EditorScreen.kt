package com.example.ui.editor

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.AspectRatio
import com.example.data.model.SubtitleStyle
import com.example.data.model.VideoClip
import com.example.ui.audio.AudioMixerPanel
import com.example.ui.dubbing.DubbingPanel
import com.example.ui.editor.components.TimelineView
import com.example.ui.editor.components.VideoPlayerView
import com.example.ui.export.ExportDialog
import com.example.ui.projects.ProjectsListDialog
import com.example.ui.subtitles.SrtExportDialog
import com.example.ui.subtitles.SubtitleStudioPanel
import com.example.ui.subtitles.SubtitleStyleDialog
import com.example.ui.theme.AudioDubColor
import com.example.ui.theme.PlayheadColor
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurpleSecondary
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceCard
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.SubtitleTrackColor
import com.example.ui.theme.VideoTrackColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: VideoEditorViewModel = viewModel()
) {
    val context = LocalContext.current
    val project by viewModel.project.collectAsState()
    val savedProjects by viewModel.savedProjects.collectAsState()
    val currentTimeMs by viewModel.currentTimeMs.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()
    val activePanel by viewModel.activePanel.collectAsState()
    val selectedSubtitleId by viewModel.selectedSubtitleId.collectAsState()
    val selectedClipId by viewModel.selectedClipId.collectAsState()

    val isAiBusy by viewModel.isAiBusy.collectAsState()
    val aiStatusMessage by viewModel.aiStatusMessage.collectAsState()
    val availableVoices by viewModel.ttsService.availableVoices.collectAsState()
    val selectedVoice by viewModel.selectedVoice.collectAsState()
    val ttsPitch by viewModel.ttsPitch.collectAsState()
    val ttsRate by viewModel.ttsRate.collectAsState()

    val isExporting by viewModel.isExporting.collectAsState()
    val exportProgress by viewModel.exportProgress.collectAsState()
    val exportStatusMessage by viewModel.exportStatusMessage.collectAsState()
    val lastExportResult by viewModel.lastExportResult.collectAsState()

    val isRecordingVoiceover by viewModel.voiceRecorder.isRecording.collectAsState()
    val recordedDurationMs by viewModel.voiceRecorder.recordedDurationMs.collectAsState()
    val recordingAmplitude by viewModel.voiceRecorder.currentAmplitude.collectAsState()

    // Dialog flags
    var showSubtitleStyleDialog by remember { mutableStateOf(false) }
    var showSrtDialog by remember { mutableStateOf(false) }
    var showTrimDialog by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showTextOverlayDialog by remember { mutableStateOf(false) }
    var showAspectMenu by remember { mutableStateOf(false) }

    // System Video File Picker
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importVideo(uri, "User Video ${System.currentTimeMillis()}")
            Toast.makeText(context, "Importing video to studio...", Toast.LENGTH_SHORT).show()
        }
    }

    // Handle Back Press to close open panels
    BackHandler(enabled = activePanel != ActivePanel.NONE) {
        viewModel.closePanel()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PurplePrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("B", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Bhasha Studio",
                                color = StudioTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = project.name,
                                color = StudioTextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                },
                actions = {
                    // Aspect ratio quick pill
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(StudioSurfaceCard)
                                .border(1.dp, StudioBorder, RoundedCornerShape(6.dp))
                                .clickable { showAspectMenu = true }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AspectRatio, contentDescription = null, tint = PurpleSecondary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(project.aspectRatio.label.split(" ").first(), color = StudioTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        DropdownMenu(
                            expanded = showAspectMenu,
                            onDismissRequest = { showAspectMenu = false },
                            modifier = Modifier.background(StudioSurfaceCard)
                        ) {
                            AspectRatio.values().forEach { ratio ->
                                DropdownMenuItem(
                                    text = { Text(ratio.label, color = StudioTextPrimary, fontSize = 12.sp) },
                                    onClick = {
                                        viewModel.setAspectRatio(ratio)
                                        showAspectMenu = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Projects list
                    IconButton(
                        onClick = { viewModel.openPanel(ActivePanel.PROJECTS) },
                        modifier = Modifier.testTag("projects_button")
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = "Projects", tint = StudioTextSecondary)
                    }

                    // Export button
                    Button(
                        onClick = { viewModel.openPanel(ActivePanel.EXPORT) },
                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("export_button")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = StudioBackground)
            )
        },
        containerColor = StudioBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Video Player Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                VideoPlayerView(
                    project = project,
                    currentTimeMs = currentTimeMs,
                    isPlaying = isPlaying,
                    onVideoClick = { viewModel.togglePlayPause() },
                    onPlayerReady = { player ->
                        viewModel.mediaPlayer = player
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 2. Playback Controller Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StudioSurface)
                    .border(1.dp, StudioBorder)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time Display: 00:02.15 / 00:12.00
                Text(
                    text = "${formatTime(currentTimeMs)} / ${formatTime(project.totalDurationMs)}",
                    color = StudioTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                // Play / Pause button
                IconButton(
                    onClick = { viewModel.togglePlayPause() },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(PurplePrimary)
                        .testTag("play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Playback speed pill
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(StudioSurfaceCard)
                            .clickable { showSpeedMenu = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = StudioTextSecondary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${playbackSpeed}x", color = StudioTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    DropdownMenu(
                        expanded = showSpeedMenu,
                        onDismissRequest = { showSpeedMenu = false },
                        modifier = Modifier.background(StudioSurfaceCard)
                    ) {
                        listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                            DropdownMenuItem(
                                text = { Text("${speed}x", color = StudioTextPrimary) },
                                onClick = {
                                    viewModel.setSpeed(speed)
                                    showSpeedMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // 3. Multi-Track Timeline
            TimelineView(
                project = project,
                currentTimeMs = currentTimeMs,
                selectedClipId = selectedClipId,
                selectedSubtitleId = selectedSubtitleId,
                onSeek = { seekMs -> viewModel.seekTo(seekMs) },
                onSelectClip = { clipId ->
                    viewModel.selectClip(clipId)
                },
                onSelectSubtitle = { subId ->
                    viewModel.selectSubtitle(subId)
                    viewModel.openPanel(ActivePanel.SUBTITLES)
                }
            )

            // 4. CapCut Pro Quick Action Bar
            QuickActionToolbar(
                onImportVideo = { videoPickerLauncher.launch("video/*") },
                onTrim = {
                    if (project.clips.isNotEmpty()) showTrimDialog = true
                    else Toast.makeText(context, "Import a video clip first", Toast.LENGTH_SHORT).show()
                },
                onSplit = {
                    if (project.clips.isNotEmpty()) viewModel.splitClipAtPlayhead()
                    else Toast.makeText(context, "Import a video clip first", Toast.LENGTH_SHORT).show()
                },
                onRotate = { viewModel.rotateSelectedClip() },
                onSubtitles = { viewModel.openPanel(ActivePanel.SUBTITLES) },
                onDubbing = { viewModel.openPanel(ActivePanel.DUBBING) },
                onAudioMixer = { viewModel.openPanel(ActivePanel.AUDIO_MIXER) },
                onTextOverlay = { showTextOverlayDialog = true },
                onDelete = { viewModel.deleteSelectedClip() }
            )

            // 5. Active Tool Bottom Panels
            AnimatedVisibility(
                visible = activePanel != ActivePanel.NONE,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                when (activePanel) {
                    ActivePanel.DUBBING -> {
                        DubbingPanel(
                            project = project,
                            availableVoices = availableVoices,
                            selectedVoice = selectedVoice,
                            ttsPitch = ttsPitch,
                            ttsRate = ttsRate,
                            isAiBusy = isAiBusy,
                            aiStatusMessage = aiStatusMessage,
                            onSelectLanguage = { langCode ->
                                viewModel.translateSubtitles(langCode)
                            },
                            onSelectVoice = { voice -> viewModel.selectVoice(voice) },
                            onSetPitch = { pitch -> viewModel.setPitch(pitch) },
                            onSetRate = { rate -> viewModel.setSpeechRate(rate) },
                            onPreviewVoice = {
                                val firstCue = project.subtitles.firstOrNull()
                                if (firstCue != null) {
                                    viewModel.previewVoiceForCue(firstCue)
                                } else {
                                    viewModel.ttsService.previewSpeech(
                                        "नमस्ते, भाषा स्टूडियो में आपका स्वागत है!",
                                        com.example.data.model.LanguageCatalog.fromCode(project.targetLanguage)
                                    )
                                }
                            },
                            onSynthesizeFullDubbing = { viewModel.synthesizeFullDubbingTrack() },
                            onClose = { viewModel.closePanel() }
                        )
                    }

                    ActivePanel.SUBTITLES -> {
                        SubtitleStudioPanel(
                            project = project,
                            selectedSubtitleId = selectedSubtitleId,
                            isAiBusy = isAiBusy,
                            aiStatusMessage = aiStatusMessage,
                            onAutoTranscribe = { viewModel.autoTranscribeSpeech() },
                            onTranslate = { targetLang -> viewModel.translateSubtitles(targetLang) },
                            onUpdateCue = { id, text, start, end ->
                                viewModel.updateSubtitleCue(id, text, start, end)
                            },
                            onAddCue = { viewModel.addManualSubtitleCue() },
                            onDeleteCue = { id -> viewModel.deleteSubtitleCue(id) },
                            onOpenStyleDialog = { showSubtitleStyleDialog = true },
                            onOpenSrtDialog = { showSrtDialog = true },
                            onClose = { viewModel.closePanel() }
                        )
                    }

                    ActivePanel.AUDIO_MIXER -> {
                        AudioMixerPanel(
                            project = project,
                            sfxLibrary = viewModel.sfxLibrary,
                            isRecordingVoiceover = isRecordingVoiceover,
                            recordedDurationMs = recordedDurationMs,
                            recordingAmplitude = recordingAmplitude,
                            onSetOriginalVideoVolume = { vol -> viewModel.setOriginalVideoVolume(vol) },
                            onToggleMuteOriginalVideo = { viewModel.toggleMuteOriginalVideo() },
                            onAddSoundPreset = { preset -> viewModel.addSoundPreset(preset) },
                            onStartVoiceOver = { viewModel.startVoiceOverRecording() },
                            onStopVoiceOver = { viewModel.stopVoiceOverRecording() },
                            onClose = { viewModel.closePanel() }
                        )
                    }

                    ActivePanel.EXPORT -> {
                        ExportDialog(
                            project = project,
                            isExporting = isExporting,
                            exportProgress = exportProgress,
                            exportStatusMessage = exportStatusMessage,
                            lastExportResult = lastExportResult,
                            onStartExport = { config -> viewModel.startExport(config) },
                            onDismissResult = { viewModel.dismissExportResult() },
                            onDismiss = { viewModel.closePanel() }
                        )
                    }

                    ActivePanel.PROJECTS -> {
                        ProjectsListDialog(
                            currentProject = project,
                            savedProjects = savedProjects,
                            onSaveProject = { viewModel.saveCurrentProject() },
                            onLoadProject = { proj -> viewModel.loadProject(proj) },
                            onCreateNewProject = { name -> viewModel.createNewProject(name) },
                            onDeleteProject = { id -> viewModel.deleteProject(id) },
                            onDismiss = { viewModel.closePanel() }
                        )
                    }

                    else -> {}
                }
            }
        }
    }

    // Subtitle Custom Style Dialog
    if (showSubtitleStyleDialog) {
        SubtitleStyleDialog(
            currentStyle = project.subtitleStyle,
            onSaveStyle = { newStyle -> viewModel.updateSubtitleStyle(newStyle) },
            onDismiss = { showSubtitleStyleDialog = false }
        )
    }

    // SRT Import / Export Dialog
    if (showSrtDialog) {
        SrtExportDialog(
            srtContent = viewModel.getExportedSrt(),
            onImportSrt = { srt -> viewModel.importSrtFile(srt) },
            onDismiss = { showSrtDialog = false }
        )
    }

    // Trim Dialog for selected clip
    if (showTrimDialog) {
        val clip = project.clips.firstOrNull { it.id == selectedClipId } ?: project.clips.firstOrNull()
        if (clip != null) {
            TrimClipDialog(
                clip = clip,
                onApplyTrim = { start, end ->
                    viewModel.trimSelectedClip(start, end)
                    showTrimDialog = false
                },
                onDismiss = { showTrimDialog = false }
            )
        }
    }

    // Text Overlay Add Dialog
    if (showTextOverlayDialog) {
        AddTextOverlayDialog(
            onAdd = { text ->
                viewModel.addTextOverlay(text)
                showTextOverlayDialog = false
            },
            onDismiss = { showTextOverlayDialog = false }
        )
    }
}

@Composable
fun QuickActionToolbar(
    onImportVideo: () -> Unit,
    onTrim: () -> Unit,
    onSplit: () -> Unit,
    onRotate: () -> Unit,
    onSubtitles: () -> Unit,
    onDubbing: () -> Unit,
    onAudioMixer: () -> Unit,
    onTextOverlay: () -> Unit,
    onDelete: () -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(StudioSurfaceVariant)
            .border(1.dp, StudioBorder)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            ToolButton(
                icon = Icons.Default.VideoFile,
                label = "Import",
                tint = PurplePrimary,
                onClick = onImportVideo,
                testTag = "toolbar_import_video"
            )
        }
        item {
            ToolButton(
                icon = Icons.Default.ContentCut,
                label = "Trim",
                tint = VideoTrackColor,
                onClick = onTrim,
                testTag = "toolbar_trim_clip"
            )
        }
        item {
            ToolButton(
                icon = Icons.Default.CallSplit,
                label = "Split",
                tint = VideoTrackColor,
                onClick = onSplit,
                testTag = "toolbar_split_clip"
            )
        }
        item {
            ToolButton(
                icon = Icons.Default.Subtitles,
                label = "Subtitles",
                tint = SubtitleTrackColor,
                onClick = onSubtitles,
                testTag = "toolbar_subtitles"
            )
        }
        item {
            ToolButton(
                icon = Icons.Default.RecordVoiceOver,
                label = "AI Dubbing",
                tint = AudioDubColor,
                onClick = onDubbing,
                testTag = "toolbar_ai_dubbing"
            )
        }
        item {
            ToolButton(
                icon = Icons.Default.GraphicEq,
                label = "Audio Mixer",
                tint = Color(0xFF06B6D4),
                onClick = onAudioMixer,
                testTag = "toolbar_audio_mixer"
            )
        }
        item {
            ToolButton(
                icon = Icons.Default.RotateRight,
                label = "Rotate",
                tint = StudioTextSecondary,
                onClick = onRotate,
                testTag = "toolbar_rotate"
            )
        }
        item {
            ToolButton(
                icon = Icons.Default.TextFields,
                label = "Text",
                tint = StudioTextSecondary,
                onClick = onTextOverlay,
                testTag = "toolbar_text"
            )
        }
        item {
            ToolButton(
                icon = Icons.Default.Delete,
                label = "Delete",
                tint = Color(0xFFEF4444),
                onClick = onDelete,
                testTag = "toolbar_delete"
            )
        }
    }
}

@Composable
fun ToolButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = label, color = StudioTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun TrimClipDialog(
    clip: VideoClip,
    onApplyTrim: (startMs: Long, endMs: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var rangeStart by remember { mutableFloatStateOf(clip.trimStartMs.toFloat()) }
    var rangeEnd by remember { mutableFloatStateOf(clip.trimEndMs.toFloat()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = StudioSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Trim Video Clip",
                    color = StudioTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = clip.name,
                    color = StudioTextSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Start: ${formatTime(rangeStart.toLong())}", color = VideoTrackColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("End: ${formatTime(rangeEnd.toLong())}", color = VideoTrackColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                @OptIn(ExperimentalMaterial3Api::class)
                RangeSlider(
                    value = rangeStart..rangeEnd,
                    onValueChange = { range ->
                        rangeStart = range.start
                        rangeEnd = range.endInclusive
                    },
                    valueRange = 0f..clip.durationMs.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = VideoTrackColor,
                        activeTrackColor = VideoTrackColor
                    )
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onApplyTrim(rangeStart.toLong(), rangeEnd.toLong()) },
                        colors = ButtonDefaults.buttonColors(containerColor = VideoTrackColor),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Apply Trim")
                    }
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceCard),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = StudioTextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun AddTextOverlayDialog(
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = StudioSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("Add Title / Text Overlay", color = StudioTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Enter title or sticker text...") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = StudioTextPrimary,
                        unfocusedTextColor = StudioTextPrimary,
                        focusedBorderColor = PurplePrimary,
                        unfocusedBorderColor = StudioBorder
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (text.isNotBlank()) onAdd(text)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Add to Video")
                    }
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceCard),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = StudioTextSecondary)
                    }
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val sec = millis / 1000
    val ms = (millis % 1000) / 10
    val min = sec / 60
    val remSec = sec % 60
    return String.format("%02d:%02d.%02d", min, remSec, ms)
}
