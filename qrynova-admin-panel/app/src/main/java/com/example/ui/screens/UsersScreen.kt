package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.*
import com.example.ui.components.OnlinePresenceBadge
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.components.UserAvatar
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreen(
    users: List<UserAccount>,
    searchQuery: String,
    statusFilter: UserStatus?,
    inactivityDaysFilter: Int?,
    usersError: String? = null,
    isUsersLoading: Boolean = false,
    onRefresh: () -> Unit = {},
    onBootstrapAdmin: () -> Unit = {},
    onDismissError: () -> Unit = {},
    onAddUser: (name: String, email: String, uid: String, startingCredits: Int) -> Unit = { _, _, _, _ -> },
    onLookupUid: (String) -> Unit = {},
    onSeedUsers: () -> Unit = {},
    onSearchChange: (String) -> Unit,
    onStatusFilterChange: (UserStatus?) -> Unit,
    onInactivityFilterChange: (Int?) -> Unit,
    onSelectUser: (UserAccount) -> Unit,
    onAdjustCredits: (UserAccount) -> Unit,
    onSendNotification: (UserAccount) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val clipboardManager = LocalClipboardManager.current
    var isCopied by remember { mutableStateOf(false) }

    var showAddUserDialog by remember { mutableStateOf(false) }
    var showLookupDialog by remember { mutableStateOf(false) }
    var showRulesDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section Header with Add User & UID Lookup buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader(
                title = "User Management",
                description = "Real-time user controls, balance & accounts"
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = { showLookupDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Lookup UID", fontSize = 11.sp)
                }

                Button(
                    onClick = { showAddUserDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanGlow,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("+ Add User", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Search Input & Action Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search name, email, or UID...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = CyanGlow)
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (isUsersLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = CyanGlow)
                } else {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh from Firestore", tint = CyanGlow)
                }
            }
        }

        // Status Filter Chips
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
                label = { Text("All (${users.size})", fontSize = 11.sp) }
            )
            FilterChip(
                selected = statusFilter == UserStatus.ACTIVE,
                onClick = { onStatusFilterChange(UserStatus.ACTIVE) },
                label = { Text("Active", fontSize = 11.sp) }
            )
            FilterChip(
                selected = statusFilter == UserStatus.SUSPENDED,
                onClick = { onStatusFilterChange(UserStatus.SUSPENDED) },
                label = { Text("Suspended", fontSize = 11.sp) }
            )
            FilterChip(
                selected = statusFilter == UserStatus.DISABLED,
                onClick = { onStatusFilterChange(UserStatus.DISABLED) },
                label = { Text("Disabled", fontSize = 11.sp) }
            )
        }

        // Inactivity Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Inactivity:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)

            FilterChip(
                selected = inactivityDaysFilter == null,
                onClick = { onInactivityFilterChange(null) },
                label = { Text("Any time", fontSize = 11.sp) }
            )
            FilterChip(
                selected = inactivityDaysFilter == 1,
                onClick = { onInactivityFilterChange(1) },
                label = { Text("≥ 1 day", fontSize = 11.sp) }
            )
            FilterChip(
                selected = inactivityDaysFilter == 3,
                onClick = { onInactivityFilterChange(3) },
                label = { Text("≥ 3 days", fontSize = 11.sp) }
            )
            FilterChip(
                selected = inactivityDaysFilter == 7,
                onClick = { onInactivityFilterChange(7) },
                label = { Text("≥ 7 days", fontSize = 11.sp) }
            )
            FilterChip(
                selected = inactivityDaysFilter == 14,
                onClick = { onInactivityFilterChange(14) },
                label = { Text("≥ 14 days", fontSize = 11.sp) }
            )
            FilterChip(
                selected = inactivityDaysFilter == 30,
                onClick = { onInactivityFilterChange(30) },
                label = { Text("≥ 30 days", fontSize = 11.sp) }
            )
        }

        // User Count Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${users.size} Users Registered in Console",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (users.isNotEmpty()) CyanGlow else MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isUsersLoading) {
                Text(
                    text = "Syncing with Firestore...",
                    fontSize = 11.sp,
                    color = WarningAmber
                )
            }
        }

        // Users List or Empty State with Quick Actions
        if (users.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GroupAdd,
                            contentDescription = null,
                            tint = CyanGlow,
                            modifier = Modifier.size(52.dp)
                        )
                        Text(
                            text = "No Users in Local View",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "You can add real user accounts directly to Firestore, lookup an existing UID from your user app, or seed sample active users to test controls.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onSeedUsers,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = IndigoVibrant,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Seed 3 Test Users", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { showAddUserDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyanGlow,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("+ Add New User", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(users, key = { it.uid }) { user ->
                    UserCard(
                        user = user,
                        dateFormat = dateFormat,
                        onSelect = { onSelectUser(user) },
                        onAdjustCredits = { onAdjustCredits(user) },
                        onNotify = { onSendNotification(user) }
                    )
                }
            }
        }
    }

    // Add User Dialog
    if (showAddUserDialog) {
        AddUserModal(
            onDismiss = { showAddUserDialog = false },
            onConfirm = { name, email, uid, startingCredits ->
                onAddUser(name, email, uid, startingCredits)
                showAddUserDialog = false
            }
        )
    }

    // UID Direct Lookup Dialog
    if (showLookupDialog) {
        DirectLookupModal(
            onDismiss = { showLookupDialog = false },
            onSearch = { query ->
                onLookupUid(query)
                showLookupDialog = false
            }
        )
    }

    // Firebase Rules Guide Dialog
    if (showRulesDialog) {
        RulesHelpModal(
            onDismiss = { showRulesDialog = false }
        )
    }
}

