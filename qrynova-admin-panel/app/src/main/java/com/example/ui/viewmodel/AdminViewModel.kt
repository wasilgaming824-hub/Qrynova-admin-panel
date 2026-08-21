package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.QrynovaDataRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class AdminNavigationSection(val title: String, val iconName: String, val badgeCount: Int = 0) {
    DASHBOARD("Dashboard", "dashboard"),
    USERS("Users", "people"),
    GENERATION_REQUESTS("Generation Requests", "auto_awesome"),
    REFERRALS("Referral Manager", "share"),
    SUPPORT_SETTINGS("Support & Community", "contact_support"),
    CREDITS("Credit Rules", "monetization_on"),
    ADS("Advertisement Gate", "smart_display"),
    GENERATION_SETTINGS("Generation Config", "tune"),
    NOTIFICATIONS("Notifications", "notifications"),
    SYSTEM_SETTINGS("System Settings", "settings"),
    AUDIT_LOGS("Activity Logs", "receipt_long"),
    SYSTEM_STATUS("Firebase Health", "health_and_safety"),
    SETUP_DOCS("Firebase Setup Guide", "menu_book"),
    USER_APP_SIMULATOR("User App Live Preview", "phone_iphone")
}

data class AdminUiState(
    val isAuthenticated: Boolean = false,
    val currentAdmin: AdminAccount? = null,
    val isAuthenticating: Boolean = false,
    val authError: String? = null,
    val unauthorizedUid: String? = null,
    val unauthorizedEmail: String? = null,
    val authorizationRequired: Boolean = false,
    val currentSection: AdminNavigationSection = AdminNavigationSection.DASHBOARD,
    val isDrawerOpen: Boolean = false,
    val snackBarMessage: String? = null,
    val selectedUser: UserAccount? = null,
    val selectedRequest: GenerationRequest? = null,
    val showCreditDialog: Boolean = false,
    val showDriveDeliveryDialog: Boolean = false,
    val showNotificationDialog: Boolean = false,
    val showUserStatusDialog: Boolean = false,
    val showAddNoteDialog: Boolean = false,
    val showAdEditDialog: Boolean = false,
    val selectedAdPlacementKey: String? = null,
    // Live Collections
    val users: List<UserAccount> = emptyList(),
    val usersError: String? = null,
    val isUsersLoading: Boolean = false,
    val requests: List<GenerationRequest> = emptyList(),
    val referralRecords: List<ReferralRecord> = emptyList(),
    val creditSettings: CreditSettings = CreditSettings(),
    val adSettings: AdSettings = AdSettings(),
    val generationSettings: GenerationSettings = GenerationSettings(),
    val systemSettings: AppSystemSettings = AppSystemSettings(),
    val referralSettings: ReferralSettings = ReferralSettings(),
    val aiLimitsSettings: AILimitsSettings = AILimitsSettings(),
    val supportSettings: SupportSettings = SupportSettings(),
    val notifications: List<AppNotification> = emptyList(),
    val auditLogs: List<AdminAuditLog> = emptyList(),
    val creditTransactions: List<CreditTransaction> = emptyList(),
    val diagnosticInfo: com.example.data.firebase.AuthDiagnosticInfo = com.example.data.firebase.AuthDiagnosticInfo(),
    // Filters
    val userSearchQuery: String = "",
    val userStatusFilter: UserStatus? = null,
    val userInactivityDaysFilter: Int? = null,
    val requestSearchQuery: String = "",
    val requestStatusFilter: RequestStatus? = null,
    val requestTypeFilter: RequestType? = null,
    val referralSearchQuery: String = ""
) {
    val filteredUsers: List<UserAccount>
        get() {
            val now = System.currentTimeMillis()
            return users.filter { user ->
                val matchesQuery = userSearchQuery.isBlank() ||
                        user.name.contains(userSearchQuery, ignoreCase = true) ||
                        user.nickname.contains(userSearchQuery, ignoreCase = true) ||
                        user.email.contains(userSearchQuery, ignoreCase = true) ||
                        user.uid.contains(userSearchQuery, ignoreCase = true)

                val matchesStatus = userStatusFilter == null || user.status == userStatusFilter

                val matchesInactivity = if (userInactivityDaysFilter == null) true else {
                    val diffDays = (now - user.lastActiveAt) / (1000L * 60 * 60 * 24)
                    diffDays >= userInactivityDaysFilter
                }

                matchesQuery && matchesStatus && matchesInactivity
            }
        }

    val filteredRequests: List<GenerationRequest>
        get() {
            return requests.filter { req ->
                val matchesQuery = requestSearchQuery.isBlank() ||
                        req.requestId.contains(requestSearchQuery, ignoreCase = true) ||
                        req.userName.contains(requestSearchQuery, ignoreCase = true) ||
                        req.userEmail.contains(requestSearchQuery, ignoreCase = true) ||
                        req.prompt.contains(requestSearchQuery, ignoreCase = true)

                val matchesStatus = requestStatusFilter == null || req.status == requestStatusFilter
                val matchesType = requestTypeFilter == null || req.requestType == requestTypeFilter

                matchesQuery && matchesStatus && matchesType
            }
        }

    val filteredReferralRecords: List<ReferralRecord>
        get() {
            return referralRecords.filter { ref ->
                referralSearchQuery.isBlank() ||
                        ref.referrerName.contains(referralSearchQuery, ignoreCase = true) ||
                        ref.referrerEmail.contains(referralSearchQuery, ignoreCase = true) ||
                        ref.referredName.contains(referralSearchQuery, ignoreCase = true) ||
                        ref.referredEmail.contains(referralSearchQuery, ignoreCase = true) ||
                        ref.referralCode.contains(referralSearchQuery, ignoreCase = true)
            }
        }
}

