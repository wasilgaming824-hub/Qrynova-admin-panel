package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.DarkBgMain
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AdminNavigationSection
import com.example.ui.viewmodel.AdminViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                QrynovaAdminApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrynovaAdminApp(
    viewModel: AdminViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        com.example.data.firebase.FirebaseManager.getInstance().checkAndInitDefault(context)
    }

    // Dialog & Sheet States
    var deliveryRequestTarget by remember { mutableStateOf<GenerationRequest?>(null) }
    var creditAdjustTarget by remember { mutableStateOf<UserAccount?>(null) }
    var userDetailTarget by remember { mutableStateOf<UserAccount?>(null) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    var notificationTargetUser by remember { mutableStateOf<UserAccount?>(null) }
    var showSetupDocsModal by remember { mutableStateOf(false) }

    // Surface SnackBar / Toast alerts
    LaunchedEffect(uiState.snackBarMessage) {
        uiState.snackBarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSnackBar()
        }
    }

    if (!uiState.isAuthenticated || uiState.currentAdmin == null) {
        LoginScreen(
            isAuthenticated = uiState.isAuthenticated,
            currentAdmin = uiState.currentAdmin,
            isAuthenticating = uiState.isAuthenticating,
            authError = uiState.authError,
            unauthorizedUid = uiState.unauthorizedUid,
            unauthorizedEmail = uiState.unauthorizedEmail,
            authorizationRequired = uiState.authorizationRequired,
            currentScreen = "LoginScreen",
            diagnosticInfo = uiState.diagnosticInfo,
            onLogin = { email, pass ->
                viewModel.login(email, pass)
            },
            onDismissError = {
                viewModel.clearAuthError()
            },
            onOpenDocs = {
                showSetupDocsModal = true
            }
        )

        if (showSetupDocsModal) {
            Dialog(
                onDismissRequest = { showSetupDocsModal = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Firebase & Admin Setup Guide") },
                            navigationIcon = {
                                IconButton(onClick = { showSetupDocsModal = false }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                                }
                            }
                        )
                    }
                ) { innerPad ->
                    Box(modifier = Modifier.padding(innerPad)) {
                        SetupDocumentationScreen()
                    }
                }
            }
        }
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                AdminNavDrawerContent(
                    currentSection = uiState.currentSection,
                    currentAdmin = uiState.currentAdmin,
                    pendingRequestsCount = uiState.requests.count { it.status == RequestStatus.PENDING },
                    onSectionSelected = { section ->
                        viewModel.selectSection(section)
                        coroutineScope.launch { drawerState.close() }
                    },
                    onLogoutClick = {
                        coroutineScope.launch { drawerState.close() }
                        viewModel.logout()
                    }
                )
            }
        ) {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBgMain),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    AdminTopBar(
                        currentSection = uiState.currentSection,
                        onMenuClick = {
                            coroutineScope.launch { drawerState.open() }
                        },
                        onSimulatorClick = {
                            viewModel.selectSection(AdminNavigationSection.USER_APP_SIMULATOR)
                        },
                        onHealthClick = {
                            viewModel.selectSection(AdminNavigationSection.SYSTEM_STATUS)
                        }
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(DarkBgMain)
                ) {
                    when (uiState.currentSection) {
                        AdminNavigationSection.DASHBOARD -> {
                            DashboardScreen(
                                users = uiState.users,
                                requests = uiState.requests,
                                creditSettings = uiState.creditSettings,
                                adSettings = uiState.adSettings,
                                systemSettings = uiState.systemSettings,
                                auditLogs = uiState.auditLogs,
                                onNavigate = { viewModel.selectSection(it) },
                                onOpenDriveDelivery = { deliveryRequestTarget = it },
                                onSelectUser = { userDetailTarget = it }
                            )
                        }

                        AdminNavigationSection.USERS -> {
                            UsersScreen(
                                users = uiState.filteredUsers,
                                searchQuery = uiState.userSearchQuery,
                                statusFilter = uiState.userStatusFilter,
                                inactivityDaysFilter = uiState.userInactivityDaysFilter,
                                usersError = uiState.usersError,
                                isUsersLoading = uiState.isUsersLoading,
                                onRefresh = { viewModel.refreshUsers() },
                                onBootstrapAdmin = { viewModel.bootstrapAdminDocument() },
                                onDismissError = { viewModel.dismissUsersError() },
                                onAddUser = { name, email, uid, startingCredits ->
                                    viewModel.createUser(name, email, uid, startingCredits)
                                },
                                onLookupUid = { viewModel.lookupUserByUidOrEmail(it) },
                                onSeedUsers = { viewModel.seedSampleUsers() },
                                onSearchChange = { viewModel.setUserSearchQuery(it) },
                                onStatusFilterChange = { viewModel.setUserStatusFilter(it) },
                                onInactivityFilterChange = { viewModel.setUserInactivityFilter(it) },
                                onSelectUser = { userDetailTarget = it },
                                onAdjustCredits = { creditAdjustTarget = it },
                                onSendNotification = {
                                    notificationTargetUser = it
                                    showNotificationDialog = true
                                }
                            )
                        }

                        AdminNavigationSection.GENERATION_REQUESTS -> {
                            GenerationRequestsScreen(
                                requests = uiState.filteredRequests,
                                searchQuery = uiState.requestSearchQuery,
                                statusFilter = uiState.requestStatusFilter,
                                typeFilter = uiState.requestTypeFilter,
                                onSearchChange = { viewModel.setRequestSearchQuery(it) },
                                onStatusFilterChange = { viewModel.setRequestStatusFilter(it) },
                                onTypeFilterChange = { viewModel.setRequestTypeFilter(it) },
                                onOpenDelivery = { deliveryRequestTarget = it },
                                onUpdateStatus = { reqId, status -> viewModel.updateRequestStatus(reqId, status) }
                            )
                        }

                        AdminNavigationSection.REFERRALS -> {
                            ReferralManagerScreen(
                                state = uiState,
                                viewModel = viewModel
                            )
                        }

                        AdminNavigationSection.SUPPORT_SETTINGS -> {
                            SupportSettingsScreen(
                                viewModel = viewModel,
                                uiState = uiState
                            )
                        }

                        AdminNavigationSection.CREDITS -> {
                            CreditSettingsScreen(
                                creditSettings = uiState.creditSettings,
                                transactions = uiState.creditTransactions,
                                onSaveSettings = { viewModel.updateCreditSettings(it) }
                            )
                        }

                        AdminNavigationSection.ADS -> {
                            AdManagementScreen(
                                adSettings = uiState.adSettings,
                                onSaveSettings = { viewModel.updateAdSettings(it) },
                                onOpenEditPlacement = { placementKey ->
                                    Toast.makeText(context, "Editing placement for: $placementKey", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }

                        AdminNavigationSection.GENERATION_SETTINGS -> {
                            GenerationSettingsScreen(
                                generationSettings = uiState.generationSettings,
                                onSaveSettings = { viewModel.updateGenerationSettings(it) }
                            )
                        }

                        AdminNavigationSection.NOTIFICATIONS -> {
                            NotificationsScreen(
                                notifications = uiState.notifications,
                                onOpenSendDialog = {
                                    notificationTargetUser = null
                                    showNotificationDialog = true
                                }
                            )
                        }

                        AdminNavigationSection.SYSTEM_SETTINGS -> {
                            SystemSettingsScreen(
                                settings = uiState.systemSettings,
                                onSaveSettings = { viewModel.updateSystemSettings(it) }
                            )
                        }

                        AdminNavigationSection.AUDIT_LOGS -> {
                            AuditLogsScreen(
                                auditLogs = uiState.auditLogs
                            )
                        }

                        AdminNavigationSection.SYSTEM_STATUS -> {
                            SystemHealthScreen(
                                usersCount = uiState.users.size,
                                requestsCount = uiState.requests.size,
                                notificationsCount = uiState.notifications.size,
                                auditLogsCount = uiState.auditLogs.size
                            )
                        }

                        AdminNavigationSection.SETUP_DOCS -> {
                            SetupDocumentationScreen()
                        }

                        AdminNavigationSection.USER_APP_SIMULATOR -> {
                            val activeUser = uiState.users.firstOrNull() ?: UserAccount(
                                uid = "sim_user",
                                name = "Alex Vance",
                                email = "alex.vance@ai-studio.net"
                            )
                            UserAppSimulatorScreen(
                                activeUser = activeUser,
                                creditSettings = uiState.creditSettings,
                                adSettings = uiState.adSettings,
                                generationSettings = uiState.generationSettings,
                                systemSettings = uiState.systemSettings,
                                referralSettings = uiState.referralSettings,
                                aiLimitsSettings = uiState.aiLimitsSettings,
                                supportSettings = uiState.supportSettings,
                                userRequests = uiState.requests,
                                notifications = uiState.notifications,
                                onSubmitGeneration = { prompt, type, res, aspect ->
                                    viewModel.submitSimulatedUserGeneration(
                                        userId = activeUser.uid,
                                        userName = activeUser.name,
                                        userEmail = activeUser.email,
                                        prompt = prompt,
                                        type = type,
                                        resolution = res,
                                        aspectRatio = aspect
                                    )
                                },
                                onClaimReferral = { code ->
                                    viewModel.claimReferral(
                                        userId = activeUser.uid,
                                        referralCode = code
                                    )
                                },
                                onBackToAdmin = {
                                    viewModel.selectSection(AdminNavigationSection.DASHBOARD)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Google Drive Result Delivery Modal
    deliveryRequestTarget?.let { request ->
        GoogleDriveDeliveryDialog(
            request = request,
            onDismiss = { deliveryRequestTarget = null },
            onSubmit = { driveUrl, adminMessage ->
                viewModel.submitGoogleDriveResult(
                    requestId = request.requestId,
                    googleDriveUrl = driveUrl,
                    adminMessage = adminMessage
                )
                deliveryRequestTarget = null
            }
        )
    }

    // Manual Credit Adjustment Dialog
    creditAdjustTarget?.let { user ->
        CreditAdjustmentDialog(
            user = user,
            onDismiss = { creditAdjustTarget = null },
            onConfirm = { amount, type, reason ->
                viewModel.adjustUserCredits(
                    userId = user.uid,
                    amount = amount,
                    type = type,
                    reason = reason
                )
                creditAdjustTarget = null
            }
        )
    }

    // User Profile / Details Modal
    userDetailTarget?.let { user ->
        // Refresh the user instance from state if changed
        val latestUser = uiState.users.find { it.uid == user.uid } ?: user

        UserDetailDialog(
            user = latestUser,
            onDismiss = { userDetailTarget = null },
            onAdjustCredits = {
                creditAdjustTarget = latestUser
            },
            onChangeStatus = { newStatus ->
                viewModel.updateUserStatus(latestUser.uid, newStatus)
            },
            onSendNotification = {
                notificationTargetUser = latestUser
                showNotificationDialog = true
            },
            onAddNote = { note ->
                viewModel.addUserNote(latestUser.uid, note)
            }
        )
    }

    // Dispatch Notification Dialog
    if (showNotificationDialog) {
        NotificationDialog(
            targetUser = notificationTargetUser,
            allUsers = uiState.users,
            onDismiss = {
                showNotificationDialog = false
                notificationTargetUser = null
            },
            onSendBroadcast = { title, message, type, url, label ->
                viewModel.sendBroadcastNotification(
                    title = title,
                    message = message,
                    type = type,
                    actionUrl = url,
                    actionLabel = label
                )
                showNotificationDialog = false
                notificationTargetUser = null
            },
            onSendDirect = { userId, title, message, type, url, label ->
                viewModel.sendDirectNotification(
                    userId = userId,
                    title = title,
                    message = message,
                    type = type,
                    actionUrl = url,
                    actionLabel = label
                )
                showNotificationDialog = false
                notificationTargetUser = null
            }
        )
    }
}
