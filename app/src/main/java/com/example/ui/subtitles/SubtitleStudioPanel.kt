package com.example.ui.subtitles

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LanguageCatalog
import com.example.data.model.Project
import com.example.data.model.SubtitleCue
import com.example.data.model.SubtitleStyle
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioError
import com.example.ui.theme.StudioSurfaceCard
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.SubtitleTrackColor

@Composable
fun SubtitleStudioPanel(
    project: Project,
    selectedSubtitleId: String?,
    isAiBusy: Boolean,
    aiStatusMessage: String,
    onAutoTranscribe: () -> Unit,
    onTranslate: (targetLang: String) -> Unit,
    onUpdateCue: (id: String, text: String, startMs: Long, endMs: Long) -> Unit,
    onAddCue: () -> Unit,
    onDeleteCue: (id: String) -> Unit,
    onOpenStyleDialog: () -> Unit,
    onOpenSrtDialog: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioSurfaceVariant)
            .border(1.dp, StudioBorder, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .padding(16.dp)
            .testTag("subtitle_studio_panel")
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Subtitles,
                    contentDescription = null,
                    tint = SubtitleTrackColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Automatic Subtitles & Translation",
                        color = StudioTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${project.subtitles.size} dialogue cues in timeline",
                        color = StudioTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = StudioTextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // AI Actions Toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onAutoTranscribe,
                enabled = !isAiBusy,
                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .testTag("auto_transcribe_button")
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Auto Transcribe", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = { onTranslate(project.targetLanguage) },
                enabled = !isAiBusy && project.subtitles.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .testTag("translate_subtitles_button")
            ) {
                Icon(Icons.Default.Translate, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Translate (${LanguageCatalog.fromCode(project.targetLanguage).nativeName})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            IconButton(
                onClick = onOpenStyleDialog,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(StudioSurfaceCard)
            ) {
                Icon(Icons.Default.Palette, contentDescription = "Subtitle Style", tint = StudioTextPrimary)
            }

            IconButton(
                onClick = onOpenSrtDialog,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(StudioSurfaceCard)
            ) {
                Icon(Icons.Default.Download, contentDescription = "SRT Import / Export", tint = StudioTextPrimary)
            }
        }

        if (isAiBusy && aiStatusMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(color = PurplePrimary, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(aiStatusMessage, color = PurplePrimary, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Cues List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 280.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(project.subtitles, key = { it.id }) { cue ->
                SubtitleCueItemCard(
                    cue = cue,
                    isSelected = cue.id == selectedSubtitleId,
                    onUpdate = { newText ->
                        onUpdateCue(cue.id, newText, cue.startMs, cue.endMs)
                    },
                    onDelete = { onDeleteCue(cue.id) }
                )
            }

            item {
                OutlinedButton(
                    onClick = onAddCue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = PurplePrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Subtitle Cue at Playhead", color = PurplePrimary, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun SubtitleCueItemCard(
    cue: SubtitleCue,
    isSelected: Boolean,
    onUpdate: (String) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF281E3B) else StudioSurfaceCard
        ),
        shape = RoundedCornerShape(10.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, SubtitleTrackColor) else null
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${formatMs(cue.startMs)} → ${formatMs(cue.endMs)}",
                    color = SubtitleTrackColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StudioTextMuted, modifier = Modifier.size(16.dp))
                }
            }

            var textState by remember(cue.text) { mutableStateOf(cue.text) }

            OutlinedTextField(
                value = textState,
                onValueChange = {
                    textState = it
                    onUpdate(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = StudioTextPrimary,
                    unfocusedTextColor = StudioTextPrimary,
                    focusedBorderColor = PurplePrimary,
                    unfocusedBorderColor = StudioBorder
                ),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                singleLine = false,
                maxLines = 3
            )
        }
    }
}

private fun formatMs(ms: Long): String {
    val sec = ms / 1000
    val remMs = ms % 1000
    return String.format("%02d:%02d.%d", sec / 60, sec % 60, remMs / 100)
}
