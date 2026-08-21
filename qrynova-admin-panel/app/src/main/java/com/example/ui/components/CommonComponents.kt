package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OnlinePresence
import com.example.data.model.RequestStatus
import com.example.data.model.UserStatus
import com.example.ui.theme.*
import com.example.ui.viewmodel.AdminNavigationSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTopBar(
    currentSection: AdminNavigationSection,
    onMenuClick: () -> Unit,
    onSimulatorClick: () -> Unit,
    onHealthClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open Navigation Menu",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "QRYNOVA",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(IndigoVibrant.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ADMIN CONSOLE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = IndigoVibrant
                            )
                        }
                    }
                    Text(
                        text = currentSection.title,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Live Simulator Button
                FilledTonalButton(
                    onClick = onSimulatorClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = CyanGlow.copy(alpha = 0.15f),
                        contentColor = CyanGlow
                    ),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneIphone,
                        contentDescription = "User App Simulator",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "User App",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Health Status Indicator
                IconButton(
                    onClick = onHealthClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Firebase Status Connected",
                            tint = SuccessEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector,
    accentColor: Color = CyanGlow,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                listOf(accentColor.copy(alpha = 0.3f), Color.Transparent)
            )
        ),
        modifier = modifier
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
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun StatusBadge(status: UserStatus) {
    val (bg, text, label) = when (status) {
        UserStatus.ACTIVE -> Triple(SuccessEmerald.copy(alpha = 0.15f), SuccessEmerald, "ACTIVE")
        UserStatus.SUSPENDED -> Triple(WarningAmber.copy(alpha = 0.15f), WarningAmber, "SUSPENDED")
        UserStatus.DISABLED -> Triple(ErrorRose.copy(alpha = 0.15f), ErrorRose, "DISABLED")
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = text
        )
    }
}

@Composable
fun RequestStatusBadge(status: RequestStatus) {
    val (bg, text, label) = when (status) {
        RequestStatus.PENDING -> Triple(WarningAmber.copy(alpha = 0.15f), WarningAmber, "PENDING")
        RequestStatus.PROCESSING -> Triple(InfoSky.copy(alpha = 0.15f), InfoSky, "PROCESSING")
        RequestStatus.COMPLETED -> Triple(SuccessEmerald.copy(alpha = 0.15f), SuccessEmerald, "COMPLETED")
        RequestStatus.FAILED -> Triple(ErrorRose.copy(alpha = 0.15f), ErrorRose, "FAILED")
        RequestStatus.CANCELLED -> Triple(TextMutedDark.copy(alpha = 0.15f), TextMutedDark, "CANCELLED")
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = text
        )
    }
}

@Composable
fun OnlinePresenceBadge(presence: OnlinePresence) {
    val (color, label) = when (presence) {
        OnlinePresence.ONLINE -> Pair(SuccessEmerald, "Online")
        OnlinePresence.RECENTLY_ACTIVE -> Pair(WarningAmber, "Recent")
        OnlinePresence.OFFLINE -> Pair(TextMutedDark, "Offline")
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = color
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    description: String? = null,
    actionButton: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (description != null) {
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (actionButton != null) {
            actionButton()
        }
    }
}

@Composable
fun UserAvatar(
    name: String,
    size: androidx.compose.ui.unit.Dp = 40.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 16.sp
) {
    val initial = name.trim().take(1).uppercase().ifEmpty { "U" }
    val gradients = listOf(
        listOf(CyanGlow, IndigoVibrant),
        listOf(IndigoVibrant, VioletNeon),
        listOf(VioletNeon, PurpleAccent),
        listOf(CyanGlowDark, InfoSky),
        listOf(SuccessEmerald, CyanGlow)
    )
    val colorIndex = kotlin.math.abs(name.hashCode()) % gradients.size
    val gradient = gradients[colorIndex]

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(gradient)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
