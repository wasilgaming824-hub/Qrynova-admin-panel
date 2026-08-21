package com.example.data.repository

import com.example.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class QrynovaDataRepository private constructor() {

    // Current Auth State (Secured: null by default until authorized in admins/{uid})
    private val _currentAdmin = MutableStateFlow<AdminAccount?>(null)
    val currentAdmin: StateFlow<AdminAccount?> = _currentAdmin.asStateFlow()

    // Config & Settings State
    private val _systemSettings = MutableStateFlow(AppSystemSettings())
    val systemSettings: StateFlow<AppSystemSettings> = _systemSettings.asStateFlow()

    private val _creditSettings = MutableStateFlow(CreditSettings())
    val creditSettings: StateFlow<CreditSettings> = _creditSettings.asStateFlow()

    private val _generationSettings = MutableStateFlow(GenerationSettings())
    val generationSettings: StateFlow<GenerationSettings> = _generationSettings.asStateFlow()

    private val _adSettings = MutableStateFlow(AdSettings())
    val adSettings: StateFlow<AdSettings> = _adSettings.asStateFlow()

    private val _referralSettings = MutableStateFlow(ReferralSettings())
    val referralSettings: StateFlow<ReferralSettings> = _referralSettings.asStateFlow()

    private val _aiLimitsSettings = MutableStateFlow(AILimitsSettings())
    val aiLimitsSettings: StateFlow<AILimitsSettings> = _aiLimitsSettings.asStateFlow()

    private val _supportSettings = MutableStateFlow(SupportSettings())
    val supportSettings: StateFlow<SupportSettings> = _supportSettings.asStateFlow()

    // Data Collections
    private val _users = MutableStateFlow<List<UserAccount>>(emptyList())
    val users: StateFlow<List<UserAccount>> = _users.asStateFlow()

    private val _usersError = MutableStateFlow<String?>(null)
    val usersError: StateFlow<String?> = _usersError.asStateFlow()

    private val _isUsersLoading = MutableStateFlow<Boolean>(false)
    val isUsersLoading: StateFlow<Boolean> = _isUsersLoading.asStateFlow()

    fun setUsersError(error: String?) {
        _usersError.value = error
    }

    fun setIsUsersLoading(loading: Boolean) {
        _isUsersLoading.value = loading
    }

    private val _generationRequests = MutableStateFlow<List<GenerationRequest>>(emptyList())
    val generationRequests: StateFlow<List<GenerationRequest>> = _generationRequests.asStateFlow()

    private val _referralRecords = MutableStateFlow<List<ReferralRecord>>(emptyList())
    val referralRecords: StateFlow<List<ReferralRecord>> = _referralRecords.asStateFlow()

    private val _creditTransactions = MutableStateFlow<List<CreditTransaction>>(emptyList())
    val creditTransactions: StateFlow<List<CreditTransaction>> = _creditTransactions.asStateFlow()

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<AdminAuditLog>>(emptyList())
    val auditLogs: StateFlow<List<AdminAuditLog>> = _auditLogs.asStateFlow()

    private val _systemHealth = MutableStateFlow(SystemHealthState())
    val systemHealth: StateFlow<SystemHealthState> = _systemHealth.asStateFlow()

    // Admin Auth Actions
    suspend fun loginAdmin(email: String, pass: String): Result<AdminAccount> {
        if (email.isBlank() || pass.isBlank()) {
            return Result.failure(IllegalArgumentException("Email and password cannot be empty"))
        }

        // Authenticate with Firebase Auth and verify against admins/{uid} in Firestore
        val authResult = com.example.data.firebase.FirebaseManager.getInstance().authenticateAndAuthorizeAdmin(email, pass)
        
        return if (authResult.isSuccess) {
            val admin = authResult.getOrNull()!!
            _currentAdmin.value = admin
            logAdminAction("ADMIN_LOGIN", "ADMIN", admin.uid, "Admin logged into QRYNOVA console: ${admin.email} (Role: ${admin.role.name})")
            Result.success(admin)
        } else {
            val error = authResult.exceptionOrNull() ?: Exception("Authentication failed")
            _currentAdmin.value = null
            Result.failure(error)
        }
    }

    suspend fun restoreSessionIfAuthorized(): Result<AdminAccount?> {
        val result = com.example.data.firebase.FirebaseManager.getInstance().checkExistingAdminSession()
        return if (result.isSuccess) {
            val admin = result.getOrNull()
            _currentAdmin.value = admin
            Result.success(admin)
        } else {
            _currentAdmin.value = null
            Result.success(null)
        }
    }

    fun logoutAdmin() {
        val current = _currentAdmin.value
        if (current != null) {
            logAdminAction("ADMIN_LOGOUT", "ADMIN", current.uid, "Admin logged out: ${current.email}")
        }
        com.example.data.firebase.FirebaseManager.getInstance().signOutAdmin()
        _currentAdmin.value = null
        _users.value = emptyList()
        _generationRequests.value = emptyList()
    }

    // Logging helper
    private fun logAdminAction(action: String, targetType: String, targetId: String, description: String) {
        val current = _currentAdmin.value
        val log = AdminAuditLog(
            logId = "log_" + UUID.randomUUID().toString().take(8),
            adminId = current?.uid ?: "sys_operator",
            adminEmail = current?.email ?: "system@qrynova.ai",
            action = action,
            targetType = targetType,
            targetId = targetId,
            description = description,
            timestamp = System.currentTimeMillis()
        )
        _auditLogs.value = listOf(log) + _auditLogs.value
    }

    // User Operations
    fun updateUserStatus(userId: String, newStatus: UserStatus, reason: String = "") {
        _users.value = _users.value.map { user ->
            if (user.uid == userId) {
                user.copy(status = newStatus)
            } else user
        }
        com.example.data.firebase.FirebaseManager.getInstance().syncUserStatus(userId, newStatus)
        logAdminAction("UPDATE_USER_STATUS", "USER", userId, "Changed user status to $newStatus. Reason: ${reason.ifEmpty { "Admin manual update" }}")
    }

    fun addUserNote(userId: String, noteContent: String) {
        if (noteContent.isBlank()) return
        val current = _currentAdmin.value
        val newNote = AdminNote(
            id = "note_" + UUID.randomUUID().toString().take(6),
            adminId = current?.uid ?: "admin_001",
            adminEmail = current?.email ?: "admin@qrynova.ai",
            note = noteContent.trim(),
            timestamp = System.currentTimeMillis()
        )
        _users.value = _users.value.map { user ->
            if (user.uid == userId) {
                user.copy(adminNotes = listOf(newNote) + user.adminNotes)
            } else user
        }
        logAdminAction("ADD_USER_NOTE", "USER", userId, "Added internal admin note to user account: \"${noteContent.take(40)}...\"")
    }

    // Credit Operations
    fun adjustUserCredits(userId: String, adjustment: Int, type: TransactionType, reason: String): Result<UserAccount> {
        val user = _users.value.find { it.uid == userId } ?: return Result.failure(Exception("User not found"))
        val previousCredits = user.credits
        val newCredits = when (type) {
            TransactionType.MANUAL_ADD -> previousCredits + adjustment
            TransactionType.MANUAL_DEDUCT -> (previousCredits - adjustment).coerceAtLeast(0)
            TransactionType.MANUAL_SET -> adjustment.coerceAtLeast(0)
            else -> previousCredits + adjustment
        }

        val updatedUser = user.copy(
            credits = newCredits,
            totalCreditsGranted = if (newCredits > previousCredits) user.totalCreditsGranted + (newCredits - previousCredits) else user.totalCreditsGranted,
            totalCreditsConsumed = if (newCredits < previousCredits) user.totalCreditsConsumed + (previousCredits - newCredits) else user.totalCreditsConsumed
        )

        _users.value = _users.value.map { if (it.uid == userId) updatedUser else it }
        com.example.data.firebase.FirebaseManager.getInstance().syncUserCredits(
            userId = userId,
            newCredits = newCredits,
            totalGranted = updatedUser.totalCreditsGranted,
            totalConsumed = updatedUser.totalCreditsConsumed
        )

        val current = _currentAdmin.value
        val tx = CreditTransaction(
            transactionId = "tx_" + UUID.randomUUID().toString().take(8),
            userId = userId,
            userEmail = user.email,
            amount = if (type == TransactionType.MANUAL_SET) newCredits else adjustment,
            previousBalance = previousCredits,
            newBalance = newCredits,
            type = type,
            reason = reason.ifEmpty { "Manual admin credit adjustment" },
            adminId = current?.uid ?: "admin_001",
            adminEmail = current?.email ?: "admin@qrynova.ai",
            timestamp = System.currentTimeMillis()
        )
        _creditTransactions.value = listOf(tx) + _creditTransactions.value
        com.example.data.firebase.FirebaseManager.getInstance().syncCreditTransaction(tx)

        logAdminAction(
            "CREDIT_ADJUSTMENT",
            "USER",
            userId,
            "Adjusted credits for ${user.name} (${type.name}): $previousCredits -> $newCredits credits. Reason: $reason"
        )
        return Result.success(updatedUser)
    }

    fun updateCreditSettings(newSettings: CreditSettings) {
        val current = _currentAdmin.value
        val updated = newSettings.copy(
            lastUpdated = System.currentTimeMillis(),
            updatedByAdmin = current?.email ?: "admin@qrynova.ai"
        )
        _creditSettings.value = updated
        com.example.data.firebase.FirebaseManager.getInstance().syncCreditSettings(updated)
        logAdminAction(
            "UPDATE_CREDIT_SETTINGS",
            "SETTINGS",
            "settings/credits",
            "Updated credit rules: Starting=${updated.newUserStartingCredits}, ImgCost=${updated.imageGenerationCost}, VidCost=${updated.videoGenerationCost}, I2VCost=${updated.imageToVideoCost}"
        )
    }

    // Generation Settings
    fun updateGenerationSettings(newSettings: GenerationSettings) {
        val updated = newSettings.copy(lastUpdated = System.currentTimeMillis())
        _generationSettings.value = updated
        com.example.data.firebase.FirebaseManager.getInstance().syncGenerationSettings(updated)
        logAdminAction(
            "UPDATE_GENERATION_SETTINGS",
            "SETTINGS",
            "settings/generation",
            "Updated generation pipeline rules: Image=${updated.image.enabled}, Video=${updated.video.enabled}, ImageToVideo=${updated.imageToVideo.enabled}"
        )
    }

    // Ad Management
    fun updateAdSettings(newSettings: AdSettings) {
        val updated = newSettings.copy(lastUpdated = System.currentTimeMillis())
        _adSettings.value = updated
        com.example.data.firebase.FirebaseManager.getInstance().syncAdSettings(updated)
        logAdminAction(
            "UPDATE_AD_SETTINGS",
            "SETTINGS",
            "settings/ads",
            "Updated ad gate rules: GlobalAds=${updated.globalAdsEnabled}, WatchDuration=${updated.defaultWatchDurationSeconds}s"
        )
    }

    // System Settings
    fun updateSystemSettings(newSettings: AppSystemSettings) {
        val updated = newSettings.copy(lastUpdated = System.currentTimeMillis())
        _systemSettings.value = updated
        com.example.data.firebase.FirebaseManager.getInstance().syncSystemSettings(updated)
        logAdminAction(
            "UPDATE_SYSTEM_SETTINGS",
            "SETTINGS",
            "settings/app",
            "Updated system app config: AppName=${updated.appName}, MaintenanceMode=${updated.maintenanceMode}, Reg=${updated.registrationEnabled}"
        )
    }

    // Generation Request Management & Google Drive Result Delivery
    fun updateRequestStatus(requestId: String, status: RequestStatus, adminNote: String = "") {
        _generationRequests.value = _generationRequests.value.map { req ->
            if (req.requestId == requestId) {
                req.copy(
                    status = status,
                    adminNotes = if (adminNote.isNotBlank()) adminNote else req.adminNotes,
                    updatedAt = System.currentTimeMillis()
                )
            } else req
        }
        com.example.data.firebase.FirebaseManager.getInstance().syncRequestStatus(requestId, status, adminNote)
        logAdminAction("UPDATE_REQUEST_STATUS", "REQUEST", requestId, "Updated request status to $status. Note: $adminNote")
    }

    fun completeGenerationWithGoogleDrive(requestId: String, googleDriveUrl: String, adminMessage: String): Result<GenerationRequest> {
        val cleanUrl = googleDriveUrl.trim()
        if (cleanUrl.isBlank()) {
            return Result.failure(IllegalArgumentException("Google Drive link cannot be empty"))
        }

        var foundReq: GenerationRequest? = null
        _generationRequests.value = _generationRequests.value.map { req ->
            if (req.requestId == requestId) {
                val completed = req.copy(
                    status = RequestStatus.COMPLETED,
                    googleDriveUrl = cleanUrl,
                    adminMessage = adminMessage.ifEmpty { "Your generation processing is completed successfully." },
                    completedAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                foundReq = completed
                completed
            } else req
        }

        val completedRequest = foundReq ?: return Result.failure(Exception("Request not found"))

        // Sync to Firestore generationRequests/{requestId}
        val current = _currentAdmin.value
        com.example.data.firebase.FirebaseManager.getInstance().syncGenerationDelivery(
            requestId = requestId,
            googleDriveUrl = cleanUrl,
            adminNotes = adminMessage,
            adminId = current?.uid ?: "admin_001",
            adminEmail = current?.email ?: "admin@qrynova.ai"
        )

        // Update user stats
        _users.value = _users.value.map { user ->
            if (user.uid == completedRequest.userId) {
                user.copy(
                    completedGenerations = user.completedGenerations + 1,
                    pendingGenerations = (user.pendingGenerations - 1).coerceAtLeast(0)
                )
            } else user
        }

        // Trigger in-app notification for the user
        val notif = AppNotification(
            id = "notif_" + UUID.randomUUID().toString().take(8),
            title = "Your ${completedRequest.requestType.name.replace("_", " ")} is ready!",
            message = adminMessage.ifEmpty { "Your processing is completed successfully. Click to download from Google Drive." },
            type = NotificationType.GENERATION,
            targetUserId = completedRequest.userId,
            targetUserEmail = completedRequest.userEmail,
            actionUrl = cleanUrl,
            actionLabel = "Open Google Drive Result",
            isRead = false,
            createdAt = System.currentTimeMillis(),
            senderAdminEmail = _currentAdmin.value?.email ?: "admin@qrynova.ai"
        )
        _notifications.value = listOf(notif) + _notifications.value
        com.example.data.firebase.FirebaseManager.getInstance().syncNotification(notif)

        logAdminAction(
            "COMPLETE_GENERATION_GOOGLE_DRIVE",
            "REQUEST",
            requestId,
            "Delivered generated result via Google Drive ($cleanUrl) to user ${completedRequest.userEmail}."
        )

        return Result.success(completedRequest)
    }

    // Notifications
    fun sendBroadcastNotification(title: String, message: String, type: NotificationType, actionUrl: String = "", actionLabel: String = "") {
        val current = _currentAdmin.value
        val notif = AppNotification(
            id = "notif_" + UUID.randomUUID().toString().take(8),
            title = title.trim(),
            message = message.trim(),
            type = type,
            targetUserId = null,
            targetUserEmail = null,
            actionUrl = actionUrl.trim(),
            actionLabel = actionLabel.trim(),
            isRead = false,
            createdAt = System.currentTimeMillis(),
            senderAdminEmail = current?.email ?: "admin@qrynova.ai"
        )
        _notifications.value = listOf(notif) + _notifications.value
        com.example.data.firebase.FirebaseManager.getInstance().syncNotification(notif)
        logAdminAction("BROADCAST_NOTIFICATION", "NOTIFICATION", notif.id, "Sent global announcement: \"$title\" (${type.name})")
    }

    fun sendUserNotification(userId: String, title: String, message: String, type: NotificationType, actionUrl: String = "", actionLabel: String = "") {
        val user = _users.value.find { it.uid == userId }
        val current = _currentAdmin.value
        val notif = AppNotification(
            id = "notif_" + UUID.randomUUID().toString().take(8),
            title = title.trim(),
            message = message.trim(),
            type = type,
            targetUserId = userId,
            targetUserEmail = user?.email,
            actionUrl = actionUrl.trim(),
            actionLabel = actionLabel.trim(),
            isRead = false,
            createdAt = System.currentTimeMillis(),
            senderAdminEmail = current?.email ?: "admin@qrynova.ai"
        )
        _notifications.value = listOf(notif) + _notifications.value
        com.example.data.firebase.FirebaseManager.getInstance().syncNotification(notif)
        logAdminAction("SEND_USER_NOTIFICATION", "NOTIFICATION", notif.id, "Sent direct notification to ${user?.email ?: userId}: \"$title\"")
    }

    // Live Client Simulator Bridge (Allows testing the User App experience live in emulator)
    fun simulateClientSubmitRequest(
        userId: String,
        type: RequestType,
        prompt: String,
        resolution: String,
        aspectRatio: String,
        durationSeconds: Int = 5,
        sourceImageUrl: String = ""
    ): Result<GenerationRequest> {
        val user = _users.value.find { it.uid == userId } ?: return Result.failure(Exception("User not found"))
        val cost = when (type) {
            RequestType.IMAGE -> _creditSettings.value.imageGenerationCost
            RequestType.TEXT_TO_VIDEO -> _creditSettings.value.videoGenerationCost
            RequestType.IMAGE_TO_VIDEO -> _creditSettings.value.imageToVideoCost
        }

        if (user.credits < cost) {
            return Result.failure(Exception("Insufficient credits. Required: $cost, Available: ${user.credits}"))
        }

        val updatedCredits = user.credits - cost
        _users.value = _users.value.map {
            if (it.uid == userId) {
                it.copy(
                    credits = updatedCredits,
                    totalCreditsConsumed = it.totalCreditsConsumed + cost,
                    totalGenerations = it.totalGenerations + 1,
                    imageGenerations = if (type == RequestType.IMAGE) it.imageGenerations + 1 else it.imageGenerations,
                    videoGenerations = if (type == RequestType.TEXT_TO_VIDEO) it.videoGenerations + 1 else it.videoGenerations,
                    imageToVideoGenerations = if (type == RequestType.IMAGE_TO_VIDEO) it.imageToVideoGenerations + 1 else it.imageToVideoGenerations,
                    pendingGenerations = it.pendingGenerations + 1,
                    lastActiveAt = System.currentTimeMillis()
                )
            } else it
        }

        val req = GenerationRequest(
            requestId = "REQ-" + (100000 + (Math.random() * 900000).toInt()),
            userId = userId,
            userEmail = user.email,
            userName = user.name,
            requestType = type,
            prompt = prompt,
            resolution = resolution,
            aspectRatio = aspectRatio,
            pixelSize = resolution,
            durationSeconds = durationSeconds,
            sourceImageUrl = sourceImageUrl,
            creditCost = cost,
            adCompleted = true,
            status = RequestStatus.PENDING,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        _generationRequests.value = listOf(req) + _generationRequests.value

        // Log transaction
        val tx = CreditTransaction(
            transactionId = "tx_" + UUID.randomUUID().toString().take(8),
            userId = userId,
            userEmail = user.email,
            amount = cost,
            previousBalance = user.credits,
            newBalance = updatedCredits,
            type = TransactionType.GENERATION_SPEND,
            reason = "Generation request ${req.requestId} (${type.name})",
            adminId = "client_system",
            adminEmail = "system@qrynova.ai",
            timestamp = System.currentTimeMillis()
        )
        _creditTransactions.value = listOf(tx) + _creditTransactions.value
        com.example.data.firebase.FirebaseManager.getInstance().syncCreditTransaction(tx)

        return Result.success(req)
    }

    /**
     * Replaces or merges real-time users streamed directly from Cloud Firestore users/{uid}
     */
    fun updateUsersFromFirestore(remoteUsers: List<UserAccount>) {
        if (remoteUsers.isEmpty() && _users.value.isNotEmpty()) {
            return
        }
        val current = _users.value.associateBy { it.uid }
        val merged = remoteUsers.map { remote ->
            val existing = current[remote.uid]
            if (existing != null && existing.adminNotes.isNotEmpty() && remote.adminNotes.isEmpty()) {
                remote.copy(adminNotes = existing.adminNotes)
            } else {
                remote
            }
        }
        _users.value = merged
    }

    /**
     * Adds or updates a single user in the local repository list
     */
    fun addOrUpdateLocalUser(user: UserAccount) {
        val current = _users.value.toMutableList()
        val index = current.indexOfFirst { it.uid == user.uid }
        if (index >= 0) {
            current[index] = user
        } else {
            current.add(0, user)
        }
        _users.value = current
    }

    /**
     * Registers a new user into repository and pushes to Firestore
     */
    fun createNewUser(
        name: String,
        email: String,
        uid: String,
        startingCredits: Int,
        onComplete: (Boolean, String) -> Unit
    ) {
        val newUid = uid.ifBlank { "usr_" + UUID.randomUUID().toString().take(8) }
        val displayName = name.trim().ifEmpty { email.substringBefore("@").replaceFirstChar { it.uppercase() } }
        val newUser = UserAccount(
            uid = newUid,
            email = email.trim(),
            name = displayName,
            nickname = displayName.lowercase().replace(" ", "_"),
            profileImageUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
            status = UserStatus.ACTIVE,
            credits = startingCredits,
            totalCreditsGranted = startingCredits,
            totalCreditsConsumed = 0,
            totalGenerations = 0,
            referralCode = "REF" + (1000..9999).random(),
            referredBy = "",
            createdAt = System.currentTimeMillis(),
            lastActiveAt = System.currentTimeMillis(),
            presence = OnlinePresence.ONLINE
        )
        addOrUpdateLocalUser(newUser)
        com.example.data.firebase.FirebaseManager.getInstance().createUserInFirestore(newUser, onComplete)
        logAdminAction("CREATE_USER", "USER", newUid, "Created user ${newUser.email} with $startingCredits credits")
    }

    /**
     * Updates real-time generation requests streamed directly from Cloud Firestore generationRequests/{requestId}
     */
    fun updateGenerationRequestsFromFirestore(remoteRequests: List<GenerationRequest>) {
        val current = _generationRequests.value.associateBy { it.requestId }
        val merged = remoteRequests.map { remote ->
            val existing = current[remote.requestId]
            if (existing != null && existing.adminNotes.isNotEmpty() && remote.adminNotes.isEmpty()) {
                remote.copy(adminNotes = existing.adminNotes)
            } else {
                remote
            }
        }
        _generationRequests.value = merged
    }

    /**
     * Revokes active admin session immediately upon status change or suspension
     */
    fun handleAdminRevoked(reason: String) {
        val current = _currentAdmin.value
        if (current != null) {
            logAdminAction("ADMIN_REVOKED", "ADMIN", current.uid, "Admin authorization revoked: $reason")
        }
        _currentAdmin.value = null
        _users.value = emptyList()
        _generationRequests.value = emptyList()
    }

    /**
     * Updates real-time referral records streamed directly from Cloud Firestore referrals/{referralId}
     */
    fun updateReferralRecordsFromFirestore(remoteRecords: List<ReferralRecord>) {
        _referralRecords.value = remoteRecords
    }

    /**
     * Updates real-time credit transactions streamed from Cloud Firestore creditTransactions/{txId}
     */
    fun updateCreditTransactionsFromFirestore(remoteTx: List<CreditTransaction>) {
        _creditTransactions.value = remoteTx
    }

    // Referral Settings
    fun updateReferralSettings(settings: ReferralSettings) {
        val updated = settings.copy(updatedAt = System.currentTimeMillis())
        _referralSettings.value = updated
        com.example.data.firebase.FirebaseManager.getInstance().syncReferralSettings(updated)
        logAdminAction("UPDATE_REFERRAL_SETTINGS", "SETTINGS", "system_settings/referral", "Updated referral reward: ${updated.rewardCredits} Credits (Enabled: ${updated.enabled})")
    }

    // AI Limits Settings
    fun updateAILimitsSettings(settings: AILimitsSettings) {
        val updated = settings.copy(updatedAt = System.currentTimeMillis())
        _aiLimitsSettings.value = updated
        com.example.data.firebase.FirebaseManager.getInstance().syncAILimitsSettings(updated)
        logAdminAction("UPDATE_AI_LIMITS", "SETTINGS", "system_settings/ai_limits", "Updated AI generation limits: DailyImg=${updated.dailyImageLimit}, DailyVid=${updated.dailyVideoLimit}, DailyI2V=${updated.dailyImageToVideoLimit}, AdDuration=${updated.requiredAdDurationSeconds}s")
    }

    // Support Settings
    fun updateSupportSettings(settings: SupportSettings) {
        val updated = settings.copy(updatedAt = System.currentTimeMillis())
        _supportSettings.value = updated
        com.example.data.firebase.FirebaseManager.getInstance().syncSupportSettings(updated)
        logAdminAction("UPDATE_SUPPORT_SETTINGS", "SETTINGS", "system_settings/support", "Updated support channels: WhatsApp=${updated.whatsappNumber}, Telegram=${updated.telegramUsername}")
    }

    // Remote sync handlers for Firestore real-time listeners
    fun updateSystemFromFirestore(settings: AppSystemSettings) {
        _systemSettings.value = settings
    }

    fun updateAdSettingsFromFirestore(settings: AdSettings) {
        _adSettings.value = settings
    }

    fun updateCreditSettingsFromFirestore(settings: CreditSettings) {
        _creditSettings.value = settings
    }

    fun updateGenerationSettingsFromFirestore(settings: GenerationSettings) {
        _generationSettings.value = settings
    }

    fun updateNotificationsFromFirestore(notifications: List<AppNotification>) {
        _notifications.value = notifications
    }

    fun updateReferralFromFirestore(settings: ReferralSettings) {
        _referralSettings.value = settings
    }

    fun updateAILimitsFromFirestore(settings: AILimitsSettings) {
        _aiLimitsSettings.value = settings
    }

    fun updateSupportFromFirestore(settings: SupportSettings) {
        _supportSettings.value = settings
    }

    /**
     * Referral Claim & Processing Engine:
     * - Validates referral program is enabled
     * - Prevents self-referral (user entering their own code)
     * - Prevents duplicate claims (referred account can only generate ONE successful referral reward)
     * - Finds referrer by referralCode
     * - Credits referrer with 20 QRYNOVA Credits (configured rewardCredits)
     * - Increments referrer's referral count
     * - Records referral in referrals/{referralId}
     * - Logs credit transaction in creditTransactions/{txId}
     * - Syncs all updates directly to Firestore
     */
    fun processReferralClaim(referredUserId: String, inputCode: String): Result<Int> {
        val cleanCode = inputCode.trim().uppercase()
        if (cleanCode.isBlank()) {
            return Result.failure(IllegalArgumentException("Referral code cannot be empty"))
        }

        if (!_referralSettings.value.enabled) {
            return Result.failure(IllegalStateException("Referral reward system is currently disabled"))
        }

        val referredUser = _users.value.find { it.uid == referredUserId }
            ?: return Result.failure(Exception("Referred user not found"))

        // Prevent self-referral
        if (referredUser.referralCode.equals(cleanCode, ignoreCase = true) || referredUser.uid == cleanCode) {
            return Result.failure(IllegalArgumentException("You cannot use your own referral code"))
        }

        // Check if referred user has already been referred (one reward per referred account constraint)
        if (referredUser.referredBy.isNotBlank()) {
            return Result.failure(IllegalStateException("Account has already claimed a referral bonus"))
        }

        val alreadyRecorded = _referralRecords.value.any { it.referredUid == referredUserId }
        if (alreadyRecorded) {
            return Result.failure(IllegalStateException("A referral reward has already been granted for this account"))
        }

        // Find referrer user by referralCode or UID
        val referrer = _users.value.find { 
            it.referralCode.equals(cleanCode, ignoreCase = true) || 
            it.uid.equals(cleanCode, ignoreCase = true) 
        } ?: return Result.failure(Exception("Referral code '$cleanCode' not found"))

        if (referrer.uid == referredUserId) {
            return Result.failure(IllegalArgumentException("Self-referrals are prohibited"))
        }

        val rewardCredits = _referralSettings.value.rewardCredits // Default: 20 Credits

        // Update Referrer Credits & Stats
        val prevReferrerCredits = referrer.credits
        val newReferrerCredits = prevReferrerCredits + rewardCredits
        val updatedReferrer = referrer.copy(
            credits = newReferrerCredits,
            totalCreditsGranted = referrer.totalCreditsGranted + rewardCredits,
            referralCount = referrer.referralCount + 1
        )

        // Update Referred User with referrer info
        val updatedReferredUser = referredUser.copy(
            referredBy = referrer.uid
        )

        // Apply state updates
        _users.value = _users.value.map {
            when (it.uid) {
                referrer.uid -> updatedReferrer
                referredUserId -> updatedReferredUser
                else -> it
            }
        }

        // Push updates to Firestore
        com.example.data.firebase.FirebaseManager.getInstance().syncUserCredits(
            userId = referrer.uid,
            newCredits = newReferrerCredits,
            totalGranted = updatedReferrer.totalCreditsGranted,
            totalConsumed = updatedReferrer.totalCreditsConsumed
        )

        // Create Referral Record
        val referralRecord = ReferralRecord(
            id = "ref_" + UUID.randomUUID().toString().take(8),
            referrerUid = referrer.uid,
            referrerName = referrer.name,
            referrerEmail = referrer.email,
            referredUid = referredUserId,
            referredName = referredUser.name,
            referredEmail = referredUser.email,
            referralCode = cleanCode,
            creditsAwarded = rewardCredits,
            timestamp = System.currentTimeMillis()
        )
        _referralRecords.value = listOf(referralRecord) + _referralRecords.value
        com.example.data.firebase.FirebaseManager.getInstance().syncReferralRecord(referralRecord)

        // Create Credit Transaction for Referrer
        val creditTx = CreditTransaction(
            transactionId = "tx_" + UUID.randomUUID().toString().take(8),
            userId = referrer.uid,
            userEmail = referrer.email,
            amount = rewardCredits,
            previousBalance = prevReferrerCredits,
            newBalance = newReferrerCredits,
            type = TransactionType.REFERRAL_BONUS,
            reason = "Referral bonus for inviting ${referredUser.email}",
            adminId = "system_referral_engine",
            adminEmail = "referral@qrynova.ai",
            timestamp = System.currentTimeMillis()
        )
        _creditTransactions.value = listOf(creditTx) + _creditTransactions.value
        com.example.data.firebase.FirebaseManager.getInstance().syncCreditTransaction(creditTx)

        logAdminAction(
            "REFERRAL_AWARDED",
            "USER",
            referrer.uid,
            "Awarded +$rewardCredits QRYNOVA credits to referrer ${referrer.email} for invitee ${referredUser.email} (Code: $cleanCode)"
        )

        return Result.success(rewardCredits)
    }

    companion object {
        @Volatile
        private var instance: QrynovaDataRepository? = null

        fun getInstance(): QrynovaDataRepository {
            return instance ?: synchronized(this) {
                instance ?: QrynovaDataRepository().also { instance = it }
            }
        }
    }
}
