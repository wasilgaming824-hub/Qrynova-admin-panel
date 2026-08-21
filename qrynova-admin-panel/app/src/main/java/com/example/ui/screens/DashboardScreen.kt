package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AdminNavigationSection
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    users: List<UserAccount>,
    requests: List<GenerationRequest>,
    creditSettings: CreditSettings,
    adSettings: AdSettings,
    systemSettings: AppSystemSettings,
    auditLogs: List<AdminAuditLog>,
    onNavigate: (AdminNavigationSection) -> Unit,
    onOpenDriveDelivery: (GenerationRequest) -> Unit,
    onSelectUser: (UserAccount) -> Unit
) {
    val totalUsers = users.size
    val activeUsers = users.count { it.status == UserStatus.ACTIVE }
    val onlineUsers = users.count { it.presence == OnlinePresence.ONLINE }
    val suspendedUsers = users.count { it.status == UserStatus.SUSPENDED }

    val totalRequests = requests.size
    val pendingRequests = requests.count { it.status == RequestStatus.PENDING }
    val processingRequests = requests.count { it.status == RequestStatus.PROCESSING }
    val completedRequests = requests.count { it.status == RequestStatus.COMPLETED }
    val failedRequests = requests.count { it.status == RequestStatus.FAILED }

    val imageRequests = requests.count { it.requestType == RequestType.IMAGE }
    val videoRequests = requests.count { it.requestType == RequestType.TEXT_TO_VIDEO }
    val i2vRequests = requests.count { it.requestType == RequestType.IMAGE_TO_VIDEO }

    val totalCreditsGranted = users.sumOf { it.totalCreditsGranted }
    val totalCreditsConsumed = users.sumOf { it.totalCreditsConsumed }
    val totalCreditsRemaining = users.sumOf { it.credits }

    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
    ) {
        // Top Banner / Maintenance Alert
        if (systemSettings.maintenanceMode) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = WarningAmber.copy(alpha = 0.15f),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(WarningAmber)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = WarningAmber)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("MAINTENANCE MODE ACTIVE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WarningAmber)
                            Text(systemSettings.maintenanceMessage, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        TextButton(onClick = { onNavigate(AdminNavigationSection.SYSTEM_SETTINGS) }) {
                            Text("Manage", fontSize = 11.sp, color = WarningAmber)
                        }
                    }
                }
            }
        }

        // Live Action Required Bar (Pending Requests)
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(
                        listOf(CyanGlow.copy(alpha = 0.4f), Color.Transparent)
                    )
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (pendingRequests > 0) WarningAmber.copy(alpha = 0.2f) else SuccessEmerald.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (pendingRequests > 0) Icons.Default.HourglassTop else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (pendingRequests > 0) WarningAmber else SuccessEmerald
                            )
                        }
                        Column {
                            Text(
                                text = if (pendingRequests > 0) "$pendingRequests Requests Awaiting Delivery" else "All Generation Requests Completed",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Deliver rendered outputs with Google Drive links",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = { onNavigate(AdminNavigationSection.GENERATION_REQUESTS) },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Open Queue", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Quick Stats Row 1: Users & Active Presence
        item {
            SectionHeader(
                title = "User & Account Statistics",
                description = "Real-time user telemetry and active sessions"
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "Total Users",
                    value = "$totalUsers",
                    subtitle = "$onlineUsers Online Now",
                    icon = Icons.Default.People,
                    accentColor = CyanGlow,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate(AdminNavigationSection.USERS) }
                )
                StatCard(
                    title = "Active Accounts",
                    value = "$activeUsers",
                    subtitle = if (suspendedUsers > 0) "$suspendedUsers Suspended" else "100% Healthy",
                    icon = Icons.Default.VerifiedUser,
                    accentColor = SuccessEmerald,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate(AdminNavigationSection.USERS) }
                )
            }
        }

        // Quick Stats Row 2: Generation Pipelines
        item {
            SectionHeader(
                title = "Generation Pipelines",
                description = "Workload distribution across models"
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "Total Jobs",
                    value = "$totalRequests",
                    subtitle = "$completedRequests Completed",
                    icon = Icons.Default.AutoAwesome,
                    accentColor = IndigoVibrant,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate(AdminNavigationSection.GENERATION_REQUESTS) }
                )
                StatCard(
                    title = "In Progress",
                    value = "${pendingRequests + processingRequests}",
                    subtitle = "$pendingRequests Pending / $processingRequests GPU",
                    icon = Icons.Default.Memory,
                    accentColor = WarningAmber,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate(AdminNavigationSection.GENERATION_REQUESTS) }
                )
            }
        }

        // Generation Types Breakdown
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PipelineMiniStat("Image Gen", "$imageRequests", Icons.Default.Image, CyanGlow)
                    PipelineMiniStat("Text-to-Video", "$videoRequests", Icons.Default.Videocam, VioletNeon)
                    PipelineMiniStat("Image-to-Video", "$i2vRequests", Icons.Default.Animation, IndigoVibrant)
                }
            }
        }

        // Quick Stats Row 3: Credit Economy
        item {
            SectionHeader(
                title = "Credit Economy Overview",
                description = "Configured costs and balance consumption"
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "Credits Remaining",
                    value = "$totalCreditsRemaining",
                    subtitle = "Pool: $totalCreditsGranted Granted",
                    icon = Icons.Default.MonetizationOn,
                    accentColor = SuccessEmerald,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate(AdminNavigationSection.CREDITS) }
                )
                StatCard(
                    title = "Credits Consumed",
                    value = "$totalCreditsConsumed",
                    subtitle = "Img: ${creditSettings.imageGenerationCost} • Vid: ${creditSettings.videoGenerationCost}",
                    icon = Icons.Default.Savings,
                    accentColor = WarningAmber,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate(AdminNavigationSection.CREDITS) }
                )
            }
        }

        // Actionable Generation Queue Preview
        item {
            SectionHeader(
                title = "Live Generation Queue",
                description = "Latest submissions from the User App"
            ) {
                TextButton(onClick = { onNavigate(AdminNavigationSection.GENERATION_REQUESTS) }) {
                    Text("View All (${requests.size})", fontSize = 12.sp)
                }
            }

            if (requests.isEmpty()) {
                Text(
                    text = "No generation requests submitted yet.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    requests.take(3).forEach { req ->
                        GenerationRequestCard(
                            request = req,
                            onOpenDelivery = { onOpenDriveDelivery(req) },
                            onSelectUser = {
                                val u = users.find { user -> user.uid == req.userId }
                                if (u != null) onSelectUser(u)
                            }
                        )
                    }
                }
            }
        }

        // Recent Audit Events
        item {
            SectionHeader(
                title = "Admin Activity Log",
                description = "Latest administrative actions and changes"
            ) {
                TextButton(onClick = { onNavigate(AdminNavigationSection.AUDIT_LOGS) }) {
                    Text("View All", fontSize = 12.sp)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                auditLogs.take(4).forEach { log ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(CyanGlow)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = log.description,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = log.adminEmail,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = dateFormat.format(Date(log.timestamp)),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PipelineMiniStat(label: String, count: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Text(text = count, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun GenerationRequestCard(
    request: GenerationRequest,
    onOpenDelivery: () -> Unit,
    onSelectUser: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    Text(
                        text = request.requestId,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "• ${request.requestType.name}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                RequestStatusBadge(status = request.status)
            }

            Text(
                text = request.prompt,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "By ${request.userName} (${request.resolution})",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (request.status == RequestStatus.PENDING || request.status == RequestStatus.PROCESSING) {
                    Button(
                        onClick = onOpenDelivery,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanGlowDark),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Deliver Result", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (request.status == RequestStatus.COMPLETED && request.googleDriveUrl.isNotBlank()) {
                    Text(
                        text = "Delivered via Google Drive",
                        fontSize = 10.sp,
                        color = SuccessEmerald,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