@Composable
fun AddUserModal(
    onDismiss: () -> Unit,
    onConfirm: (name: String, email: String, uid: String, startingCredits: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var customUid by remember { mutableStateOf("") }
    var startingCreditsText by remember { mutableStateOf("100") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add User to Firestore",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name") },
                    placeholder = { Text("e.g. John Doe") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address *") },
                    placeholder = { Text("user@example.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = customUid,
                    onValueChange = { customUid = it },
                    label = { Text("User UID (Optional / Auth UID)") },
                    placeholder = { Text("Leave blank to auto-generate") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = startingCreditsText,
                    onValueChange = { startingCreditsText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Starting Credits") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (email.isNotBlank()) {
                                val credits = startingCreditsText.toIntOrNull() ?: 100
                                onConfirm(name, email, customUid, credits)
                            }
                        },
                        enabled = email.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanGlow, contentColor = Color.Black)
                    ) {
                        Text("Save & Sync User")
                    }
                }
            }
        }
    }
}

@Composable
fun DirectLookupModal(
    onDismiss: () -> Unit,
    onSearch: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Direct Firestore User Lookup",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Enter any User UID or Email directly from your user app. This queries the document directly in Firestore.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("UID or Email Address") },
                    placeholder = { Text("e.g. Firebase Auth UID or user email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (query.isNotBlank()) {
                                onSearch(query)
                            }
                        },
                        enabled = query.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoVibrant, contentColor = Color.White)
                    ) {
                        Text("Fetch User")
                    }
                }
            }
        }
    }
}

@Composable
fun RulesHelpModal(
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    val rulesSnippet = """rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}"""

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Firebase Firestore Rules Guide",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "If PERMISSION_DENIED happens, open Firebase Console -> Firestore Database -> Rules, and paste:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0F172A),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = rulesSnippet,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = CyanGlow,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(rulesSnippet))
                            copied = true
                        }
                    ) {
                        Icon(imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (copied) "Rules Copied!" else "Copy Rules", fontSize = 12.sp)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoVibrant, contentColor = Color.White)
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

@Composable
fun UserCard(
    user: UserAccount,
    dateFormat: SimpleDateFormat,
    onSelect: () -> Unit,
    onAdjustCredits: () -> Unit,
    onNotify: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
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
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    UserAvatar(name = user.name, size = 40.dp, fontSize = 16.sp)

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = user.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            StatusBadge(status = user.status)
                        }
                        Text(
                            text = "${user.email} • @${user.nickname.ifEmpty { "user" }}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                OnlinePresenceBadge(presence = user.presence)
            }

            // Quick Stats Row
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.MonetizationOn, contentDescription = null, tint = CyanGlow, modifier = Modifier.size(16.dp))
                        Text("Credits: ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${user.credits}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyanGlow)
                    }

                    Text(
                        text = "${user.totalGenerations} Total Jobs (${user.completedGenerations} done)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Footer Actions & UID
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "UID: ${user.uid.take(12)}...",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(
                        onClick = onAdjustCredits,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text("Credits", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }

                    FilledTonalButton(
                        onClick = onNotify,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = IndigoVibrant.copy(alpha = 0.15f), contentColor = IndigoVibrant),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text("Notify", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
