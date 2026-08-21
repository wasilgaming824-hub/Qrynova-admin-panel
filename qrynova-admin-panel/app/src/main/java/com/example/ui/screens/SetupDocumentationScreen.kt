package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.firebase.FirebaseConnectionStatus
import com.example.data.firebase.FirebaseManager
import com.example.data.firebase.FirebaseWebConfig
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*

@Composable
fun SetupDocumentationScreen() {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val firebaseManager = remember { FirebaseManager.getInstance() }

    val connectionStatus by firebaseManager.connectionStatus.collectAsStateWithLifecycle()
    val connectedProjectId by firebaseManager.connectedProjectId.collectAsStateWithLifecycle()
    val statusMessage by firebaseManager.statusMessage.collectAsStateWithLifecycle()

    var showConfigDialog by remember { mutableStateOf(false) }
    var apiKeyInput by remember { mutableStateOf(com.example.data.firebase.ExistingFirebaseConfig.API_KEY) }
    var authDomainInput by remember { mutableStateOf(com.example.data.firebase.ExistingFirebaseConfig.AUTH_DOMAIN) }
    var projectIdInput by remember { mutableStateOf(com.example.data.firebase.ExistingFirebaseConfig.PROJECT_ID) }
    var storageBucketInput by remember { mutableStateOf(com.example.data.firebase.ExistingFirebaseConfig.STORAGE_BUCKET) }
    var messagingSenderIdInput by remember { mutableStateOf(com.example.data.firebase.ExistingFirebaseConfig.MESSAGING_SENDER_ID) }
    var appIdInput by remember { mutableStateOf(com.example.data.firebase.ExistingFirebaseConfig.APP_ID) }

    val securityRulesSnippet = """rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // 1. Helper function: Verifies if the authenticated caller has an active, valid admin record
    function isAdmin() {
      return request.auth != null && 
        exists(/databases/$(database)/documents/admins/$(request.auth.uid)) &&
        get(/databases/$(database)/documents/admins/$(request.auth.uid)).data.isActive != false &&
        get(/databases/$(database)/documents/admins/$(request.auth.uid)).data.status != 'SUSPENDED' &&
        get(/databases/$(database)/documents/admins/$(request.auth.uid)).data.status != 'DISABLED';
    }

    // 2. Helper function: Enforces field-level validation on non-admin user profile updates.
    // Sensitive credit, generation counters, status, and referral claim fields are strictly protected against modification by normal users.
    function isSafeUserProfileUpdate() {
      let allowedKeys = [
        'name', 'nickname', 'profileImageUrl', 'lastActiveAt', 
        'presence', 'updatedAt'
      ];
      return request.resource.data.diff(resource.data).affectedKeys().hasOnly(allowedKeys);
    }
    
    // 3. Admin accounts & authorization
    // Normal users CANNOT create or elevate their own document in admins collection
    match /admins/{adminId} {
      allow read: if request.auth != null && (request.auth.uid == adminId || isAdmin());
      allow write: if isAdmin();
    }
    
    // 4. User Profiles & Credit Accounts
    match /users/{userId} {
      allow read: if request.auth != null && (request.auth.uid == userId || isAdmin());
      allow create: if request.auth != null && request.auth.uid == userId &&
        request.resource.data.credits <= 100 &&
        request.resource.data.status == 'ACTIVE';
      allow update: if isAdmin() || (
        request.auth != null && request.auth.uid == userId && isSafeUserProfileUpdate()
      );
      allow delete: if isAdmin();
    }
    
    // 5. Generation Queue & Requests
    match /generationRequests/{requestId} {
      allow read: if request.auth != null && (resource.data.userId == request.auth.uid || isAdmin());
      allow create: if request.auth != null && request.resource.data.userId == request.auth.uid;
      allow update, delete: if isAdmin();
    }
    
    // 6. System Settings & Real-Time Configurations (Read-only for users, write for Admin)
    match /settings/{settingDoc} {
      allow read: if request.auth != null;
      allow write: if isAdmin();
    }

    match /system_settings/{settingDoc} {
      allow read: if request.auth != null;
      allow write: if isAdmin();
    }

    // 7. Referral Records
    match /referrals/{referralId} {
      allow read: if request.auth != null && (resource.data.referrerId == request.auth.uid || resource.data.referredUserId == request.auth.uid || isAdmin());
      allow write: if isAdmin();
    }
    
    // 8. In-App Notifications
    match /notifications/{notifId} {
      allow read: if request.auth != null && (resource.data.isGlobal == true || resource.data.targetUserId == request.auth.uid || isAdmin());
      allow write: if isAdmin();
    }
    
    // 9. Credit Transactions & Audit Logs
    match /creditTransactions/{txId} {
      allow read: if request.auth != null && (resource.data.userId == request.auth.uid || isAdmin());
      allow write: if isAdmin();
    }
    
    match /adminAuditLogs/{logId} {
      allow read, write: if isAdmin();
    }
  }
}"""

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 36.dp)
    ) {
        item {
            SectionHeader(
                title = "Firebase & Backend Connection",
                description = "Live connection status with your existing QRYNOVA Firebase project"
            )
        }

        // Live Firebase Status Card
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
                            Icon(
                                imageVector = when (connectionStatus) {
                                    FirebaseConnectionStatus.CONNECTED -> Icons.Default.CloudDone
                                    FirebaseConnectionStatus.CONNECTING -> Icons.Default.CloudSync
                                    FirebaseConnectionStatus.ERROR -> Icons.Default.CloudOff
                                    FirebaseConnectionStatus.NOT_CONFIGURED -> Icons.Default.CloudQueue
                                },
                                contentDescription = null,
                                tint = when (connectionStatus) {
                                    FirebaseConnectionStatus.CONNECTED -> SuccessEmerald
                                    FirebaseConnectionStatus.CONNECTING -> CyanGlow
                                    FirebaseConnectionStatus.ERROR -> ErrorRose
                                    FirebaseConnectionStatus.NOT_CONFIGURED -> WarningAmber
                                }
                            )
                            Column {
                                Text("Existing Firebase Project", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = if (connectedProjectId != null) "Project ID: $connectedProjectId" else "Status: ${connectionStatus.name}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = { showConfigDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoVibrant),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Configure", fontSize = 11.sp)
                        }
                    }

                    Text(
                        text = statusMessage,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    // Connected Target Collections List
                    Text("Target Firestore Collections in Existing Project:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(
                            "• users/{FirebaseAuthenticationUID} — Live User Profiles & Credits",
                            "• generationRequests/{requestId} — Queue for Image / Video rendering",
                            "• settings/systemSettings & settings/adSettings — Platform Controls",
                            "• notifications/{notificationId} — In-app alerts & updates",
                            "• creditTransactions/{txId} — Credit ledger & audit trails"
                        ).forEach { colText ->
                            Text(colText, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = CyanGlow)
                        }
                    }
                }
            }
        }

        // STEP 1: Firebase Project Setup
        item {
            DocStepCard(
                stepNumber = "1",
                title = "Create Firebase Project & Enable Services",
                description = "In your Firebase Console (https://console.firebase.google.com):",
                items = listOf(
                    "1. Click 'Add project' and name it 'QRYNOVA' (or your desired brand name).",
                    "2. Navigate to Build -> Authentication -> Sign-in method. Enable 'Email/Password' and optionally 'Google'.",
                    "3. Navigate to Build -> Firestore Database -> Create database in production mode.",
                    "4. In Project Settings -> General -> Add app -> Add Android app (package: com.example / com.aistudio.qrynova) and download google-services.json if compiling external builds."
                )
            )
        }

        // STEP 2: Firestore Security Rules
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CyanGlow.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("2", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CyanGlow)
                            }
                            Text("Deploy Production Security Rules", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { clipboard.setText(AnnotatedString(securityRulesSnippet)) },
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy Rules", modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy Rules", fontSize = 10.sp)
                        }
                    }

                    Text(
                        text = "Paste these rules into Firestore Database -> Rules tab to strictly enforce admin authentication and protect user data:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = securityRulesSnippet,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = CyanGlow,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }

        // STEP 3: Admin Provisioning
        item {
            DocStepCard(
                stepNumber = "3",
                title = "Provisioning Your Admin Account in Firestore",
                description = "To grant an account full administrative access to this console:",
                items = listOf(
                    "1. Sign up or register your account in Firebase Authentication (e.g. admin@qrynova.ai).",
                    "2. Copy the newly created user's UID from the Authentication tab.",
                    "3. Open Firestore Database -> Start collection named 'admins'.",
                    "4. Set Document ID = <YOUR_COPIED_UID>.",
                    "5. Add fields: email (string) = 'admin@qrynova.ai', role (string) = 'SUPER_ADMIN', active (boolean) = true.",
                    "6. You can now log into this QRYNOVA Admin Console with those credentials!"
                )
            )
        }

        // STEP 4: Google Drive Delivery Workflow
        item {
            DocStepCard(
                stepNumber = "4",
                title = "Google Drive Rendering & Delivery Workflow",
                description = "How generation requests are fulfilled by administrators:",
                items = listOf(
                    "1. User submits an Image, Video, or Image-to-Video generation in the User App.",
                    "2. The request enters the live 'Generation Requests' queue as PENDING and deducts user credits.",
                    "3. Admin copies the prompt & specs using the 'Copy Prompt' action.",
                    "4. Admin generates the output on high-end external GPU / ComfyUI / Midjourney / Runway / Luma / Sora.",
                    "5. Admin uploads the resulting .png / .mp4 to Google Drive with 'Anyone with the link can view' access.",
                    "6. Admin pastes the Google Drive link into this console's delivery modal and clicks 'Submit & Deliver'.",
                    "7. The User App instantly receives real-time snapshot update and delivers the Google Drive download button!"
                )
            )
        }

        // STEP 5: 10-Second Ad Gate Integration
        item {
            DocStepCard(
                stepNumber = "5",
                title = "10-Second Ad Gate Timer Logic in Client Apps",
                description = "How the sponsor gate is enforced before submission:",
                items = listOf(
                    "1. User taps 'Generate Image' or 'Generate Video'.",
                    "2. Client checks settings/adSettings document in Firestore.",
                    "3. If globalAdsEnabled is true and placement is active, the 10-second sponsor interstitial opens.",
                    "4. The countdown timer ticks from 10 down to 0; skip/submit buttons are disabled until the timer completes.",
                    "5. Upon completion, the ad gate is marked 'completed' and the generation request is submitted to Firestore."
                )
            )
        }
    }

    if (showConfigDialog) {
        AlertDialog(
            onDismissRequest = { showConfigDialog = false },
            title = {
                Text("Connect Existing Firebase Project", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Enter the existing Firebase Web App configuration from your QRYNOVA project:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("apiKey *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = projectIdInput,
                        onValueChange = { projectIdInput = it },
                        label = { Text("projectId *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = appIdInput,
                        onValueChange = { appIdInput = it },
                        label = { Text("appId *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = authDomainInput,
                        onValueChange = { authDomainInput = it },
                        label = { Text("authDomain (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = storageBucketInput,
                        onValueChange = { storageBucketInput = it },
                        label = { Text("storageBucket (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = messagingSenderIdInput,
                        onValueChange = { messagingSenderIdInput = it },
                        label = { Text("messagingSenderId (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val config = FirebaseWebConfig(
                            apiKey = apiKeyInput.trim(),
                            authDomain = authDomainInput.trim(),
                            projectId = projectIdInput.trim(),
                            storageBucket = storageBucketInput.trim(),
                            messagingSenderId = messagingSenderIdInput.trim(),
                            appId = appIdInput.trim()
                        )
                        firebaseManager.initializeWithConfig(context, config)
                        showConfigDialog = false
                    },
                    enabled = apiKeyInput.isNotBlank() && projectIdInput.isNotBlank() && appIdInput.isNotBlank()
                ) {
                    Text("Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfigDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DocStepCard(
    stepNumber: String,
    title: String,
    description: String,
    items: List<String>
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(IndigoVibrant.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stepNumber, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CyanGlow)
                }
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }

            Text(description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            items.forEach { stepText ->
                Text(
                    text = stepText,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
