package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdminAccount
import com.example.ui.theme.*
import com.example.ui.viewmodel.AdminNavigationSection

@Composable
fun AdminNavDrawerContent(
    currentSection: AdminNavigationSection,
    currentAdmin: AdminAccount?,
    pendingRequestsCount: Int,
    onSectionSelected: (AdminNavigationSection) -> Unit,
    onLogoutClick: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.width(300.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                IndigoVibrant.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(CyanGlow, IndigoVibrant)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Q",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }

                        Column {
                            Text(
                                text = "QRYNOVA",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Control & Delivery Center",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (currentAdmin != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(SuccessEmerald)
                                )
                                Column {
                                    Text(
                                        text = currentAdmin.displayName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = currentAdmin.email,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                thickness = 1.dp
            )

            // Nav List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(AdminNavigationSection.values()) { section ->
                    val isSelected = section == currentSection
                    val icon = getSectionIcon(section)
                    val badge = if (section == AdminNavigationSection.GENERATION_REQUESTS && pendingRequestsCount > 0) {
                        pendingRequestsCount.toString()
                    } else null

                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = section.title,
                                tint = if (isSelected) CyanGlow else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        label = {
                            Text(
                                text = section.title,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        badge = if (badge != null) {
                            {
                                Badge(
                                    containerColor = WarningAmber,
                                    contentColor = Color.Black
                                ) {
                                    Text(text = badge, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else null,
                        selected = isSelected,
                        onClick = { onSectionSelected(section) },
                        shape = RoundedCornerShape(10.dp),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = CyanGlow.copy(alpha = 0.12f),
                            unselectedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                thickness = 1.dp
            )

            // Logout row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLogoutClick() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = "Logout",
                    tint = ErrorRose
                )
                Text(
                    text = "Sign Out",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ErrorRose
                )
            }
        }
    }
}

private fun getSectionIcon(section: AdminNavigationSection): ImageVector {
    return when (section) {
        AdminNavigationSection.DASHBOARD -> Icons.Default.Dashboard
        AdminNavigationSection.USERS -> Icons.Default.People
        AdminNavigationSection.GENERATION_REQUESTS -> Icons.Default.AutoAwesome
        AdminNavigationSection.REFERRALS -> Icons.Default.Share
        AdminNavigationSection.SUPPORT_SETTINGS -> Icons.Default.ContactSupport
        AdminNavigationSection.CREDITS -> Icons.Default.MonetizationOn
        AdminNavigationSection.ADS -> Icons.Default.SmartDisplay
        AdminNavigationSection.GENERATION_SETTINGS -> Icons.Default.Tune
        AdminNavigationSection.NOTIFICATIONS -> Icons.Default.Notifications
        AdminNavigationSection.SYSTEM_SETTINGS -> Icons.Default.Settings
        AdminNavigationSection.AUDIT_LOGS -> Icons.Default.ReceiptLong
        AdminNavigationSection.SYSTEM_STATUS -> Icons.Default.HealthAndSafety
        AdminNavigationSection.SETUP_DOCS -> Icons.Default.MenuBook
        AdminNavigationSection.USER_APP_SIMULATOR -> Icons.Default.PhoneIphone
    }
}