class AdminViewModel(
    private val repository: QrynovaDataRepository = QrynovaDataRepository.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        // Collect flows from repository into UI state
        viewModelScope.launch {
            repository.currentAdmin.collect { admin ->
                _uiState.update { state ->
                    if (admin != null) {
                        android.util.Log.i("AdminViewModel", "[STATE TRANSITION] Logged in as Admin: ${admin.email} (UID: ${admin.uid}, Role: ${admin.role})")
                        state.copy(
                            currentAdmin = admin,
                            isAuthenticated = true,
                            isAuthenticating = false,
                            authError = null,
                            unauthorizedUid = null,
                            unauthorizedEmail = null,
                            authorizationRequired = false
                        )
                    } else {
                        android.util.Log.i("AdminViewModel", "[STATE TRANSITION] No current admin active")
                        state.copy(
                            currentAdmin = null,
                            isAuthenticated = false
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            repository.users.collect { users ->
                _uiState.update { it.copy(users = users) }
            }
        }
        viewModelScope.launch {
            repository.usersError.collect { err ->
                _uiState.update { it.copy(usersError = err) }
            }
        }
        viewModelScope.launch {
            repository.isUsersLoading.collect { loading ->
                _uiState.update { it.copy(isUsersLoading = loading) }
            }
        }
        viewModelScope.launch {
            repository.generationRequests.collect { reqs ->
                _uiState.update { it.copy(requests = reqs) }
            }
        }
        viewModelScope.launch {
            repository.referralRecords.collect { records ->
                _uiState.update { it.copy(referralRecords = records) }
            }
        }
        viewModelScope.launch {
            repository.creditSettings.collect { cs ->
                _uiState.update { it.copy(creditSettings = cs) }
            }
        }
        viewModelScope.launch {
            repository.adSettings.collect { ads ->
                _uiState.update { it.copy(adSettings = ads) }
            }
        }
        viewModelScope.launch {
            repository.generationSettings.collect { gs ->
                _uiState.update { it.copy(generationSettings = gs) }
            }
        }
        viewModelScope.launch {
            repository.systemSettings.collect { ss ->
                _uiState.update { it.copy(systemSettings = ss) }
            }
        }
        viewModelScope.launch {
            repository.referralSettings.collect { rs ->
                _uiState.update { it.copy(referralSettings = rs) }
            }
        }
        viewModelScope.launch {
            repository.aiLimitsSettings.collect { als ->
                _uiState.update { it.copy(aiLimitsSettings = als) }
            }
        }
        viewModelScope.launch {
            repository.supportSettings.collect { ss ->
                _uiState.update { it.copy(supportSettings = ss) }
            }
        }
        viewModelScope.launch {
            repository.notifications.collect { notifs ->
                _uiState.update { it.copy(notifications = notifs) }
            }
        }
        viewModelScope.launch {
            repository.auditLogs.collect { logs ->
                _uiState.update { it.copy(auditLogs = logs) }
            }
        }
        viewModelScope.launch {
            repository.creditTransactions.collect { txs ->
                _uiState.update { it.copy(creditTransactions = txs) }
            }
        }
        viewModelScope.launch {
            com.example.data.firebase.FirebaseManager.getInstance().diagnosticInfo.collect { diag ->
                _uiState.update { it.copy(diagnosticInfo = diag) }
            }
        }
        // Check and restore existing authenticated admin session
        viewModelScope.launch {
            repository.restoreSessionIfAuthorized()
        }
    }

    // Navigation & UI Actions
    fun selectSection(section: AdminNavigationSection) {
        _uiState.update { it.copy(currentSection = section, isDrawerOpen = false) }
    }

    fun navigateTo(section: AdminNavigationSection) {
        selectSection(section)
    }

    fun setDrawerOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isDrawerOpen = isOpen) }
    }

    fun showToast(message: String) {
        _uiState.update { it.copy(snackBarMessage = message) }
    }

    fun clearSnackBar() {
        _uiState.update { it.copy(snackBarMessage = null) }
    }

    // Filter Actions
    fun setUserSearchQuery(query: String) {
        _uiState.update { it.copy(userSearchQuery = query) }
    }

    fun setUserStatusFilter(status: UserStatus?) {
        _uiState.update { it.copy(userStatusFilter = status) }
    }

    fun setUserInactivityFilter(days: Int?) {
        _uiState.update { it.copy(userInactivityDaysFilter = days) }
    }

    fun setRequestSearchQuery(query: String) {
        _uiState.update { it.copy(requestSearchQuery = query) }
    }

    fun setRequestStatusFilter(status: RequestStatus?) {
        _uiState.update { it.copy(requestStatusFilter = status) }
    }

    fun setRequestTypeFilter(type: RequestType?) {
        _uiState.update { it.copy(requestTypeFilter = type) }
    }

    fun setReferralSearchQuery(query: String) {
        _uiState.update { it.copy(referralSearchQuery = query) }
    }

    // User Actions
    fun selectUser(user: UserAccount?) {
        _uiState.update { it.copy(selectedUser = user) }
    }

    fun adjustUserCredits(userId: String, amount: Int, type: TransactionType, reason: String) {
        viewModelScope.launch {
            val result = repository.adjustUserCredits(userId, amount, type, reason)
            result.onSuccess { updatedUser ->
                showToast("Credits successfully updated (${updatedUser.credits} credits)")
            }.onFailure {
                showToast(it.message ?: "Credit adjustment failed")
            }
        }
    }

    fun updateUserStatus(userId: String, status: UserStatus, reason: String = "Admin status update") {
        repository.updateUserStatus(userId, status, reason)
        showToast("Account status set to ${status.name}")
    }

    fun addUserNote(userId: String, note: String) {
        repository.addUserNote(userId, note)
        showToast("Admin note saved")
    }

    fun createUser(
        name: String,
        email: String,
        uid: String,
        startingCredits: Int
    ) {
        repository.createNewUser(name, email, uid, startingCredits) { success, msg ->
            showToast(msg)
        }
    }

    fun lookupUserByUidOrEmail(query: String) {
        com.example.data.firebase.FirebaseManager.getInstance().fetchUserByUidOrEmail(query) { user, error ->
            if (user != null) {
                showToast("Loaded user '${user.name}' (${user.credits} credits)")
                selectUser(user)
            } else {
                showToast(error ?: "User not found")
            }
        }
    }

    fun seedSampleUsers() {
        com.example.data.firebase.FirebaseManager.getInstance().seedSampleUsersToFirestore { count, msg ->
            showToast(msg)
        }
    }

    fun dismissUsersError() {
        repository.setUsersError(null)
    }

    fun bootstrapAdminDocument(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        com.example.data.firebase.FirebaseManager.getInstance().bootstrapAdminDocumentInFirestore { success, msg ->
            showToast(msg)
            onComplete(success, msg)
        }
    }

    // Requests Actions
    fun updateRequestStatus(requestId: String, status: RequestStatus, note: String = "") {
        repository.updateRequestStatus(requestId, status, note)
        showToast("Request status updated to $status")
    }

    fun submitGoogleDriveResult(requestId: String, googleDriveUrl: String, adminMessage: String) {
        viewModelScope.launch {
            val result = repository.completeGenerationWithGoogleDrive(requestId, googleDriveUrl, adminMessage)
            result.onSuccess {
                showToast("Generation delivered via Google Drive successfully!")
            }.onFailure {
                showToast(it.message ?: "Failed to deliver result")
            }
        }
    }

    // Settings Updates
    fun updateCreditSettings(settings: CreditSettings) {
        repository.updateCreditSettings(settings)
        showToast("Credit pricing saved to Firestore")
    }

    fun updateGenerationSettings(settings: GenerationSettings) {
        repository.updateGenerationSettings(settings)
        showToast("Generation pipeline updated in Firestore")
    }

    fun updateAdSettings(settings: AdSettings) {
        repository.updateAdSettings(settings)
        showToast("Ad Gate configuration saved in Firestore")
    }

    fun updateSystemSettings(settings: AppSystemSettings) {
        repository.updateSystemSettings(settings)
        showToast("System settings updated in Firestore")
    }

    fun updateReferralSettings(settings: ReferralSettings) {
        repository.updateReferralSettings(settings)
        showToast("Referral reward settings saved to Firestore (${settings.rewardCredits} Credits)")
    }

    fun updateAILimitsSettings(settings: AILimitsSettings) {
        repository.updateAILimitsSettings(settings)
        showToast("AI Generation limits synced to Firestore")
    }

    fun updateSupportSettings(settings: SupportSettings) {
        repository.updateSupportSettings(settings)
        showToast("Support & Community channels synced to Firestore")
    }

    // Notifications
    fun sendBroadcastNotification(title: String, message: String, type: NotificationType, actionUrl: String, actionLabel: String) {
        repository.sendBroadcastNotification(title, message, type, actionUrl, actionLabel)
        showToast("Global announcement broadcast to all users")
    }

    fun sendDirectNotification(userId: String, title: String, message: String, type: NotificationType, actionUrl: String, actionLabel: String) {
        repository.sendUserNotification(userId, title, message, type, actionUrl, actionLabel)
        showToast("Direct notification sent to user")
    }

    // Auth
    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isAuthenticating = true,
                    authError = null,
                    unauthorizedUid = null,
                    unauthorizedEmail = null,
                    authorizationRequired = false
                )
            }
            val result = repository.loginAdmin(email, pass)
            result.onSuccess { admin ->
                android.util.Log.i("AdminViewModel", "AUTH RESULT = SUCCESS | IS AUTHENTICATED = true | CURRENT ADMIN = ${admin.email}")
                _uiState.update { state ->
                    state.copy(
                        isAuthenticated = true,
                        currentAdmin = admin,
                        isAuthenticating = false,
                        authError = null,
                        unauthorizedUid = null,
                        unauthorizedEmail = null,
                        authorizationRequired = false
                    )
                }
                showToast("Authenticated as ${admin.displayName}")
            }.onFailure { error ->
                if (error is com.example.data.firebase.UnauthorizedAdminException) {
                    android.util.Log.w("AdminViewModel", "AUTH RESULT = FAILURE (UNAUTHORIZED) | IS AUTHENTICATED = false | UNAUTHORIZED UID = ${error.uid}")
                    _uiState.update { state ->
                        state.copy(
                            isAuthenticated = false,
                            currentAdmin = null,
                            isAuthenticating = false,
                            authError = error.message,
                            unauthorizedUid = error.uid,
                            unauthorizedEmail = error.email,
                            authorizationRequired = true
                        )
                    }
                } else {
                    val msg = error.message ?: "Authentication failed"
                    android.util.Log.w("AdminViewModel", "AUTH RESULT = FAILURE | IS AUTHENTICATED = false | AUTH ERROR = $msg")
                    _uiState.update { state ->
                        state.copy(
                            isAuthenticated = false,
                            currentAdmin = null,
                            isAuthenticating = false,
                            authError = msg,
                            unauthorizedUid = null,
                            unauthorizedEmail = null,
                            authorizationRequired = false
                        )
                    }
                    showToast(msg)
                }
            }
        }
    }

    fun clearAuthError() {
        _uiState.update {
            it.copy(
                authError = null,
                unauthorizedUid = null,
                unauthorizedEmail = null,
                authorizationRequired = false
            )
        }
    }

    fun logout() {
        repository.logoutAdmin()
        _uiState.update {
            it.copy(
                isAuthenticated = false,
                currentAdmin = null,
                authError = null,
                unauthorizedUid = null,
                unauthorizedEmail = null,
                authorizationRequired = false
            )
        }
        showToast("Signed out safely")
    }

    // User App Simulator
    fun submitSimulatedUserGeneration(
        userId: String,
        userName: String,
        userEmail: String,
        prompt: String,
        type: RequestType,
        resolution: String,
        aspectRatio: String
    ) {
        viewModelScope.launch {
            val result = repository.simulateClientSubmitRequest(
                userId = userId,
                type = type,
                prompt = prompt,
                resolution = resolution,
                aspectRatio = aspectRatio
            )
            result.onSuccess {
                showToast("Simulated Job ${it.requestId} submitted to Firestore queue!")
            }.onFailure {
                showToast("Submission error: ${it.message}")
            }
        }
    }

    fun refreshUsers() {
        viewModelScope.launch {
            com.example.data.firebase.FirebaseManager.getInstance().triggerDirectUsersRead()
        }
    }

    fun claimReferral(userId: String, referralCode: String) {
        claimReferralReward(userId, referralCode)
    }

    fun claimReferralReward(referredUserId: String, referralCode: String) {
        viewModelScope.launch {
            val result = repository.processReferralClaim(referredUserId, referralCode)
            result.onSuccess { rewardCredits ->
                showToast("Referral rewarded! +$rewardCredits QRYNOVA Credits added to referrer.")
            }.onFailure { error ->
                showToast("Referral claim error: ${error.message}")
            }
        }
    }
}
