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
import com.example.data.model.AppSystemSettings
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*

@Composable
fun SystemSettingsScreen(
    settings: AppSystemSettings,
    onSaveSettings: (AppSystemSettings) -> Unit
) {
    var appName by remember(settings) { mutableStateOf(settings.appName) }
    var appTagline by remember(settings) { mutableStateOf(settings.appTagline) }
    var logoUrl by remember(settings) { mutableStateOf(settings.appLogoUrl) }
    var supportEmail by remember(settings) { mutableStateOf(settings.publicAdminsContact) }
    var maintenanceMode by remember(settings) { mutableStateOf(settings.maintenanceMode) }
    var maintenanceMsg by remember(settings) { mutableStateOf(settings.maintenanceMessage) }
    var allowRegistration by remember(settings) { mutableStateOf(settings.registrationEnabled) }
    var allowLogin by remember(settings) { mutableStateOf(settings.loginEnabled) }
    var forceUpdateEnabled by remember(settings) { mutableStateOf(settings.forceUpdateRequired) }
    var forceUpdateUrl by remember(settings) { mutableStateOf(settings.forceUpdateUrl) }

    val hasChanges = appName != settings.appName ||
            appTagline != settings.appTagline ||
            logoUrl != settings.appLogoUrl ||
            supportEmail != settings.publicAdminsContact ||
            maintenanceMode != settings.maintenanceMode ||
            maintenanceMsg != settings.maintenanceMessage ||
            allowRegistration != settings.registrationEnabled ||
            allowLogin != settings.loginEnabled ||
            forceUpdateEnabled != settings.forceUpdateRequired ||
            forceUpdateUrl != settings.forceUpdateUrl

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
    ) {
        item {
            SectionHeader(
                title = "Global System Settings",
                description = "Master application identity, access controls, and maintenance switches"
            )
        }

        // Card 1: Branding & Identity
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Palette, contentDescription = null, tint = CyanGlow)
                        Text("App Identity & Branding", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    // App Name
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Application Name", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = appName,
                            onValueChange = { appName = it },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // App Tagline
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Tagline / Subheading", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = appTagline,
                            onValueChange = { appTagline = it },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Logo URL
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Logo Asset URL", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = logoUrl,
                            onValueChange = { logoUrl = it },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Support Email
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Support / Contact Email", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = supportEmail,
                            onValueChange = { supportEmail = it },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Card 2: Maintenance Lockdown Mode (Prompt #24)
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
                            Icon(imageVector = Icons.Default.Construction, contentDescription = null, tint = WarningAmber)
                            Column {
                                Text("Global Maintenance Mode", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Displays maintenance banner and blocks jobs", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Switch(
                            checked = maintenanceMode,
                            onCheckedChange = { maintenanceMode = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = WarningAmber)
                        )
                    }

                    if (maintenanceMode) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Maintenance Screen Message", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            OutlinedTextField(
                                value = maintenanceMsg,
                                onValueChange = { maintenanceMsg = it },
                                minLines = 2,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        // Card 3: Registration & Login Controls (Prompts #25 & #26)
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.AdminPanelSettings, contentDescription = null, tint = IndigoVibrant)
                        Text("Access & Authentication Gates", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    // Registration toggle (Prompt #25)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Allow New User Registrations", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("When disabled, new users cannot create accounts", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = allowRegistration,
                            onCheckedChange = { allowRegistration = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = IndigoVibrant)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    // Login toggle (Prompt #26)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Allow User Logins", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("When disabled, existing user sign-ins are temporarily suspended", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = allowLogin,
                            onCheckedChange = { allowLogin = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = IndigoVibrant)
                        )
                    }
                }
            }
        }

        // Card 4: Force Update Options
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
                            Icon(imageVector = Icons.Default.SystemUpdate, contentDescription = null, tint = InfoSky)
                            Column {
                                Text("Client Force Update", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Require users to update to latest build", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Switch(
                            checked = forceUpdateEnabled,
                            onCheckedChange = { forceUpdateEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = InfoSky)
                        )
                    }

                    if (forceUpdateEnabled) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Store / Download URL", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            OutlinedTextField(
                                value = forceUpdateUrl,
                                onValueChange = { forceUpdateUrl = it },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        // Save Button
        item {
            Button(
                onClick = {
                    onSaveSettings(
                        settings.copy(
                            appName = appName.trim(),
                            appTagline = appTagline.trim(),
                            appLogoUrl = logoUrl.trim(),
                            publicAdminsContact = supportEmail.trim(),
                            maintenanceMode = maintenanceMode,
                            maintenanceMessage = maintenanceMsg.trim(),
                            registrationEnabled = allowRegistration,
                            loginEnabled = allowLogin,
                            forceUpdateRequired = forceUpdateEnabled,
                            forceUpdateUrl = forceUpdateUrl.trim()
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
                Text("Save System Settings to Firestore", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}
