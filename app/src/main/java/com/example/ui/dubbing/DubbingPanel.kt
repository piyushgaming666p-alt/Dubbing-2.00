package com.example.ui.dubbing

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.data.model.SupportedLanguage
import com.example.domain.ai.DubbingVoice
import com.example.ui.theme.AudioDubColor
import com.example.ui.theme.PurpleGlow
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurpleSecondary
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurfaceCard
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary

@Composable
fun DubbingPanel(
    project: Project,
    availableVoices: List<DubbingVoice>,
    selectedVoice: DubbingVoice?,
    ttsPitch: Float,
    ttsRate: Float,
    isAiBusy: Boolean,
    aiStatusMessage: String,
    onSelectLanguage: (String) -> Unit,
    onSelectVoice: (DubbingVoice) -> Unit,
    onSetPitch: (Float) -> Unit,
    onSetRate: (Float) -> Unit,
    onPreviewVoice: () -> Unit,
    onSynthesizeFullDubbing: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioSurfaceVariant)
            .border(1.dp, StudioBorder, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("dubbing_panel")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.RecordVoiceOver,
                    contentDescription = null,
                    tint = AudioDubColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "AI Hindi Voice Dubbing",
                        color = StudioTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Real Text-to-Speech synthesis synchronized to video",
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

        // Target Dubbing Language Selector
        Text(
            text = "Dubbing Language",
            color = StudioTextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(LanguageCatalog.ALL) { lang ->
                val isSelected = lang.code.equals(project.targetLanguage, ignoreCase = true)
                LanguageChip(
                    language = lang,
                    isSelected = isSelected,
                    onClick = { onSelectLanguage(lang.code) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Voice Character Profiles
        Text(
            text = "Available Voice Profiles",
            color = StudioTextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val voicesToShow = availableVoices.ifEmpty {
                listOf(
                    DubbingVoice("hi_female", "Studio Aarti (Female)", "hi", "Female"),
                    DubbingVoice("hi_male", "Studio Rohan (Male)", "hi", "Male"),
                    DubbingVoice("hi_host", "Natural Host", "hi", "Neutral")
                )
            }

            voicesToShow.take(3).forEach { voice ->
                val isSelected = selectedVoice?.id == voice.id
                VoiceOptionCard(
                    voice = voice,
                    isSelected = isSelected,
                    onClick = { onSelectVoice(voice) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pitch & Speed Sliders
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = StudioSurfaceCard),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Pitch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Voice Pitch", color = StudioTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    Text("${String.format("%.1f", ttsPitch)}x", color = PurpleGlow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = ttsPitch,
                    onValueChange = onSetPitch,
                    valueRange = 0.5f..1.8f,
                    colors = SliderDefaults.colors(thumbColor = PurplePrimary, activeTrackColor = PurplePrimary)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Rate
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = AudioDubColor, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Speech Rate (Speed)", color = StudioTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    Text("${String.format("%.1f", ttsRate)}x", color = AudioDubColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = ttsRate,
                    onValueChange = onSetRate,
                    valueRange = 0.7f..1.6f,
                    colors = SliderDefaults.colors(thumbColor = AudioDubColor, activeTrackColor = AudioDubColor)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons: Preview Voice & Synthesize Full Dubbing
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onPreviewVoice,
                colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceCard),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("preview_voice_button")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = StudioTextPrimary)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Audition Voice", color = StudioTextPrimary, fontSize = 13.sp)
            }

            Button(
                onClick = onSynthesizeFullDubbing,
                enabled = !isAiBusy && project.subtitles.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = AudioDubColor),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1.3f)
                    .height(48.dp)
                    .testTag("synthesize_dubbing_button")
            ) {
                if (isAiBusy) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Synthesizing...", color = Color.Black, fontSize = 13.sp)
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Generate Dub Track", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        if (isAiBusy && aiStatusMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = aiStatusMessage,
                color = PurpleSecondary,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun LanguageChip(
    language: SupportedLanguage,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) PurplePrimary else StudioSurfaceCard)
            .border(
                1.dp,
                if (isSelected) Color.White else StudioBorder,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(language.flagEmoji, fontSize = 13.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = language.name,
                    color = if (isSelected) Color.White else StudioTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = language.nativeName,
                    color = if (isSelected) Color.White.copy(alpha = 0.85f) else StudioTextSecondary,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun VoiceOptionCard(
    voice: DubbingVoice,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) Color(0xFF1E2F27) else StudioSurfaceCard)
            .border(
                1.5.dp,
                if (isSelected) AudioDubColor else StudioBorder,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = null,
                tint = if (isSelected) AudioDubColor else StudioTextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = voice.gender,
                color = if (isSelected) AudioDubColor else StudioTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = voice.name.split("-").lastOrNull()?.trim() ?: "Studio",
                color = StudioTextMuted,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}
