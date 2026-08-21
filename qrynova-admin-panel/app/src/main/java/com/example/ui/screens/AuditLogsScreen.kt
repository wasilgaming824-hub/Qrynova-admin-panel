package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdminAuditLog
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AuditLogsScreen(
    auditLogs: List<AdminAuditLog>
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<String?>(null) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    val filteredLogs = remember(auditLogs, searchQuery, selectedFilter) {
        auditLogs.filter { log ->
            val matchesSearch = searchQuery.isBlank() ||
                    log.description.contains(searchQuery, ignoreCase = true) ||
                    log.adminEmail.contains(searchQuery, ignoreCase = true) ||
                    log.targetId.contains(searchQuery, ignoreCase = true)

            val matchesFilter = selectedFilter == null || log.targetType.equals(selectedFilter, ignoreCase = true)

            matchesSearch && matchesFilter
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search audit trails, admins, actions...", fontSize = 13.sp) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = CyanGlow) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        )

        // Filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Module:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)

            FilterChip(
                selected = selectedFilter == null,
                onClick = { selectedFilter = null },
                label = { Text("All (${auditLogs.size})", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedFilter == "USER",
                onClick = { selectedFilter = "USER" },
                label = { Text("Users", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedFilter == "CREDITS",
                onClick = { selectedFilter = "CREDITS" },
                label = { Text("Credits", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedFilter == "REQUEST",
                onClick = { selectedFilter = "REQUEST" },
                label = { Text("Requests", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedFilter == "AD_SETTINGS",
                onClick = { selectedFilter = "AD_SETTINGS" },
                label = { Text("Ads", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedFilter == "SYSTEM_SETTINGS",
                onClick = { selectedFilter = "SYSTEM_SETTINGS" },
                label = { Text("System", fontSize = 11.sp) }
            )
        }

        if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                    Text("No audit log records found", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredLogs) { log ->
                    AuditLogItemCard(log = log, dateFormat = dateFormat)
                }
            }
        }
    }
}

@Composable
private fun AuditLogItemCard(
    log: AdminAuditLog,
    dateFormat: SimpleDateFormat
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = IndigoVibrant.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = log.action,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanGlow,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "• ${log.targetType}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = dateFormat.format(Date(log.timestamp)),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = log.description,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Admin: ${log.adminEmail}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                if (log.targetId.isNotBlank()) {
                    Text(
                        text = "Target ID: ${log.targetId.take(16)}...",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
