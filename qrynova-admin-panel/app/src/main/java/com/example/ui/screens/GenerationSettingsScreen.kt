package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GenerationSettings
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*

@Composable
fun GenerationSettingsScreen(
    generationSettings: GenerationSettings,
    onSaveSettings: (GenerationSettings) -> Unit
) {
    var imageEnabled by remember(generationSettings) { mutableStateOf(generationSettings.image.enabled) }
    var imageMaint by remember(generationSettings) { mutableStateOf(generationSettings.image.maintenanceMode) }
    var imageMaintMsg by remember(generationSettings) { mutableStateOf(generationSettings.image.maintenanceMessage) }

    var videoEnabled by remember(generationSettings) { mutableStateOf(generationSettings.video.enabled) }
    var videoMaint by remember(generationSettings) { mutableStateOf(generationSettings.video.maintenanceMode) }
    var videoMaintMsg by remember(generationSettings) { mutableStateOf(generationSettings.video.maintenanceMessage) }

    var i2vEnabled by remember(generationSettings) { mutableStateOf(generationSettings.imageToVideo.enabled) }
    var i2vMaint by remember(generationSettings) { mutableStateOf(generationSettings.imageToVideo.maintenanceMode) }
    var i2vMaintMsg by remember(generationSettings) { mutableStateOf(generationSettings.imageToVideo.maintenanceMessage) }

    val hasChanges = imageEnabled != generationSettings.image.enabled ||
            imageMaint != generationSettings.image.maintenanceMode ||
            imageMaintMsg != generationSettings.image.maintenanceMessage ||
            videoEnabled != generationSettings.video.enabled ||
            videoMaint != generationSettings.video.maintenanceMode ||
            videoMaintMsg != generationSettings.video.maintenanceMessage ||
            i2vEnabled != generationSettings.imageToVideo.enabled ||
            i2vMaint != generationSettings.imageToVideo.maintenanceMode ||
            i2vMaintMsg != generationSettings.imageToVideo.maintenanceMessage

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
    ) {
        item {
            SectionHeader(
                title = "Generation Pipeline Rules",
                description = "Configure model availability, resolution capabilities, and maintenance lockdowns"
            )
        }

        // Section 1: Image Generation Pipeline
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = CyanGlow)
                            Column {
                                Text("Image Generation Engine", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Text-to-Image prompt synthesis", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Switch(
                            checked = imageEnabled,
                            onCheckedChange = { imageEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = CyanGlowDark)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Supported Resolutions: 1080x1080 (Default), 1920x1080, 1080x1920, 512x512", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("Aspect Ratios: 1:1, 16:9, 9:16, 4:3, 3:4", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Max Prompt Length: ${generationSettings.image.maxPromptLength} chars • Max Output: ${generationSettings.image.maxOutputSizeMb}MB", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Image Maintenance Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Image Pipeline Maintenance Mode", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (imageMaint) WarningAmber else MaterialTheme.colorScheme.onSurface)
                            Text("Locks image generation in User App with custom notice", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = imageMaint,
                            onCheckedChange = { imageMaint = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = WarningAmber)
                        )
                    }

                    if (imageMaint) {
                        OutlinedTextField(
                            value = imageMaintMsg,
                            onValueChange = { imageMaintMsg = it },
                            placeholder = { Text("Enter maintenance message shown to users...") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
        }

        // Section 2: Text-to-Video Pipeline
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Videocam, contentDescription = null, tint = VioletNeon)
                            Column {
                                Text("Text-to-Video Engine", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Cinematic scene rendering", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Switch(
                            checked = videoEnabled,
                            onCheckedChange = { videoEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = VioletNeon)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Available Resolutions: 1080p (1920x1080), 720p (1280x720), 1080x1080", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("Duration Choices: 5s, 10s, 15s (Max: ${generationSettings.video.maxDurationSeconds}s)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Video Maintenance Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Video Pipeline Maintenance Mode", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (videoMaint) WarningAmber else MaterialTheme.colorScheme.onSurface)
                            Text("Temporarily disables video queue for GPU upgrades", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = videoMaint,
                            onCheckedChange = { videoMaint = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = WarningAmber)
                        )
                    }

                    if (videoMaint) {
                        OutlinedTextField(
                            value = videoMaintMsg,
                            onValueChange = { videoMaintMsg = it },
                            placeholder = { Text("Enter video maintenance notice...") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
        }

        // Section 3: Image-to-Video Pipeline
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Animation, contentDescription = null, tint = IndigoVibrant)
                            Column {
                                Text("Image-to-Video Synthesis", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Motion animation from uploaded still images", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Switch(
                            checked = i2vEnabled,
                            onCheckedChange = { i2vEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = IndigoVibrant)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Supported Outputs: 1080p, 720p, Square • Standard Duration: ${generationSettings.imageToVideo.durationSeconds}s", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("Prompt Requirement: Mandatory guiding motion prompt", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // I2V Maintenance
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Image-to-Video Maintenance", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (i2vMaint) WarningAmber else MaterialTheme.colorScheme.onSurface)
                            Text("Locks animation queue in client app", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = i2vMaint,
                            onCheckedChange = { i2vMaint = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = WarningAmber)
                        )
                    }

                    if (i2vMaint) {
                        OutlinedTextField(
                            value = i2vMaintMsg,
                            onValueChange = { i2vMaintMsg = it },
                            placeholder = { Text("Enter notice...") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
        }

        // Save Button
        item {
            Button(
                onClick = {
                    onSaveSettings(
                        generationSettings.copy(
                            image = generationSettings.image.copy(
                                enabled = imageEnabled,
                                maintenanceMode = imageMaint,
                                maintenanceMessage = imageMaintMsg
                            ),
                            video = generationSettings.video.copy(
                                enabled = videoEnabled,
                                maintenanceMode = videoMaint,
                                maintenanceMessage = videoMaintMsg
                            ),
                            imageToVideo = generationSettings.imageToVideo.copy(
                                enabled = i2vEnabled,
                                maintenanceMode = i2vMaint,
                                maintenanceMessage = i2vMaintMsg
                            )
                        )
                    )
                },
                enabled = hasChanges,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanGlowDark,
                    contentColor = Color.White
                )
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Generation Rules to Firestore", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}
