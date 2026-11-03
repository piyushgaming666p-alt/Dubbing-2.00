package com.example.ui.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.window.Dialog
import com.example.data.model.Project
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceCard
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProjectsListDialog(
    currentProject: Project,
    savedProjects: List<Project>,
    onSaveProject: () -> Unit,
    onLoadProject: (Project) -> Unit,
    onCreateNewProject: (name: String) -> Unit,
    onDeleteProject: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newProjectName by remember { mutableStateOf("") }
    var isCreatingNew by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = StudioSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .testTag("projects_dialog")
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Studio Projects",
                            color = StudioTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = StudioTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Current Project Status Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = StudioSurfaceCard),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Current Project", color = StudioTextMuted, fontSize = 11.sp)
                            Text(currentProject.name, color = StudioTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${currentProject.clips.size} clips • ${currentProject.subtitles.size} subtitles",
                                color = StudioTextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        Button(
                            onClick = {
                                onSaveProject()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // New Project Input or Button
                if (isCreatingNew) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = newProjectName,
                            onValueChange = { newProjectName = it },
                            placeholder = { Text("Project title (e.g. Travel Vlog Hindi)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = StudioTextPrimary,
                                unfocusedTextColor = StudioTextPrimary,
                                focusedBorderColor = PurplePrimary,
                                unfocusedBorderColor = StudioBorder
                            ),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val name = newProjectName.ifBlank { "New Studio Project" }
                                    onCreateNewProject(name)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Create & Open")
                            }
                            Button(
                                onClick = { isCreatingNew = false },
                                colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceCard),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel", color = StudioTextSecondary)
                            }
                        }
                    }
                } else {
                    Button(
                        onClick = { isCreatingNew = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B2244)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = PurplePrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create New Blank Project", color = PurplePrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Saved Projects in Storage", color = StudioTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))

                // Saved Projects List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (savedProjects.isEmpty()) {
                        item {
                            Text(
                                text = "No other saved projects yet.",
                                color = StudioTextMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                    } else {
                        items(savedProjects, key = { it.id }) { proj ->
                            SavedProjectItemCard(
                                project = proj,
                                isCurrent = proj.id == currentProject.id,
                                onOpen = { onLoadProject(proj) },
                                onDelete = { onDeleteProject(proj.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SavedProjectItemCard(
    project: Project,
    isCurrent: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(project.lastModifiedAt) {
        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(project.lastModifiedAt))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) Color(0xFF261D3A) else StudioSurfaceCard
        ),
        shape = RoundedCornerShape(10.dp),
        border = if (isCurrent) androidx.compose.foundation.BorderStroke(1.5.dp, PurplePrimary) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Default.VideoLibrary,
                    contentDescription = null,
                    tint = if (isCurrent) PurplePrimary else StudioTextSecondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = project.name,
                        color = StudioTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = "$dateStr • ${project.clips.size} clips",
                        color = StudioTextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StudioTextMuted, modifier = Modifier.size(16.dp))
            }
        }
    }
}
