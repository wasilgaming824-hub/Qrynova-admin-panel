package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun LoginScreen(
    isAuthenticated: Boolean = false,
    currentAdmin: com.example.data.model.AdminAccount? = null,
    isAuthenticating: Boolean = false,
    authError: String? = null,
    unauthorizedUid: String? = null,
    unauthorizedEmail: String? = null,
    authorizationRequired: Boolean = false,
    currentScreen: String = "LoginScreen",
    diagnosticInfo: com.example.data.firebase.AuthDiagnosticInfo = com.example.data.firebase.AuthDiagnosticInfo(),
    onLogin: (email: String, pass: String) -> Unit,
    onDismissError: () -> Unit = {},
    onOpenDocs: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    var copiedToClipboard by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBgMain)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Glowing Logo Branding
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(CyanGlow, IndigoVibrant, VioletNeon)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Q",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "QRYNOVA",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CyanGlow,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "ADMINISTRATION CONSOLE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = IndigoVibrant.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, IndigoVibrant.copy(alpha = 0.5f)),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    Text(
                        text = "ADMIN AUTH DEBUG BUILD: BUILD_20260820_v3.5_RUNTIME_TRACE",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = CyanGlow,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            // Auth Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.linearGradient(
                        listOf(CyanGlow.copy(alpha = 0.4f), IndigoVibrant.copy(alpha = 0.2f))
                    )
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Administrator Sign In",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Sign in with your Firebase account. Access requires authorization in Firestore collection 'admins/{uid}'.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Error Banner (if general error)
                    if (!authError.isNullOrBlank() && unauthorizedUid == null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ErrorRose.copy(alpha = 0.15f),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(ErrorRose, ErrorRose))),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Error",
                                    tint = ErrorRose,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = authError,
                                    fontSize = 12.sp,
                                    color = ErrorRose,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Email Field
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Admin Email", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            placeholder = { Text("admin@yourdomain.com", fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = CyanGlow)
                            },
                            singleLine = true,
                            enabled = !isAuthenticating,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Password Field
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Password", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = { Text("Enter your password", fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = CyanGlow)
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            enabled = !isAuthenticating,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (email.isNotBlank() && password.isNotBlank() && !isAuthenticating) {
                                    onLogin(email, password)
                                }
                            }),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Security Notice
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = SuccessEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Enforced by Firestore Security Rules & admins/{uid} document whitelist.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Login Button
                    Button(
                        onClick = {
                            if (email.isNotBlank() && password.isNotBlank()) {
                                onLogin(email, password)
                            }
                        },
                        enabled = !isAuthenticating && email.isNotBlank() && password.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanGlowDark,
                            contentColor = Color.White
                        )
                    ) {
                        if (isAuthenticating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Verifying Authorization...", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(imageVector = Icons.Default.Login, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sign In as Admin",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Quick Setup Guide link
            TextButton(onClick = onOpenDocs) {
                Icon(imageVector = Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = IndigoVibrant)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "How to add your UID to admins/{uid} in Firebase",
                    fontSize = 12.sp,
                    color = IndigoVibrant
                )
            }

            // Runtime Diagnostic / Debug Panel (Requested by Admin)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = null,
                            tint = CyanGlow,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "AUTH STATE & RUNTIME DIAGNOSTICS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanGlow
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    val firebaseAuthUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: diagnosticInfo.currentAuthUid ?: "null"
                    val firebaseAuthEmail = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email ?: diagnosticInfo.currentAuthEmail ?: "null"

                    DiagnosticRow(label = "Firebase currentUser UID", value = firebaseAuthUid, highlight = firebaseAuthUid != "null")
                    DiagnosticRow(label = "Firebase currentUser email", value = firebaseAuthEmail)
                    DiagnosticRow(label = "isAuthenticated", value = isAuthenticated.toString(), highlight = isAuthenticated)
                    DiagnosticRow(label = "currentAdmin != null", value = (currentAdmin != null).toString(), highlight = currentAdmin != null)
                    DiagnosticRow(label = "unauthorizedUid", value = unauthorizedUid ?: "null")
                    DiagnosticRow(label = "authError", value = authError ?: "null")
                    DiagnosticRow(label = "authorizationRequired", value = authorizationRequired.toString())
                    val showModalCalculated = authorizationRequired && unauthorizedUid != null
                    DiagnosticRow(label = "showAuthorizationModal", value = showModalCalculated.toString())
                    DiagnosticRow(label = "current screen/route", value = currentScreen)
                    DiagnosticRow(label = "Firebase projectId", value = diagnosticInfo.projectId)

                    val docExistsDisplay = when (diagnosticInfo.documentExists) {
                        true -> "true"
                        false -> "false"
                        null -> "null (Waiting for query)"
                    }
                    val docExistsColor = when (diagnosticInfo.documentExists) {
                        true -> SuccessEmerald
                        false -> ErrorRose
                        null -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "admins document exists",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = docExistsDisplay,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = docExistsColor
                        )
                    }

                    val statusVal = diagnosticInfo.documentData?.get("status")?.toString()
                        ?: diagnosticInfo.documentData?.get("Status")?.toString() ?: "N/A"
                    val isActiveVal = diagnosticInfo.documentData?.get("isActive")?.toString()
                        ?: diagnosticInfo.documentData?.get("is_active")?.toString() ?: "N/A"
                    val roleVal = diagnosticInfo.documentData?.get("role")?.toString()
                        ?: diagnosticInfo.documentData?.get("Role")?.toString() ?: "N/A"

                    DiagnosticRow(label = "status", value = statusVal)
                    DiagnosticRow(label = "isActive", value = isActiveVal)
                    DiagnosticRow(label = "role", value = roleVal)

                    val modalSourceText = if (showModalCalculated) {
                        "LoginScreen AlertDialog (authorizationRequired=true, unauthorizedUid=$unauthorizedUid)"
                    } else {
                        "None (Modal Hidden)"
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (showModalCalculated) WarningAmber.copy(alpha = 0.15f) else SuccessEmerald.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, if (showModalCalculated) WarningAmber.copy(alpha = 0.4f) else SuccessEmerald.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "MODAL SOURCE:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (showModalCalculated) WarningAmber else SuccessEmerald
                            )
                            Text(
                                text = modalSourceText,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (!diagnosticInfo.lastError.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ErrorRose.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Query Details: ${diagnosticInfo.lastError}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = ErrorRose,
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Unauthorized Admin Alert Dialog
        if (authorizationRequired && unauthorizedUid != null) {
            val isSuspended = authError?.contains("disabled or suspended", ignoreCase = true) == true
            AlertDialog(
                onDismissRequest = {
                    copiedToClipboard = false
                    onDismissError()
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.LockPerson,
                        contentDescription = null,
                        tint = WarningAmber,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        text = if (isSuspended) "Admin Account Inactive" else "Admin Authorization Required",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = if (isSuspended) {
                                "Your account is found in Firestore, but its status is marked as inactive or suspended."
                            } else {
                                "Your account is authenticated in Firebase, but is not yet registered in the 'admins' collection."
                            },
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // UID box with copy button
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = CardDefaults.outlinedCardBorder(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Your Firebase Authentication UID:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = unauthorizedUid,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CyanGlow,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(unauthorizedUid))
                                            copiedToClipboard = true
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (copiedToClipboard) Icons.Default.Check else Icons.Default.ContentCopy,
                                            contentDescription = "Copy UID",
                                            tint = if (copiedToClipboard) SuccessEmerald else CyanGlow,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                if (copiedToClipboard) {
                                    Text("Copied UID to clipboard!", fontSize = 11.sp, color = SuccessEmerald)
                                }
                            }
                        }

                        // Instructions
                        Text("To authorize this account in Firebase Console:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("1. Open project 'master-qrynova' in Firebase Console.", fontSize = 12.sp)
                        Text("2. Go to Firestore Database -> Start collection: 'admins'.", fontSize = 12.sp)
                        Text("3. Set Document ID = (paste your UID above).", fontSize = 12.sp)
                        Text("4. Add fields: role = \"SUPER_ADMIN\", status = \"ACTIVE\", isActive = true.", fontSize = 12.sp)
                        Text("5. Click Save, then sign in again.", fontSize = 12.sp)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            copiedToClipboard = false
                            onDismissError()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanGlowDark)
                    ) {
                        Text("Understood")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onOpenDocs) {
                        Text("View Setup Guide")
                    }
                }
            )
        }
    }
}

@Composable
private fun DiagnosticRow(
    label: String,
    value: String,
    highlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
            color = if (highlight) CyanGlow else MaterialTheme.colorScheme.onSurface
        )
    }
}

