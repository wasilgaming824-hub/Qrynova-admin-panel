package com.example.ui.components

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.NotificationType
import com.example.data.model.UserAccount
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationDialog(
    targetUser: UserAccount?,
    allUsers: List<UserAccount>,
    onDismiss: () -> Unit,
    onSendBroadcast: (title: String, message: String, type: NotificationType, actionUrl: String, actionLabel: String) -> Unit,
    onSendDirect: (userId: String, title: String, message: String, type: NotificationType, actionUrl: String, actionLabel: String) -> Unit
) {
    var isBroadcast by remember { mutableStateOf(targetUser == null) }
    var selectedUserId by remember { mutableStateOf(targetUser?.uid ?: allUsers.firstOrNull()?.uid ?: "") }
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(NotificationType.INFO) }
    var actionUrl by remember { mutableStateOf("") }
    var actionLabel by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Send In-App Notification",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isBroadcast) "Global Announcement (All Users)" else "Targeted User Dispatch",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Target Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = isBroadcast,
                        onClick = { isBroadcast = true },
                        label = { Text("Global Broadcast") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = !isBroadcast,
                        onClick = { isBroadcast = false },
                        label = { Text("Single User") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (!isBroadcast) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Select Target User *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        // Simple dropdown/picker
                        var expanded by remember { mutableStateOf(false) }
                        val currentSelectedUser = allUsers.find { it.uid == selectedUserId }

                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it }
                        ) {
                            OutlinedTextField(
                                value = currentSelectedUser?.let { "${it.name} (${it.email})" } ?: "Select user",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                allUsers.forEach { user ->
                                    DropdownMenuItem(
                                        text = { Text("${user.name} • ${user.email}", fontSize = 12.sp) },
                                        onClick = {
                                            selectedUserId = user.uid
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Notification Type
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Notification Type", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    var typeExpanded by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = typeExpanded,
                        onExpandedChange = { typeExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedType.label,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = typeExpanded,
                            onDismissRequest = { typeExpanded = false }
                        ) {
                            NotificationType.values().forEach { nType ->
                                DropdownMenuItem(
                                    text = { Text(nType.label, fontSize = 12.sp) },
                                    onClick = {
                                        selectedType = nType
                                        typeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Title
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Title *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("e.g. GPU Cluster Upgrade Completed") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Message
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Message Content *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        placeholder = { Text("Enter detailed notification message for users...") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Optional CTA URL
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Action URL & Label (Optional)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = actionUrl,
                        onValueChange = { actionUrl = it },
                        placeholder = { Text("https://qrynova.ai/promo", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = actionLabel,
                        onValueChange = { actionLabel = it },
                        placeholder = { Text("Button Label (e.g. Learn More)", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (title.isNotBlank() && message.isNotBlank()) {
                                if (isBroadcast) {
                                    onSendBroadcast(title, message, selectedType, actionUrl, actionLabel)
                                } else {
                                    onSendDirect(selectedUserId, title, message, selectedType, actionUrl, actionLabel)
                                }
                            }
                        },
                        enabled = title.isNotBlank() && message.isNotBlank(),
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoVibrant)
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Dispatch", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
