package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdPlacement
import com.example.data.model.AdPlacementType
import com.example.data.model.AdSettings
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*

@Composable
fun AdManagementScreen(
    adSettings: AdSettings,
    onSaveSettings: (AdSettings) -> Unit,
    onOpenEditPlacement: (String) -> Unit
) {
    var globalEnabled by remember(adSettings) { mutableStateOf(adSettings.globalAdsEnabled) }
    var defaultDurationText by remember(adSettings) { mutableStateOf(adSettings.defaultWatchDurationSeconds.toString()) }
    var networkSnippet by remember(adSettings) { mutableStateOf(adSettings.customNetworkSnippet) }
    var currentPlacements by remember(adSettings) { mutableStateOf(adSettings.placements) }

    val defaultDuration = defaultDurationText.toIntOrNull() ?: adSettings.defaultWatchDurationSeconds

    val hasChanges = globalEnabled != adSettings.globalAdsEnabled ||
            defaultDuration != adSettings.defaultWatchDurationSeconds ||
            networkSnippet != adSettings.customNetworkSnippet ||
            currentPlacements != adSettings.placements

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
    ) {
        item {
            SectionHeader(
                title = "Advertisement Gate Management",
                description = "Configure 10-second sponsor gate requirements before generation submissions"
            )
        }

        // Global Ad Master Switch Card
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
                            Icon(imageVector = Icons.Default.SmartDisplay, contentDescription = null, tint = CyanGlow)
                            Column {
                                Text("Global Advertisement Gate", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Master switch for all generation gates in User App", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Switch(
                            checked = globalEnabled,
                            onCheckedChange = { globalEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CyanGlowDark
                            )
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    // Default Watch Duration
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Required Watch Duration", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Countdown timer enforced before generation unlocks (Default: 10s)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        OutlinedTextField(
                            value = defaultDurationText,
                            onValueChange = { if (it.all { c -> c.isDigit() }) defaultDurationText = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            trailingIcon = { Text("sec", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 8.dp)) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.width(96.dp)
                        )
                    }
                }
            }
        }

        // Ad Placements List
        item {
            SectionHeader(
                title = "Configured Placements",
                description = "Customize copy, sponsor links, and target actions for each gate"
            )
        }

        items(AdPlacementType.values()) { placementType ->
            val placement = currentPlacements[placementType.name] ?: AdPlacement(type = placementType)

            PlacementCard(
                placement = placement,
                onToggleEnabled = { isEnabled ->
                    currentPlacements = currentPlacements.toMutableMap().apply {
                        put(placementType.name, placement.copy(enabled = isEnabled))
                    }
                },
                onEditClick = { onOpenEditPlacement(placementType.name) }
            )
        }

        // Custom Ad Network Script/Provider Snippet
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Code, contentDescription = null, tint = IndigoVibrant)
                        Text("Third-Party Ad Network SDK / Tag Snippet", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "Paste script tags or network unit IDs here when transitioning from direct sponsor creatives to Google AdMob / Unity Ads / AppLovin.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = networkSnippet,
                        onValueChange = { networkSnippet = it },
                        placeholder = { Text("<!-- e.g. <script async src='https://adnetwork.com/tag.js'></script> -->", fontSize = 11.sp) },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Save Button
        item {
            Button(
                onClick = {
                    onSaveSettings(
                        adSettings.copy(
                            globalAdsEnabled = globalEnabled,
                            defaultWatchDurationSeconds = defaultDuration,
                            placements = currentPlacements,
                            customNetworkSnippet = networkSnippet
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
                Text("Save Ad Gate Rules to Firestore", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun PlacementCard(
    placement: AdPlacement,
    onToggleEnabled: (Boolean) -> Unit,
    onEditClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = placement.type.label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = placement.type.description,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = placement.enabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = IndigoVibrant
                    )
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Title: ${placement.title}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Gate: ${placement.watchDurationSeconds}s timer",
                            fontSize = 11.sp,
                            color = WarningAmber
                        )
                    }
                    Text(
                        text = placement.description,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                    Text(
                        text = "CTA: \"${placement.ctaText}\" → ${placement.targetUrl}",
                        fontSize = 10.sp,
                        color = CyanGlow
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onEditClick,
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit Creative & Timer", fontSize = 10.sp)
                }
            }
        }
    }
}
