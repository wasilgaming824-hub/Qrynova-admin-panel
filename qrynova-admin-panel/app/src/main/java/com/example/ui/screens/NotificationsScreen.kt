package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppNotification
import com.example.data.model.NotificationType
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotificationsScreen(
    notifications: List<AppNotification>,
    onOpenSendDialog: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
    ) {
        item {
            SectionHeader(
                title = "In-App Notifications & Alerts",
                description = "Dispatch announcements and direct push notices to users"
            ) {
                Button(
                    onClick = onOpenSendDialog,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanGlowDark)
                ) {
                    Icon(imageVector = Icons.Default.AddComment, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Message", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (notifications.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(imageVector = Icons.Default.NotificationsNone, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                        Text("No notifications recorded yet", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(notifications) { notif ->
                NotificationCardItem(notification = notif, dateFormat = dateFormat)
            }
        }
    }
}

@Composable
private fun NotificationCardItem(
    notification: AppNotification,
    dateFormat: SimpleDateFormat
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val (badgeBg, badgeText) = when (notification.type) {
                        NotificationType.INFO -> Pair(InfoSky.copy(alpha = 0.15f), InfoSky)
                        NotificationType.SUCCESS -> Pair(SuccessEmerald.copy(alpha = 0.15f), SuccessEmerald)
                        NotificationType.WARNING -> Pair(WarningAmber.copy(alpha = 0.15f), WarningAmber)
                        NotificationType.GENERATION -> Pair(CyanGlow.copy(alpha = 0.15f), CyanGlow)
                        NotificationType.SYSTEM -> Pair(VioletNeon.copy(alpha = 0.15f), VioletNeon)
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = badgeBg
                    ) {
                        Text(
                            text = notification.type.name,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeText,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (notification.isGlobal) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = IndigoVibrant.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "GLOBAL BROADCAST",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = IndigoVibrant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = dateFormat.format(Date(notification.createdAt)),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = notification.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = notification.message,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (notification.actionUrl.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, tint = CyanGlow, modifier = Modifier.size(14.dp))
                        Text(
                            text = "${notification.actionLabel.ifEmpty { "Link" }}: ${notification.actionUrl}",
                            fontSize = 11.sp,
                            color = CyanGlow
                        )
                    }
                }
            }

            if (!notification.isGlobal && !notification.targetUserId.isNullOrBlank()) {
                Text(
                    text = "Target User ID: ${notification.targetUserId}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
