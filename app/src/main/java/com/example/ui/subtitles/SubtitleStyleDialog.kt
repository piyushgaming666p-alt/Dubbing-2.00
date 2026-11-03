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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.SubtitlePosition
import com.example.data.model.SubtitleStyle
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceCard
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary

@Composable
fun SubtitleStyleDialog(
    currentStyle: SubtitleStyle,
    onSaveStyle: (SubtitleStyle) -> Unit,
    onDismiss: () -> Unit
) {
    var fontSize by remember { mutableFloatStateOf(currentStyle.fontSizeSp) }
    var selectedColorHex by remember { mutableStateOf(currentStyle.textColorHex) }
    var selectedPos by remember { mutableStateOf(currentStyle.verticalPosition) }
    var bgBoxColorHex by remember { mutableStateOf(currentStyle.bgBoxColorHex) }

    val colorPresets = listOf(
        "#FFFF00" to "Yellow",
        "#FFFFFF" to "White",
        "#38BDF8" to "Cyan",
        "#34D399" to "Emerald",
        "#F472B6" to "Pink"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = StudioSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Customize Subtitle Style",
                        color = StudioTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = StudioTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Font Size
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Font Size", color = StudioTextPrimary, fontSize = 13.sp)
                    Text("${fontSize.toInt()} sp", color = PurplePrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = fontSize,
                    onValueChange = { fontSize = it },
                    valueRange = 14f..32f,
                    colors = SliderDefaults.colors(thumbColor = PurplePrimary, activeTrackColor = PurplePrimary)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Text Color Presets
                Text("Text Color", color = StudioTextPrimary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    colorPresets.forEach { (hex, name) ->
                        val isSelected = selectedColorHex.equals(hex, ignoreCase = true)
                        val color = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) PurplePrimary else Color.Gray,
                                    shape = CircleShape
                                )
                                .clickable { selectedColorHex = hex }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Subtitle Position
                Text("On-Screen Position", color = StudioTextPrimary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SubtitlePosition.values().forEach { pos ->
                        val isSelected = selectedPos == pos
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) PurplePrimary else StudioSurfaceCard)
                                .border(1.dp, if (isSelected) Color.White else StudioBorder, RoundedCornerShape(8.dp))
                                .clickable { selectedPos = pos }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = pos.name.lowercase().replaceFirstChar { it.uppercase() },
                                color = if (isSelected) Color.White else StudioTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val newStyle = currentStyle.copy(
                            fontSizeSp = fontSize,
                            textColorHex = selectedColorHex,
                            verticalPosition = selectedPos,
                            bgBoxColorHex = bgBoxColorHex
                        )
                        onSaveStyle(newStyle)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Apply Style", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
