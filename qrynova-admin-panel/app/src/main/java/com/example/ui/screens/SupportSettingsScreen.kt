package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.SupportSettings
import com.example.ui.viewmodel.AdminUiState
import com.example.ui.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportSettingsScreen(
    viewModel: AdminViewModel,
    uiState: AdminUiState
) {
    val scrollState = rememberScrollState()

    var whatsappNumberStr by remember(uiState.supportSettings.whatsappNumber) {
        mutableStateOf(uiState.supportSettings.whatsappNumber)
    }
    var whatsappGroupStr by remember(uiState.supportSettings.whatsappGroupLink) {
        mutableStateOf(uiState.supportSettings.whatsappGroupLink)
    }
    var telegramUsernameStr by remember(uiState.supportSettings.telegramUsername) {
        mutableStateOf(uiState.supportSettings.telegramUsername)
    }
    var telegramGroupStr by remember(uiState.supportSettings.telegramGroupLink) {
        mutableStateOf(uiState.supportSettings.telegramGroupLink)
    }
    var emailStr by remember(uiState.supportSettings.supportEmail) {
        mutableStateOf(uiState.supportSettings.supportEmail)
    }
    var websiteStr by remember(uiState.supportSettings.websiteUrl) {
        mutableStateOf(uiState.supportSettings.websiteUrl)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.setDrawerOpen(true) },
                    modifier = Modifier.testTag("menu_button")
                ) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Support & Community Links",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Real-time sync to User App contact & community buttons",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Official Contact Channels",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "When users tap 'Help & Support' in their app, they will be directed to these channels.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = whatsappNumberStr,
                        onValueChange = { whatsappNumberStr = it },
                        label = { Text("WhatsApp Support Number") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        placeholder = { Text("+8801700000000") }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = whatsappGroupStr,
                        onValueChange = { whatsappGroupStr = it },
                        label = { Text("WhatsApp Group / Community Link") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Groups, contentDescription = null) },
                        placeholder = { Text("https://chat.whatsapp.com/...") }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = telegramUsernameStr,
                        onValueChange = { telegramUsernameStr = it },
                        label = { Text("Telegram Username") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Send, contentDescription = null) },
                        placeholder = { Text("qrynova_support") }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = telegramGroupStr,
                        onValueChange = { telegramGroupStr = it },
                        label = { Text("Telegram Group / Channel Link") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Forum, contentDescription = null) },
                        placeholder = { Text("https://t.me/qrynova_community") }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = emailStr,
                        onValueChange = { emailStr = it },
                        label = { Text("Support Email Address") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        placeholder = { Text("support@qrynova.ai") }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = websiteStr,
                        onValueChange = { websiteStr = it },
                        label = { Text("Official Website / Documentation Link") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) },
                        placeholder = { Text("https://qrynova.ai") }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            viewModel.updateSupportSettings(
                                SupportSettings(
                                    whatsappNumber = whatsappNumberStr.trim(),
                                    whatsappGroupLink = whatsappGroupStr.trim(),
                                    telegramUsername = telegramUsernameStr.trim(),
                                    telegramGroupLink = telegramGroupStr.trim(),
                                    supportEmail = emailStr.trim(),
                                    websiteUrl = websiteStr.trim(),
                                    updatedAt = System.currentTimeMillis()
                                )
                            )
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save & Sync to Firestore")
                    }
                }
            }
        }
    }
}
