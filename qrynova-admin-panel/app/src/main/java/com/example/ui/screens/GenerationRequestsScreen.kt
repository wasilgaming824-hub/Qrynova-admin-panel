package com.example.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GenerationRequest
import com.example.data.model.RequestStatus
import com.example.data.model.RequestType
import com.example.ui.components.RequestStatusBadge
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerationRequestsScreen(
    requests: List<GenerationRequest>,
    searchQuery: String,
    statusFilter: RequestStatus?,
    typeFilter: RequestType?,
    onSearchChange: (String) -> Unit,
    onStatusFilterChange: (RequestStatus?) -> Unit,
    onTypeFilterChange: (RequestType?) -> Unit,
    onOpenDelivery: (GenerationRequest) -> Unit,
    onUpdateStatus: (requestId: String, status: RequestStatus) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search by Request ID, user, prompt...", fontSize = 13.sp) },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = CyanGlow)
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { onSearchChange("") }) {
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

        // Status Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Status:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)

            FilterChip(
                selected = statusFilter == null,
                onClick = { onStatusFilterChange(null) },
                label = { Text("All (${requests.size})", fontSize = 11.sp) }
            )
            FilterChip(
                selected = statusFilter == RequestStatus.PENDING,
                onClick = { onStatusFilterChange(RequestStatus.PENDING) },
                label = { Text("Pending", fontSize = 11.sp) }
            )
            FilterChip(
                selected = statusFilter == RequestStatus.PROCESSING,
                onClick = { onStatusFilterChange(RequestStatus.PROCESSING) },
                label = { Text("Processing", fontSize = 11.sp) }
            )
            FilterChip(
                selected = statusFilter == RequestStatus.COMPLETED,
                onClick = { onStatusFilterChange(RequestStatus.COMPLETED) },
                label = { Text("Completed", fontSize = 11.sp) }
            )
            FilterChip(
                selected = statusFilter == RequestStatus.FAILED,
                onClick = { onStatusFilterChange(RequestStatus.FAILED) },
                label = { Text("Failed", fontSize = 11.sp) }
            )
        }

        // Type Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Type:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)

            FilterChip(
                selected = typeFilter == null,
                onClick = { onTypeFilterChange(null) },
                label = { Text("All Types", fontSize = 11.sp) }
            )
            FilterChip(
                selected = typeFilter == RequestType.IMAGE,
                onClick = { onTypeFilterChange(RequestType.IMAGE) },
                label = { Text("Image", fontSize = 11.sp) }
            )
            FilterChip(
                selected = typeFilter == RequestType.TEXT_TO_VIDEO,
                onClick = { onTypeFilterChange(RequestType.TEXT_TO_VIDEO) },
                label = { Text("Text-to-Video", fontSize = 11.sp) }
            )
            FilterChip(
                selected = typeFilter == RequestType.IMAGE_TO_VIDEO,
                onClick = { onTypeFilterChange(RequestType.IMAGE_TO_VIDEO) },
                label = { Text("Image-to-Video", fontSize = 11.sp) }
            )
        }

        // Request List
        if (requests.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Inbox, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                    Text("No generation requests in this view", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(requests) { req ->
                    GenerationDetailCard(
                        request = req,
                        dateFormat = dateFormat,
                        onCopyPrompt = {
                            clipboard.setText(
                                AnnotatedString(
                                    "PROMPT:\n${req.prompt}\n\nDETAILS:\nType: ${req.requestType.name}\nResolution: ${req.resolution}\nAspect Ratio: ${req.aspectRatio}\nDuration: ${req.durationSeconds}s\nUser: ${req.userName} (${req.userEmail})"
                                )
                            )
                        },
                        onOpenDelivery = { onOpenDelivery(req) },
                        onMarkProcessing = { onUpdateStatus(req.requestId, RequestStatus.PROCESSING) }
                    )
                }
            }
        }
    }
}

@Composable
private fun GenerationDetailCard(
    request: GenerationRequest,
    dateFormat: SimpleDateFormat,
    onCopyPrompt: () -> Unit,
    onOpenDelivery: () -> Unit,
    onMarkProcessing: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = request.requestId,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "• ${request.requestType.name}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                RequestStatusBadge(status = request.status)
            }

            // Prompt Box
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "USER PROMPT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(
                            onClick = onCopyPrompt,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Prompt",
                                tint = CyanGlow,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        text = request.prompt,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Specs Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Res: ${request.resolution} (${request.aspectRatio})",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (request.requestType != RequestType.IMAGE) {
                    Text(
                        text = "Duration: ${request.durationSeconds}s",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "Cost: ${request.creditCost} credits",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CyanGlow
                )
            }

            // User Info & Created Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "User: ${request.userName} (${request.userEmail})",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = dateFormat.format(Date(request.createdAt)),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Completed Google Drive delivery banner if finished
            if (request.status == RequestStatus.COMPLETED && request.googleDriveUrl.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SuccessEmerald.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CloudDone, contentDescription = null, tint = SuccessEmerald, modifier = Modifier.size(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Google Drive Link Delivered", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SuccessEmerald)
                            Text(request.googleDriveUrl, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (request.status == RequestStatus.PENDING) {
                    OutlinedButton(
                        onClick = onMarkProcessing,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Memory, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mark GPU Processing", fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Button(
                    onClick = onOpenDelivery,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (request.status == RequestStatus.COMPLETED) IndigoVibrant else SuccessEmerald
                    ),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (request.status == RequestStatus.COMPLETED) "Edit Drive Link" else "Submit Drive Result",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
