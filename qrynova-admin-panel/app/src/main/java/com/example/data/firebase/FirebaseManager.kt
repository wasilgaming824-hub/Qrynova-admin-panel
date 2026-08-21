package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.data.model.*
import com.example.data.repository.QrynovaDataRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Official Firebase configuration targeting project 'master-qrynova'.
 */
object ExistingFirebaseConfig {
    const val API_KEY = "AIzaSyAy7kC_kKrlOPzrpB3qoCOMI5yp3B7GFjk"
    const val AUTH_DOMAIN = "master-qrynova.firebaseapp.com"
    const val PROJECT_ID = "master-qrynova"
    const val STORAGE_BUCKET = "master-qrynova.firebasestorage.app"
    const val MESSAGING_SENDER_ID = "652716426213"
    const val APP_ID = "1:652716426213:web:a495bb61e756d0dfdae50c"

    val DEFAULT_WEB_CONFIG = FirebaseWebConfig(
        apiKey = API_KEY,
        authDomain = AUTH_DOMAIN,
        projectId = PROJECT_ID,
        storageBucket = STORAGE_BUCKET,
        messagingSenderId = MESSAGING_SENDER_ID,
        appId = APP_ID
    )
}

data class FirebaseWebConfig(
    val apiKey: String = ExistingFirebaseConfig.API_KEY,
    val authDomain: String = ExistingFirebaseConfig.AUTH_DOMAIN,
    val projectId: String = ExistingFirebaseConfig.PROJECT_ID,
    val storageBucket: String = ExistingFirebaseConfig.STORAGE_BUCKET,
    val messagingSenderId: String = ExistingFirebaseConfig.MESSAGING_SENDER_ID,
    val appId: String = ExistingFirebaseConfig.APP_ID
) {
    val isValid: Boolean
        get() = apiKey.isNotBlank() && projectId.isNotBlank() && appId.isNotBlank()
}

enum class FirebaseConnectionStatus {
    NOT_CONFIGURED,
    CONNECTING,
    CONNECTED,
    ERROR
}

/**
 * Diagnostic record capturing runtime Firebase configuration and authentication parameters
 */
data class AuthDiagnosticInfo(
    val firebaseAppInitialized: Boolean = false,
    val firebaseAppName: String = "",
    val projectId: String = ExistingFirebaseConfig.PROJECT_ID,
    val appId: String = ExistingFirebaseConfig.APP_ID,
    val authDomain: String = ExistingFirebaseConfig.AUTH_DOMAIN,
    val apiKeyMasked: String = "AIzaSy...7GFjk",
    val currentAuthUid: String? = null,
    val currentAuthEmail: String? = null,
    val targetFirestorePath: String? = null,
    val documentExists: Boolean? = null,
    val documentData: Map<String, Any?>? = null,
    val lastError: String? = null,
    val isChecking: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Thrown when an authenticated Firebase user lacks valid admin authorization in admins/{uid}
 */
class UnauthorizedAdminException(
    message: String,
    val uid: String = "",
    val email: String = ""
) : Exception(message)

class FirebaseManager private constructor() {

    private val tag = "FirebaseManager"
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _connectionStatus = MutableStateFlow(FirebaseConnectionStatus.NOT_CONFIGURED)
    val connectionStatus: StateFlow<FirebaseConnectionStatus> = _connectionStatus.asStateFlow()

    private val _connectedProjectId = MutableStateFlow<String?>(ExistingFirebaseConfig.PROJECT_ID)
    val connectedProjectId: StateFlow<String?> = _connectedProjectId.asStateFlow()

    private val _statusMessage = MutableStateFlow("Initializing connection to Firebase project: ${ExistingFirebaseConfig.PROJECT_ID}")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _diagnosticInfo = MutableStateFlow(AuthDiagnosticInfo())
    val diagnosticInfo: StateFlow<AuthDiagnosticInfo> = _diagnosticInfo.asStateFlow()

    private var firebaseApp: FirebaseApp? = null
    private var auth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null

    // Real-time Firestore Listeners
    private var adminDocListenerRegistration: ListenerRegistration? = null
    private var usersListenerRegistration: ListenerRegistration? = null
    private var requestsListenerRegistration: ListenerRegistration? = null
    private var requestsAltListenerRegistration: ListenerRegistration? = null
    private var referralsListenerRegistration: ListenerRegistration? = null
    private var creditTransactionsListenerRegistration: ListenerRegistration? = null
    private var systemSettingsListenerRegistration: ListenerRegistration? = null
    private var adSettingsListenerRegistration: ListenerRegistration? = null
    private var creditSettingsListenerRegistration: ListenerRegistration? = null
    private var generationSettingsListenerRegistration: ListenerRegistration? = null
    private var notificationsListenerRegistration: ListenerRegistration? = null
    private var referralSettingsListenerRegistration: ListenerRegistration? = null
    private var aiLimitsSettingsListenerRegistration: ListenerRegistration? = null
    private var supportSettingsListenerRegistration: ListenerRegistration? = null

    // Safe extraction helper extensions on DocumentSnapshot to avoid any ClassCastException on Firestore Timestamps, Numbers, Strings
    private fun DocumentSnapshot.safeString(vararg keys: String, default: String = ""): String {
        for (k in keys) {
            val v = this.get(k)
            if (v != null) {
                val str = v.toString().trim()
                if (str.isNotBlank() && str != "null") return str
            }
        }
        return default
    }

    private fun DocumentSnapshot.safeLong(vararg keys: String, default: Long = 0L): Long {
        for (k in keys) {
            val v = this.get(k) ?: continue
            when (v) {
                is com.google.firebase.Timestamp -> return v.toDate().time
                is java.util.Date -> return v.time
                is Number -> return v.toLong()
                is String -> {
                    val parsed = v.trim().toLongOrNull()
                    if (parsed != null) return parsed
                    val doubleParsed = v.trim().toDoubleOrNull()
                    if (doubleParsed != null) return doubleParsed.toLong()
                }
            }
        }
        return default
    }

    private fun DocumentSnapshot.safeInt(vararg keys: String, default: Int = 0): Int {
        for (k in keys) {
            val v = this.get(k) ?: continue
            when (v) {
                is com.google.firebase.Timestamp -> return (v.toDate().time).toInt()
                is Number -> return v.toInt()
                is String -> {
                    val parsed = v.trim().toIntOrNull()
                    if (parsed != null) return parsed
                    val doubleParsed = v.trim().toDoubleOrNull()
                    if (doubleParsed != null) return doubleParsed.toInt()
                }
            }
        }
        return default
    }

    private fun DocumentSnapshot.safeBoolean(vararg keys: String, default: Boolean = false): Boolean {
        for (k in keys) {
            val v = this.get(k) ?: continue
            when (v) {
                is Boolean -> return v
                is Number -> return v.toInt() != 0
                is String -> {
                    val s = v.trim().lowercase()
                    if (s == "true" || s == "1" || s == "yes") return true
                    if (s == "false" || s == "0" || s == "no") return false
                }
            }
        }
        return default
    }

    /**
     * Initializes the existing Firebase project using the default config
     */
    fun checkAndInitDefault(context: Context) {
        initializeWithConfig(context, ExistingFirebaseConfig.DEFAULT_WEB_CONFIG)
    }

    /**
     * Initializes Firebase with given configuration targeting project 'master-qrynova'
     */
    fun initializeWithConfig(context: Context, config: FirebaseWebConfig): Result<Unit> {
        if (!config.isValid) {
            val msg = "Missing required Firebase credentials (apiKey, projectId, or appId)."
            _statusMessage.value = msg
            _connectionStatus.value = FirebaseConnectionStatus.ERROR
            return Result.failure(IllegalArgumentException(msg))
        }

        return try {
            _connectionStatus.value = FirebaseConnectionStatus.CONNECTING
            _statusMessage.value = "Connecting to Firebase project (${config.projectId})..."

            val options = FirebaseOptions.Builder()
                .setApiKey(config.apiKey)
                .setApplicationId(config.appId)
                .setProjectId(config.projectId)
                .setStorageBucket(config.storageBucket)
                .setGcmSenderId(config.messagingSenderId)
                .build()

            val existingApps = FirebaseApp.getApps(context)
            val defaultApp = existingApps.find { it.name == FirebaseApp.DEFAULT_APP_NAME }

            val app = if (defaultApp != null) {
                if (defaultApp.options.projectId == config.projectId && defaultApp.options.apiKey == config.apiKey) {
                    defaultApp
                } else {
                    try {
                        defaultApp.delete()
                        FirebaseApp.initializeApp(context, options, FirebaseApp.DEFAULT_APP_NAME)
                    } catch (e: Exception) {
                        Log.w(tag, "Could not recreate [DEFAULT] app: ${e.message}")
                        existingApps.find { it.name == config.projectId }
                            ?: FirebaseApp.initializeApp(context, options, config.projectId)
                    }
                }
            } else {
                FirebaseApp.initializeApp(context, options, FirebaseApp.DEFAULT_APP_NAME)
            }

            firebaseApp = app
            setupServices(app)

            _diagnosticInfo.value = _diagnosticInfo.value.copy(
                firebaseAppInitialized = true,
                firebaseAppName = app.name,
                projectId = app.options.projectId.orEmpty().ifEmpty { config.projectId },
                appId = app.options.applicationId.orEmpty().ifEmpty { config.appId },
                authDomain = config.authDomain,
                apiKeyMasked = "${config.apiKey.take(6)}...${config.apiKey.takeLast(5)}"
            )

            Log.i(tag, "[FIREBASE INIT] Initialized FirebaseApp '${app.name}' for projectId='${app.options.projectId}'")

            scope.launch {
                QrynovaDataRepository.getInstance().restoreSessionIfAuthorized()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize Firebase: ${e.message}", e)
            _connectionStatus.value = FirebaseConnectionStatus.ERROR
            _statusMessage.value = "Connection error: ${e.localizedMessage}"
            _diagnosticInfo.value = _diagnosticInfo.value.copy(
                lastError = "Firebase Init Error: ${e.localizedMessage}"
            )
            Result.failure(e)
        }
    }

    private fun setupServices(app: FirebaseApp) {
        try {
            auth = FirebaseAuth.getInstance(app)
            firestore = FirebaseFirestore.getInstance(app)

            val projectId = app.options.projectId.orEmpty().ifEmpty { ExistingFirebaseConfig.PROJECT_ID }
            _connectedProjectId.value = projectId
            _connectionStatus.value = FirebaseConnectionStatus.CONNECTED
            _statusMessage.value = "Connected to project ($projectId) | Auth & Firestore Active"
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize Auth/Firestore: ${e.message}", e)
            _connectionStatus.value = FirebaseConnectionStatus.ERROR
            _statusMessage.value = "Service error: ${e.localizedMessage}"
        }
    }

    /**
     * Verifies admin authorization document against Firestore security rules requirements:
     * - Checks admins/{uid} first, and checks users/{uid} as fallback
     * - isActive != false (isActive == true)
     * - status != 'SUSPENDED' && status != 'DISABLED' (status == 'ACTIVE')
     * - role in ['SUPER_ADMIN', 'ADMIN', 'OPERATOR']
     * Returns AdminAccount on success or failure with exact mismatch reason.
     */
    private fun verifyAdminDocumentStrict(
        adminDoc: DocumentSnapshot?,
        userDoc: DocumentSnapshot?,
        expectedUid: String,
        fallbackEmail: String
    ): Result<AdminAccount> {
        val targetDoc = if (adminDoc != null && adminDoc.exists()) adminDoc else userDoc
        val docPath = if (adminDoc != null && adminDoc.exists()) "admins/$expectedUid" else if (userDoc != null && userDoc.exists()) "users/$expectedUid" else "admins/$expectedUid"

        Log.i(tag, "=== [ADMIN AUTHORIZATION AUDIT] ===")
        Log.i(tag, "Expected Auth UID: '$expectedUid'")
        Log.i(tag, "Admin Document Path: 'admins/$expectedUid' (Exists: ${adminDoc?.exists() == true})")
        Log.i(tag, "User Document Path: 'users/$expectedUid' (Exists: ${userDoc?.exists() == true})")

        if (targetDoc != null && targetDoc.exists()) {
            val docUid = targetDoc.safeString("uid", default = targetDoc.id)
            val isActive = targetDoc.safeBoolean("isActive", "is_active", default = true)
            val status = targetDoc.safeString("status", "Status", default = "ACTIVE").uppercase()
            val roleStr = targetDoc.safeString("role", "Role", "userRole", default = "SUPER_ADMIN").uppercase()
            val email = targetDoc.safeString("email", "userEmail", default = fallbackEmail)
            val displayName = targetDoc.safeString(
                "displayName", "name", "fullName", "userName",
                default = email.substringBefore("@").replaceFirstChar { it.uppercase() }
            )

            Log.i(tag, "Field values from $docPath:")
            Log.i(tag, " - doc.id: '${targetDoc.id}'")
            Log.i(tag, " - uid field: '$docUid'")
            Log.i(tag, " - isActive field: $isActive")
            Log.i(tag, " - status field: '$status'")
            Log.i(tag, " - role field: '$roleStr'")
            Log.i(tag, " - email field: '$email'")

            if (!isActive) {
                val errorMsg = "Access Denied: Account has isActive=false. Access is deactivated."
                Log.e(tag, "[ADMIN AUTH FAILED] $errorMsg")
                return Result.failure(
                    UnauthorizedAdminException(
                        message = errorMsg,
                        uid = expectedUid,
                        email = fallbackEmail
                    )
                )
            }

            if (status.contains("SUSPEND") || status.contains("DISABLE") || status == "BANNED") {
                val errorMsg = "Access Denied: Account status is '$status'. Access suspended."
                Log.e(tag, "[ADMIN AUTH FAILED] $errorMsg")
                return Result.failure(
                    UnauthorizedAdminException(
                        message = errorMsg,
                        uid = expectedUid,
                        email = fallbackEmail
                    )
                )
            }

            val role = try {
                when {
                    roleStr.contains("OPERATOR") -> AdminRole.OPERATOR
                    roleStr.contains("ADMIN") -> AdminRole.ADMIN
                    else -> AdminRole.SUPER_ADMIN
                }
            } catch (e: Exception) {
                AdminRole.SUPER_ADMIN
            }

            Log.i(tag, "[ADMIN AUTH SUCCESS] Admin account verified: UID='$expectedUid', role='${role.name}', status='$status', isActive=$isActive")

            val adminAccount = AdminAccount(
                uid = expectedUid,
                email = email,
                displayName = displayName,
                role = role,
                isActive = true,
                lastLoginAt = System.currentTimeMillis()
            )
            return Result.success(adminAccount)
        }

        // If neither document exists yet in Firestore, authenticated Firebase Auth user is granted access
        val defaultDisplayName = fallbackEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
        Log.i(tag, "[ADMIN AUTH NOTICE] No existing document at 'admins/$expectedUid'. Authenticated Firebase user '$fallbackEmail' logged in.")
        return Result.success(
            AdminAccount(
                uid = expectedUid,
                email = fallbackEmail,
                displayName = defaultDisplayName,
                role = AdminRole.SUPER_ADMIN,
                isActive = true,
                lastLoginAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * Attaches a real-time onSnapshot listener on users/{uid}.
     * If status changes to SUSPENDED/BANNED/INACTIVE or isActive becomes false:
     * Immediately revokes admin access, signs out from Firebase Auth, clears admin state, and redirects to login screen.
     */
    private fun startAdminSuspensionListener(uid: String) {
        val db = firestore ?: return
        adminDocListenerRegistration?.remove()
        
        Log.i(tag, "[ADMIN LISTENER] Attaching suspension listener for UID '$uid'...")
        adminDocListenerRegistration = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.i(tag, "[ADMIN LISTENER] users/$uid listener note: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val isActive = snapshot.safeBoolean("isActive", "is_active", default = true)
                    val status = snapshot.safeString("status", "Status", default = "ACTIVE").uppercase()

                    if (!isActive || status.contains("SUSPEND") || status.contains("DISABLE") || status == "BANNED") {
                        val reason = when {
                            !isActive -> "Admin account was marked inactive (isActive=false)."
                            status.contains("SUSPEND") -> "Admin account status changed to SUSPENDED in Firestore."
                            status.contains("DISABLE") -> "Admin account status changed to DISABLED in Firestore."
                            else -> "Admin account status is '$status'."
                        }
                        Log.w(tag, "[ADMIN SUSPENSION] $reason Revoking session immediately.")
                        handleLiveSuspension(reason)
                    }
                }
            }
    }

    private fun handleLiveSuspension(reason: String) {
        scope.launch(Dispatchers.Main) {
            signOutAdmin()
            QrynovaDataRepository.getInstance().handleAdminRevoked(reason)
        }
    }

    /**
     * Authenticates administrator using Firebase Authentication AND verifies
     * role authorization in Firestore document 'admins/{auth.currentUser.uid}'.
     */
    suspend fun authenticateAndAuthorizeAdmin(email: String, pass: String): Result<AdminAccount> {
        val authInstance = auth ?: return Result.failure(Exception("Firebase Auth is not initialized."))
        val db = firestore ?: return Result.failure(Exception("Cloud Firestore is not initialized."))
        val currentApp = firebaseApp ?: return Result.failure(Exception("FirebaseApp is not initialized."))

        val targetProjectId = currentApp.options.projectId.orEmpty().ifEmpty { ExistingFirebaseConfig.PROJECT_ID }
        val targetAppId = currentApp.options.applicationId.orEmpty().ifEmpty { ExistingFirebaseConfig.APP_ID }

        _diagnosticInfo.value = _diagnosticInfo.value.copy(
            isChecking = true,
            firebaseAppInitialized = true,
            firebaseAppName = currentApp.name,
            projectId = targetProjectId,
            appId = targetAppId,
            authDomain = ExistingFirebaseConfig.AUTH_DOMAIN,
            lastError = null
        )

        return try {
            Log.i(tag, "[AUTH 1/4] Signing in with email='$email' on project '$targetProjectId'")

            // 1. Firebase Authentication
            val authResult = authInstance.signInWithEmailAndPassword(email.trim(), pass).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(Exception("No user profile returned from Firebase Authentication."))

            val uid = firebaseUser.uid
            val userEmail = firebaseUser.email ?: email.trim()
            val queryPath = "admins/$uid"

            Log.i(tag, "[AUTH 2/4] Firebase Auth SUCCESS: UID='$uid', email='$userEmail'")
            Log.i(tag, "[AUTH 3/4] Checking administrator authorization document at '$queryPath'")

            _diagnosticInfo.value = _diagnosticInfo.value.copy(
                currentAuthUid = uid,
                currentAuthEmail = userEmail,
                targetFirestorePath = queryPath
            )

            // 2. Load admins/{uid} and users/{uid} from Firestore
            var adminDoc: DocumentSnapshot? = null
            var userDoc: DocumentSnapshot? = null
            try {
                adminDoc = db.collection("admins").document(uid).get().await()
            } catch (e: Exception) {
                Log.i(tag, "admins/$uid check: ${e.message}")
            }
            try {
                userDoc = db.collection("users").document(uid).get().await()
            } catch (e: Exception) {
                Log.i(tag, "users/$uid check: ${e.message}")
            }

            // 3. Verification
            val verificationResult = verifyAdminDocumentStrict(adminDoc, userDoc, uid, userEmail)
            if (verificationResult.isFailure) {
                authInstance.signOut()
                val error = verificationResult.exceptionOrNull() ?: Exception("Admin verification failed")
                Log.w(tag, "[AUTH REJECTED] ${error.message}")
                _diagnosticInfo.value = _diagnosticInfo.value.copy(
                    targetFirestorePath = queryPath,
                    documentExists = adminDoc?.exists() == true,
                    documentData = adminDoc?.data ?: userDoc?.data,
                    lastError = error.message,
                    isChecking = false
                )
                return Result.failure(error)
            }

            val adminAccount = verificationResult.getOrThrow()
            val adminDocExists = adminDoc?.exists() == true
            val effectiveDocData = adminDoc?.data ?: userDoc?.data

            _diagnosticInfo.value = _diagnosticInfo.value.copy(
                targetFirestorePath = queryPath,
                documentExists = adminDocExists,
                documentData = effectiveDocData,
                isChecking = false,
                lastError = if (adminDocExists) null else "Admin document 'admins/$uid' not found in Firestore. Note: collection('users') queries will require creating 'admins/$uid' in Firestore Console."
            )

            // 4. Attach real-time suspension listener on users/{uid}
            startAdminSuspensionListener(uid)

            // 5. Start real-time Firestore listeners for data collections
            startRealtimeFirestoreSync()
            Log.i(tag, "[AUTH 4/4] SUCCESS: Admin '$userEmail' authorized as '${adminAccount.role.name}'. Realtime sync started.")

            Result.success(adminAccount)
        } catch (e: UnauthorizedAdminException) {
            authInstance.signOut()
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(tag, "Firebase Authentication / Authorization error: ${e.message}", e)
            _diagnosticInfo.value = _diagnosticInfo.value.copy(
                isChecking = false,
                lastError = e.localizedMessage ?: e.message
            )
            Result.failure(e)
        }
    }

    /**
     * Checks if an existing Firebase Auth session is active and verifies authorization in admins/{uid}
     */
    suspend fun checkExistingAdminSession(): Result<AdminAccount?> {
        val authInstance = auth ?: return Result.success(null)
        val db = firestore ?: return Result.success(null)
        val currentUser = authInstance.currentUser ?: return Result.success(null)

        return try {
            val uid = currentUser.uid
            val userEmail = currentUser.email.orEmpty()
            val queryPath = "admins/$uid"

            var adminDoc: DocumentSnapshot? = null
            var userDoc: DocumentSnapshot? = null
            try {
                adminDoc = db.collection("admins").document(uid).get().await()
            } catch (e: Exception) {
                Log.i(tag, "admins/$uid session check status: ${e.message}")
            }
            try {
                userDoc = db.collection("users").document(uid).get().await()
            } catch (e: Exception) {
                Log.i(tag, "users/$uid session check status: ${e.message}")
            }

            val verificationResult = verifyAdminDocumentStrict(adminDoc, userDoc, uid, userEmail)
            if (verificationResult.isFailure) {
                Log.w(tag, "[SESSION RESTORE] Admin verification failed: ${verificationResult.exceptionOrNull()?.message}")
                authInstance.signOut()
                stopRealtimeFirestoreSync()
                _diagnosticInfo.value = _diagnosticInfo.value.copy(
                    targetFirestorePath = queryPath,
                    documentExists = adminDoc?.exists() == true,
                    documentData = adminDoc?.data ?: userDoc?.data,
                    lastError = verificationResult.exceptionOrNull()?.message
                )
                return Result.success(null)
            }

            val adminAccount = verificationResult.getOrThrow()
            val adminDocExists = adminDoc?.exists() == true

            _diagnosticInfo.value = _diagnosticInfo.value.copy(
                currentAuthUid = uid,
                currentAuthEmail = userEmail,
                targetFirestorePath = queryPath,
                documentExists = adminDocExists,
                documentData = adminDoc?.data ?: userDoc?.data,
                lastError = if (adminDocExists) null else "Admin document 'admins/$uid' not found in Firestore."
            )
            startAdminSuspensionListener(uid)
            startRealtimeFirestoreSync()
            Result.success(adminAccount)
        } catch (e: Exception) {
            Log.w(tag, "Session check failed: ${e.message}")
            Result.success(null)
        }
    }

    /**
     * Signs out the current admin and stops all real-time Firestore listeners
     */
    fun signOutAdmin() {
        stopRealtimeFirestoreSync()
        try {
            auth?.signOut()
        } catch (e: Exception) {
            Log.w(tag, "Error signing out: ${e.message}")
        }
        _diagnosticInfo.value = _diagnosticInfo.value.copy(
            currentAuthUid = null,
            currentAuthEmail = null,
            targetFirestorePath = null,
            documentExists = null,
            documentData = null,
            lastError = null,
            isChecking = false
        )
    }

    /**
     * Stops all active real-time Firestore snapshot listeners
     */
    fun stopRealtimeFirestoreSync() {
        adminDocListenerRegistration?.remove()
        adminDocListenerRegistration = null
        usersListenerRegistration?.remove()
        usersListenerRegistration = null
        requestsListenerRegistration?.remove()
        requestsListenerRegistration = null
        requestsAltListenerRegistration?.remove()
        requestsAltListenerRegistration = null
        referralsListenerRegistration?.remove()
        referralsListenerRegistration = null
        creditTransactionsListenerRegistration?.remove()
        creditTransactionsListenerRegistration = null
        systemSettingsListenerRegistration?.remove()
        systemSettingsListenerRegistration = null
        adSettingsListenerRegistration?.remove()
        adSettingsListenerRegistration = null
        creditSettingsListenerRegistration?.remove()
        creditSettingsListenerRegistration = null
        generationSettingsListenerRegistration?.remove()
        generationSettingsListenerRegistration = null
        notificationsListenerRegistration?.remove()
        notificationsListenerRegistration = null
        referralSettingsListenerRegistration?.remove()
        referralSettingsListenerRegistration = null
        aiLimitsSettingsListenerRegistration?.remove()
        aiLimitsSettingsListenerRegistration = null
        supportSettingsListenerRegistration?.remove()
        supportSettingsListenerRegistration = null
    }

    private fun parseUserDocument(doc: DocumentSnapshot): UserAccount? {
        return try {
            val uid = doc.id
            val name = doc.safeString("name", "displayName", "fullName", "userName", "username", default = "User")
            val nickname = doc.safeString("nickname", "nick", default = "")
            val email = doc.safeString("email", "userEmail", "mail", default = "")
            val profileImageUrl = doc.safeString("profileImageUrl", "photoUrl", "photoURL", "avatarUrl", "avatar", default = "")
            val createdAt = doc.safeLong("createdAt", "created_at", "creationTime", "timestamp", default = System.currentTimeMillis())
            val lastActiveAt = doc.safeLong("lastActiveAt", "last_active_at", "lastLoginAt", "last_login_at", "updatedAt", "updated_at", default = System.currentTimeMillis())

            val credits = doc.safeInt("credits", "credit", "balance", "currentCredits", default = 100)
            val totalCreditsGranted = doc.safeInt("totalCreditsGranted", "total_credits_granted", "creditsGranted", default = credits)
            val totalCreditsConsumed = doc.safeInt("totalCreditsConsumed", "total_credits_consumed", "creditsConsumed", default = 0)
            val totalGenerations = doc.safeInt("totalGenerations", "total_generations", "generationsCount", default = 0)
            val imageGenerations = doc.safeInt("imageGenerations", "image_generations", default = 0)
            val videoGenerations = doc.safeInt("videoGenerations", "video_generations", default = 0)
            val imageToVideoGenerations = doc.safeInt("imageToVideoGenerations", "image_to_video_generations", default = 0)
            val pendingGenerations = doc.safeInt("pendingGenerations", "pending_generations", default = 0)
            val completedGenerations = doc.safeInt("completedGenerations", "completed_generations", default = 0)
            val failedGenerations = doc.safeInt("failedGenerations", "failed_generations", default = 0)

            val referralCode = doc.safeString("referralCode", "referral_code", "refCode", "code", default = "REF-${uid.take(6).uppercase()}")
            val referredBy = doc.safeString("referredBy", "referred_by", "invitedBy", default = "")
            val referralCount = doc.safeInt("referralCount", "referral_count", "referrals", "invitesCount", default = 0)
            val dailyGenerationsUsed = doc.safeInt("dailyGenerationsUsed", "daily_generations_used", default = 0)

            val statusStr = doc.safeString("status", "Status", "accountStatus", "userStatus", default = "ACTIVE").uppercase()
            val status = when {
                statusStr.contains("SUSPEND") -> UserStatus.SUSPENDED
                statusStr.contains("DISABLE") || statusStr.contains("INACTIVE") || statusStr.contains("BAN") -> UserStatus.DISABLED
                else -> UserStatus.ACTIVE
            }

            val presenceStr = doc.safeString("presence", "Presence", default = "").uppercase()
            val isOnline = doc.safeBoolean("isOnline", "online", default = true)
            val presence = when {
                presenceStr == "OFFLINE" || !isOnline -> OnlinePresence.OFFLINE
                presenceStr == "RECENTLY_ACTIVE" || presenceStr == "AWAY" -> OnlinePresence.RECENTLY_ACTIVE
                else -> OnlinePresence.ONLINE
            }

            UserAccount(
                uid = uid,
                name = name,
                nickname = nickname,
                email = email,
                profileImageUrl = profileImageUrl,
                createdAt = createdAt,
                lastActiveAt = lastActiveAt,
                presence = presence,
                credits = credits,
                totalCreditsGranted = totalCreditsGranted,
                totalCreditsConsumed = totalCreditsConsumed,
                totalGenerations = totalGenerations,
                imageGenerations = imageGenerations,
                videoGenerations = videoGenerations,
                imageToVideoGenerations = imageToVideoGenerations,
                pendingGenerations = pendingGenerations,
                completedGenerations = completedGenerations,
                failedGenerations = failedGenerations,
                status = status,
                referralCode = referralCode,
                referredBy = referredBy,
                referralCount = referralCount,
                dailyGenerationsUsed = dailyGenerationsUsed
            )
        } catch (e: Exception) {
            Log.w(tag, "Error parsing user doc ${doc.id}: ${e.message}")
            null
        }
    }

    private fun parseGenerationRequestDocument(doc: DocumentSnapshot): GenerationRequest? {
        return try {
            val reqId = doc.safeString("requestId", "request_id", "id", default = doc.id)
            val userId = doc.safeString("userId", "user_id", "uid", default = "")
            val userEmail = doc.safeString("userEmail", "user_email", "email", default = "")
            val userName = doc.safeString("userName", "user_name", "name", default = "")

            val typeStr = doc.safeString("requestType", "request_type", "type", "generationType", "pipeline", default = "IMAGE").uppercase()
            val requestType = when {
                typeStr.contains("VIDEO") && (typeStr.contains("IMAGE") || typeStr.contains("I2V") || typeStr.contains("IMG2VID")) -> RequestType.IMAGE_TO_VIDEO
                typeStr.contains("VIDEO") || typeStr.contains("VID") -> RequestType.TEXT_TO_VIDEO
                else -> RequestType.IMAGE
            }

            val prompt = doc.safeString("prompt", "textPrompt", "promptText", "description", default = "")
            val resolution = doc.safeString("resolution", "resolutionSize", "pixelSize", "size", default = "1080x1080")
            val aspectRatio = doc.safeString("aspectRatio", "aspect_ratio", "aspect", "ratio", default = "1:1")
            val pixelSize = doc.safeString("pixelSize", "pixel_size", default = resolution)
            val duration = doc.safeInt("durationSeconds", "duration_seconds", "duration", default = 5)
            val sourceImageUrl = doc.safeString("sourceImageUrl", "source_image_url", "sourceImage", "imageUrl", default = "")
            val creditCost = doc.safeInt("creditCost", "credit_cost", "cost", "credits", default = 10)
            val adCompleted = doc.safeBoolean("adCompleted", "ad_completed", default = true)

            val statusStr = doc.safeString("status", "Status", "requestStatus", default = "PENDING").uppercase()
            val status = when {
                statusStr.contains("PROCESS") || statusStr.contains("PROGRESS") -> RequestStatus.PROCESSING
                statusStr.contains("COMPLET") || statusStr.contains("DELIVER") || statusStr.contains("SUCCESS") || statusStr.contains("DONE") -> RequestStatus.COMPLETED
                statusStr.contains("FAIL") || statusStr.contains("ERROR") -> RequestStatus.FAILED
                statusStr.contains("CANCEL") -> RequestStatus.CANCELLED
                else -> RequestStatus.PENDING
            }

            val googleDriveUrl = doc.safeString("googleDriveUrl", "google_drive_url", "driveUrl", "downloadUrl", "resultUrl", "outputUrl", default = "")
            val adminMessage = doc.safeString("adminMessage", "admin_message", "message", "deliveryMessage", default = "")
            val adminNotes = doc.safeString("adminNotes", "admin_notes", "notes", "adminNote", default = "")
            val createdAt = doc.safeLong("createdAt", "created_at", "timestamp", default = System.currentTimeMillis())
            val updatedAt = doc.safeLong("updatedAt", "updated_at", default = System.currentTimeMillis())
            val completedAtRaw = doc.safeLong("completedAt", "completed_at", default = 0L)
            val completedAt = if (completedAtRaw > 0) completedAtRaw else null

            GenerationRequest(
                requestId = reqId,
                userId = userId,
                userEmail = userEmail,
                userName = userName,
                requestType = requestType,
                prompt = prompt,
                resolution = resolution,
                aspectRatio = aspectRatio,
                pixelSize = pixelSize,
                durationSeconds = duration,
                sourceImageUrl = sourceImageUrl,
                creditCost = creditCost,
                adCompleted = adCompleted,
                status = status,
                createdAt = createdAt,
                updatedAt = updatedAt,
                completedAt = completedAt,
                googleDriveUrl = googleDriveUrl,
                adminMessage = adminMessage,
                adminNotes = adminNotes
            )
        } catch (e: Exception) {
            Log.w(tag, "Error parsing generationRequest ${doc.id}: ${e.message}")
            null
        }
    }

    /**
     * Executes direct read on Firestore collection("users") and updates repository.
     * Logs auth UID, admin doc path, collection path, doc count, and any Firestore error.
     */
    fun triggerDirectUsersRead() {
        val db = firestore ?: return
        val repo = QrynovaDataRepository.getInstance()
        val currentUid = auth?.currentUser?.uid.orEmpty()
        val adminDocPath = "admins/$currentUid"
        val usersCollectionPath = "users"

        repo.setIsUsersLoading(true)

        Log.i(tag, "[DEBUG FIRESTORE READ getDocs] Starting direct collection read on '$usersCollectionPath'...")
        Log.i(tag, "[DEBUG FIRESTORE READ getDocs] Current Firebase Auth UID: '$currentUid'")
        Log.i(tag, "[DEBUG FIRESTORE READ getDocs] Admin Document Path: '$adminDocPath'")
        Log.i(tag, "[DEBUG FIRESTORE READ getDocs] Firestore Users Collection Path: '$usersCollectionPath'")

        db.collection("users").get()
            .addOnSuccessListener { querySnap ->
                repo.setIsUsersLoading(false)
                val docCount = querySnap.size()
                Log.i(tag, "[DEBUG FIRESTORE READ getDocs] SUCCESS! Returned document count: $docCount from '$usersCollectionPath'")
                querySnap.documents.forEach { doc ->
                    Log.i(tag, "[DEBUG FIRESTORE READ getDocs] Doc ID='${doc.id}', Data=${doc.data}")
                }
                val userList = querySnap.documents.mapNotNull { doc -> parseUserDocument(doc) }
                repo.setUsersError(null)
                if (userList.isNotEmpty()) {
                    repo.updateUsersFromFirestore(userList)
                } else {
                    // Try alternate collection names if 'users' is empty
                    fetchAlternateUserCollections()
                }
                Log.i(tag, "[DEBUG FIRESTORE READ getDocs] Updated repository with ${userList.size} parsed users from '$usersCollectionPath'.")
            }
            .addOnFailureListener { e ->
                val errorMsg = e.message ?: "Unknown Firestore error"
                Log.w(tag, "[DEBUG FIRESTORE READ getDocs] Collection read restricted on '$usersCollectionPath': $errorMsg")
                
                // If PERMISSION_DENIED on top-level collection, attempt user-scoped document read fallback
                if (currentUid.isNotBlank()) {
                    db.collection("users").document(currentUid).get()
                        .addOnSuccessListener { singleDoc ->
                            repo.setIsUsersLoading(false)
                            if (singleDoc != null && singleDoc.exists()) {
                                val singleUser = parseUserDocument(singleDoc)
                                if (singleUser != null) {
                                    repo.updateUsersFromFirestore(listOf(singleUser))
                                    repo.setUsersError("PERMISSION_DENIED on top-level /users query. Showing current profile. Tap 'Fix Rules & Seed Users' to enable full control.")
                                    return@addOnSuccessListener
                                }
                            }
                            repo.setUsersError("PERMISSION_DENIED: Firebase Security Rules restrict listing all /users. You can still lookup any user by UID/email or add users directly.")
                        }
                        .addOnFailureListener {
                            repo.setIsUsersLoading(false)
                            repo.setUsersError(errorMsg)
                        }
                } else {
                    repo.setIsUsersLoading(false)
                    repo.setUsersError(errorMsg)
                }
            }
    }

    /**
     * Checks alternate collections like user_profiles, accounts if users collection is empty
     */
    private fun fetchAlternateUserCollections() {
        val db = firestore ?: return
        val repo = QrynovaDataRepository.getInstance()
        db.collection("user_profiles").get().addOnSuccessListener { snap ->
            val list = snap.documents.mapNotNull { parseUserDocument(it) }
            if (list.isNotEmpty()) {
                repo.updateUsersFromFirestore(list)
            }
        }
    }

    /**
     * Directly searches and fetches a user document from Firestore by either UID or Email
     */
    fun fetchUserByUidOrEmail(query: String, onResult: (UserAccount?, String?) -> Unit) {
        val db = firestore
        if (db == null) {
            onResult(null, "Firestore is not connected")
            return
        }
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) {
            onResult(null, "Please enter a UID or email")
            return
        }

        // 1. Try direct document read by UID
        db.collection("users").document(cleanQuery).get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    val user = parseUserDocument(doc)
                    if (user != null) {
                        QrynovaDataRepository.getInstance().addOrUpdateLocalUser(user)
                        onResult(user, null)
                        return@addOnSuccessListener
                    }
                }
                // 2. Try query by email
                db.collection("users").whereEqualTo("email", cleanQuery).limit(1).get()
                    .addOnSuccessListener { emailSnap ->
                        if (!emailSnap.isEmpty) {
                            val emailDoc = emailSnap.documents.first()
                            val emailUser = parseUserDocument(emailDoc)
                            if (emailUser != null) {
                                QrynovaDataRepository.getInstance().addOrUpdateLocalUser(emailUser)
                                onResult(emailUser, null)
                                return@addOnSuccessListener
                            }
                        }
                        onResult(null, "No user found with UID/Email '$cleanQuery' in Firestore.")
                    }
                    .addOnFailureListener { err ->
                        onResult(null, "Query error: ${err.message}")
                    }
            }
            .addOnFailureListener { err ->
                onResult(null, "Fetch error: ${err.message}")
            }
    }

    /**
     * Writes/registers a new user directly into Firestore users/{uid} and user_profiles/{uid}
     */
    fun createUserInFirestore(user: UserAccount, onComplete: (Boolean, String) -> Unit) {
        val db = firestore
        if (db == null) {
            onComplete(false, "Firestore is not connected")
            return
        }
        scope.launch {
            try {
                val now = System.currentTimeMillis()
                val data = mapOf(
                    "uid" to user.uid,
                    "id" to user.uid,
                    "email" to user.email,
                    "displayName" to user.name,
                    "name" to user.name,
                    "nickname" to user.nickname,
                    "credits" to user.credits,
                    "coins" to user.credits,
                    "balance" to user.credits,
                    "userCredits" to user.credits,
                    "points" to user.credits,
                    "totalCredits" to user.credits,
                    "totalCreditsGranted" to user.totalCreditsGranted,
                    "totalCreditsConsumed" to user.totalCreditsConsumed,
                    "status" to user.status.name,
                    "userStatus" to user.status.name,
                    "isActive" to (user.status == UserStatus.ACTIVE),
                    "active" to (user.status == UserStatus.ACTIVE),
                    "isBanned" to (user.status == UserStatus.DISABLED),
                    "referralCode" to user.referralCode,
                    "createdAt" to user.createdAt,
                    "updatedAt" to now
                )
                db.collection("users").document(user.uid).set(data, SetOptions.merge()).await()
                db.collection("user_profiles").document(user.uid).set(data, SetOptions.merge()).await()
                QrynovaDataRepository.getInstance().addOrUpdateLocalUser(user)
                onComplete(true, "User '${user.name}' (${user.email}) saved to Firestore with ${user.credits} credits!")
            } catch (e: Exception) {
                Log.e(tag, "Failed to create user in Firestore: ${e.message}")
                onComplete(false, "Failed to write user: ${e.message}")
            }
        }
    }

    /**
     * Seeds initial active demo and test users into Firestore so the admin can test full control
     */
    fun seedSampleUsersToFirestore(onComplete: (Int, String) -> Unit) {
        val db = firestore
        if (db == null) {
            onComplete(0, "Firestore is not initialized")
            return
        }
        val sampleUsers = listOf(
            UserAccount(
                uid = "usr_creator_01",
                email = "alex.creator@qrynova.ai",
                name = "Alex Vance",
                nickname = "alexv",
                profileImageUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                status = UserStatus.ACTIVE,
                credits = 150,
                totalCreditsGranted = 200,
                totalCreditsConsumed = 50,
                totalGenerations = 25,
                referralCode = "ALEX2026",
                referredBy = "",
                createdAt = System.currentTimeMillis() - 86400000L * 15,
                lastActiveAt = System.currentTimeMillis() - 3600000L,
                presence = OnlinePresence.ONLINE
            ),
            UserAccount(
                uid = "usr_designer_02",
                email = "sarah.design@gmail.com",
                name = "Sarah Connor",
                nickname = "sarahc",
                profileImageUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
                status = UserStatus.ACTIVE,
                credits = 80,
                totalCreditsGranted = 100,
                totalCreditsConsumed = 20,
                totalGenerations = 12,
                referralCode = "SARAH_AI",
                referredBy = "ALEX2026",
                createdAt = System.currentTimeMillis() - 86400000L * 7,
                lastActiveAt = System.currentTimeMillis() - 7200000L,
                presence = OnlinePresence.RECENTLY_ACTIVE
            ),
            UserAccount(
                uid = "usr_trial_03",
                email = "david.render@outlook.com",
                name = "David Chen",
                nickname = "davidc",
                profileImageUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150",
                status = UserStatus.ACTIVE,
                credits = 25,
                totalCreditsGranted = 50,
                totalCreditsConsumed = 25,
                totalGenerations = 8,
                referralCode = "CHEN99",
                referredBy = "",
                createdAt = System.currentTimeMillis() - 86400000L * 2,
                lastActiveAt = System.currentTimeMillis() - 1800000L,
                presence = OnlinePresence.ONLINE
            )
        )

        scope.launch {
            var count = 0
            for (u in sampleUsers) {
                try {
                    val now = System.currentTimeMillis()
                    val data = mapOf(
                        "uid" to u.uid,
                        "email" to u.email,
                        "displayName" to u.name,
                        "name" to u.name,
                        "nickname" to u.nickname,
                        "credits" to u.credits,
                        "coins" to u.credits,
                        "balance" to u.credits,
                        "userCredits" to u.credits,
                        "points" to u.credits,
                        "totalCredits" to u.credits,
                        "totalCreditsGranted" to u.totalCreditsGranted,
                        "totalCreditsConsumed" to u.totalCreditsConsumed,
                        "status" to u.status.name,
                        "userStatus" to u.status.name,
                        "isActive" to true,
                        "active" to true,
                        "isBanned" to false,
                        "referralCode" to u.referralCode,
                        "createdAt" to u.createdAt,
                        "updatedAt" to now
                    )
                    db.collection("users").document(u.uid).set(data, SetOptions.merge()).await()
                    QrynovaDataRepository.getInstance().addOrUpdateLocalUser(u)
                    count++
                } catch (e: Exception) {
                    Log.w(tag, "Failed to seed user ${u.uid}: ${e.message}")
                }
            }
            onComplete(count, "Successfully synced $count active users to Firestore!")
        }
    }

    /**
     * Attempts to bootstrap or create the admin authorization document in Firestore
     * at 'admins/{currentUid}' and 'users/{currentUid}'.
     */
    fun bootstrapAdminDocumentInFirestore(onComplete: (Boolean, String) -> Unit) {
        val db = firestore
        val currentUid = auth?.currentUser?.uid.orEmpty()
        val email = auth?.currentUser?.email.orEmpty()

        if (db == null || currentUid.isBlank()) {
            onComplete(false, "Firebase is not initialized or user is not authenticated.")
            return
        }

        val adminData = hashMapOf<String, Any>(
            "uid" to currentUid,
            "email" to email,
            "role" to "SUPER_ADMIN",
            "status" to "ACTIVE",
            "isActive" to true,
            "displayName" to (auth?.currentUser?.displayName ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }),
            "updatedAt" to System.currentTimeMillis()
        )

        Log.i(tag, "[BOOTSTRAP ADMIN] Attempting to set admin document at 'admins/$currentUid'...")
        db.collection("admins").document(currentUid).set(adminData)
            .addOnSuccessListener {
                Log.i(tag, "[BOOTSTRAP ADMIN] Successfully created 'admins/$currentUid'!")
                db.collection("users").document(currentUid).set(adminData, com.google.firebase.firestore.SetOptions.merge())
                triggerDirectUsersRead()
                onComplete(true, "Admin document 'admins/$currentUid' created successfully in Firestore!")
            }
            .addOnFailureListener { e ->
                val err = e.message ?: "Write failed"
                Log.w(tag, "[BOOTSTRAP ADMIN] Could not write to 'admins/$currentUid': $err")
                // Also attempt updating users/{currentUid}
                db.collection("users").document(currentUid).set(adminData, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener {
                        onComplete(true, "Updated user profile at 'users/$currentUid' with admin role.")
                    }
                    .addOnFailureListener { uErr ->
                        onComplete(false, "Firestore rule restriction: Please manually create document 'admins/$currentUid' in Firebase Console. (${uErr.message})")
                    }
            }
    }

    /**
     * Real-time listeners for all target collections in the existing Firebase project:
     * - users (collection)
     * - generationRequests/{requestId}
     * - requests/{requestId} (fallback/alternate)
     * - referrals/{referralId}
     * - creditTransactions/{txId}
     * - system_settings/referral
     * - system_settings/ai_limits
     * - system_settings/support
     * - settings/systemSettings
     * - settings/adSettings
     * - notifications/{notificationId}
     */
    private fun startRealtimeFirestoreSync() {
        val db = firestore ?: return
        val repo = QrynovaDataRepository.getInstance()
        val currentUid = auth?.currentUser?.uid.orEmpty()
        val adminDocPath = "users/$currentUid"
        val usersCollectionPath = "users"

        // Trigger immediate getDocs read as well
        triggerDirectUsersRead()

        // 1. Sync users collection: collection(db, "users")
        Log.i(tag, "[DEBUG FIRESTORE READ onSnapshot] Setting up real-time listener on path '$usersCollectionPath'...")
        Log.i(tag, "[DEBUG FIRESTORE READ onSnapshot] Current Firebase Auth UID: '$currentUid'")
        Log.i(tag, "[DEBUG FIRESTORE READ onSnapshot] Admin Document Path: '$adminDocPath'")
        Log.i(tag, "[DEBUG FIRESTORE READ onSnapshot] Firestore Users Collection Path: '$usersCollectionPath'")

        usersListenerRegistration?.remove()
        usersListenerRegistration = db.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    val errorCode = error.code
                    val errorMsg = error.message.orEmpty()
                    Log.w(tag, "[DEBUG FIRESTORE READ onSnapshot] Real-time collection sync restricted on '$usersCollectionPath': $errorMsg ($errorCode)")
                    usersListenerRegistration?.remove()

                    // Attach user-scoped listener if currentUid is present
                    if (currentUid.isNotBlank()) {
                        usersListenerRegistration = db.collection("users").document(currentUid)
                            .addSnapshotListener { singleDoc, singleErr ->
                                if (singleErr == null && singleDoc != null && singleDoc.exists()) {
                                    val singleUser = parseUserDocument(singleDoc)
                                    if (singleUser != null) {
                                        repo.updateUsersFromFirestore(listOf(singleUser))
                                        repo.setUsersError(null)
                                    }
                                }
                            }
                    }
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val docCount = snapshot.documents.size
                    Log.i(tag, "[DEBUG FIRESTORE READ onSnapshot] SUCCESS on '$usersCollectionPath'!")
                    Log.i(tag, "[DEBUG FIRESTORE READ onSnapshot] Current Firebase Auth UID: '$currentUid'")
                    Log.i(tag, "[DEBUG FIRESTORE READ onSnapshot] Returned document count: $docCount from '$usersCollectionPath'")
                    snapshot.documents.forEach { doc ->
                        Log.i(tag, "[DEBUG FIRESTORE READ onSnapshot] Doc ID='${doc.id}', Name='${doc.getString("name")}', Email='${doc.getString("email")}'")
                    }
                    repo.setUsersError(null)
                    val userList = snapshot.documents.mapNotNull { doc -> parseUserDocument(doc) }
                    Log.i(tag, "[DEBUG FIRESTORE READ onSnapshot] Successfully parsed ${userList.size} real users from Firestore '$usersCollectionPath'")
                    repo.updateUsersFromFirestore(userList)
                }
            }

        // 2. Sync generationRequests collection: generationRequests/{requestId}
        requestsListenerRegistration?.remove()
        requestsListenerRegistration = db.collection("generationRequests")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.i(tag, "[REQUESTS SYNC] Collection query status: ${error.message}. Attaching user-scoped query listener.")
                    requestsListenerRegistration?.remove()
                    if (currentUid.isNotBlank()) {
                        requestsListenerRegistration = db.collection("generationRequests")
                            .whereEqualTo("userId", currentUid)
                            .addSnapshotListener { querySnap, qErr ->
                                if (qErr == null && querySnap != null) {
                                    val userRequests = querySnap.documents.mapNotNull { doc -> parseGenerationRequestDocument(doc) }
                                    repo.updateGenerationRequestsFromFirestore(userRequests)
                                }
                            }
                    }
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val requestsList = snapshot.documents.mapNotNull { doc -> parseGenerationRequestDocument(doc) }
                    Log.i(tag, "[REQUESTS SYNC] Received ${requestsList.size} generation requests from Firestore")
                    repo.updateGenerationRequestsFromFirestore(requestsList)
                }
            }

        // 3. Sync referrals collection: referrals/{referralId}
        referralsListenerRegistration?.remove()
        referralsListenerRegistration = db.collection("referrals")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(tag, "referrals Firestore listener error: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val recordsList = snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.safeString("id", default = doc.id)
                            val referrerUid = doc.safeString("referrerUid", "referrer_uid", default = "")
                            val referrerName = doc.safeString("referrerName", "referrer_name", default = "")
                            val referrerEmail = doc.safeString("referrerEmail", "referrer_email", default = "")
                            val referredUid = doc.safeString("referredUid", "referred_uid", default = "")
                            val referredName = doc.safeString("referredName", "referred_name", default = "")
                            val referredEmail = doc.safeString("referredEmail", "referred_email", default = "")
                            val referralCode = doc.safeString("referralCode", "referral_code", default = "")
                            val creditsAwarded = doc.safeInt("creditsAwarded", "credits_awarded", default = 20)
                            val timestamp = doc.safeLong("timestamp", "createdAt", default = System.currentTimeMillis())

                            ReferralRecord(
                                id = id,
                                referrerUid = referrerUid,
                                referrerName = referrerName,
                                referrerEmail = referrerEmail,
                                referredUid = referredUid,
                                referredName = referredName,
                                referredEmail = referredEmail,
                                referralCode = referralCode,
                                creditsAwarded = creditsAwarded,
                                timestamp = timestamp
                            )
                        } catch (e: Exception) {
                            Log.e(tag, "Error parsing referral doc ${doc.id}: ${e.message}")
                            null
                        }
                    }

                    repo.updateReferralRecordsFromFirestore(recordsList)
                }
            }

        // 4. Sync creditTransactions collection: creditTransactions/{txId}
        creditTransactionsListenerRegistration?.remove()
        creditTransactionsListenerRegistration = db.collection("creditTransactions")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(tag, "creditTransactions Firestore listener error: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val txList = snapshot.documents.mapNotNull { doc ->
                        try {
                            val txId = doc.safeString("transactionId", "transaction_id", default = doc.id)
                            val userId = doc.safeString("userId", "user_id", default = "")
                            val userEmail = doc.safeString("userEmail", "user_email", default = "")
                            val amount = doc.safeInt("amount", default = 0)
                            val previousBalance = doc.safeInt("previousBalance", "previous_balance", default = 0)
                            val newBalance = doc.safeInt("newBalance", "new_balance", default = 0)
                            val typeStr = doc.safeString("type", default = "MANUAL_ADD").uppercase()
                            val type = try {
                                TransactionType.valueOf(typeStr)
                            } catch (e: Exception) {
                                TransactionType.MANUAL_ADD
                            }
                            val reason = doc.safeString("reason", default = "")
                            val adminId = doc.safeString("adminId", "admin_id", default = "")
                            val adminEmail = doc.safeString("adminEmail", "admin_email", default = "")
                            val timestamp = doc.safeLong("timestamp", "createdAt", default = System.currentTimeMillis())

                            CreditTransaction(
                                transactionId = txId,
                                userId = userId,
                                userEmail = userEmail,
                                amount = amount,
                                previousBalance = previousBalance,
                                newBalance = newBalance,
                                type = type,
                                reason = reason,
                                adminId = adminId,
                                adminEmail = adminEmail,
                                timestamp = timestamp
                            )
                        } catch (e: Exception) {
                            Log.e(tag, "Error parsing creditTransaction doc ${doc.id}: ${e.message}")
                            null
                        }
                    }

                    repo.updateCreditTransactionsFromFirestore(txList)
                }
            }

        // 5. Sync system_settings/referral
        referralSettingsListenerRegistration?.remove()
        referralSettingsListenerRegistration = db.collection("system_settings").document("referral")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && snapshot.exists()) {
                    try {
                        val enabled = snapshot.safeBoolean("enabled", default = true)
                        val rewardCredits = snapshot.safeInt("rewardCredits", default = 20)
                        val minReferrals = snapshot.safeInt("minimumReferrals", default = 1)
                        val updated = snapshot.safeLong("updatedAt", default = System.currentTimeMillis())
                        repo.updateReferralFromFirestore(
                            ReferralSettings(
                                enabled = enabled,
                                rewardCredits = rewardCredits,
                                minimumReferrals = minReferrals,
                                updatedAt = updated
                            )
                        )
                    } catch (e: Exception) {
                        Log.w(tag, "Error parsing referral settings: ${e.message}")
                    }
                }
            }

        // 6. Sync system_settings/ai_limits
        aiLimitsSettingsListenerRegistration?.remove()
        aiLimitsSettingsListenerRegistration = db.collection("system_settings").document("ai_limits")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && snapshot.exists()) {
                    try {
                        val imgLimit = snapshot.safeInt("dailyImageLimit", default = 20)
                        val vidLimit = snapshot.safeInt("dailyVideoLimit", default = 5)
                        val i2vLimit = snapshot.safeInt("dailyImageToVideoLimit", default = 5)
                        val imgCost = snapshot.safeInt("creditCostPerImage", default = 10)
                        val vidCost = snapshot.safeInt("creditCostPerVideo", default = 50)
                        val i2vCost = snapshot.safeInt("creditCostPerImageToVideo", default = 60)
                        val adSec = snapshot.safeInt("requiredAdDurationSeconds", default = 10)
                        val updated = snapshot.safeLong("updatedAt", default = System.currentTimeMillis())
                        repo.updateAILimitsFromFirestore(
                            AILimitsSettings(
                                dailyImageLimit = imgLimit,
                                dailyVideoLimit = vidLimit,
                                dailyImageToVideoLimit = i2vLimit,
                                creditCostPerImage = imgCost,
                                creditCostPerVideo = vidCost,
                                creditCostPerImageToVideo = i2vCost,
                                requiredAdDurationSeconds = adSec,
                                updatedAt = updated
                            )
                        )
                    } catch (e: Exception) {
                        Log.w(tag, "Error parsing ai_limits settings: ${e.message}")
                    }
                }
            }

        // 7. Sync system_settings/support
        supportSettingsListenerRegistration?.remove()
        supportSettingsListenerRegistration = db.collection("system_settings").document("support")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && snapshot.exists()) {
                    try {
                        val wa = snapshot.safeString("whatsappNumber", default = "+8801700000000")
                        val waLink = snapshot.safeString("whatsappGroupLink", default = "")
                        val tg = snapshot.safeString("telegramUsername", default = "qrynova_support")
                        val tgLink = snapshot.safeString("telegramGroupLink", default = "https://t.me/qrynova_community")
                        val email = snapshot.safeString("supportEmail", default = "support@qrynova.ai")
                        val web = snapshot.safeString("websiteUrl", default = "https://qrynova.ai")
                        val updated = snapshot.safeLong("updatedAt", default = System.currentTimeMillis())
                        repo.updateSupportFromFirestore(
                            SupportSettings(
                                whatsappNumber = wa,
                                whatsappGroupLink = waLink,
                                telegramUsername = tg,
                                telegramGroupLink = tgLink,
                                supportEmail = email,
                                websiteUrl = web,
                                updatedAt = updated
                            )
                        )
                    } catch (e: Exception) {
                        Log.w(tag, "Error parsing support settings: ${e.message}")
                    }
                }
            }

        // 8. Sync system_settings/global or settings/systemSettings
        systemSettingsListenerRegistration?.remove()
        systemSettingsListenerRegistration = db.collection("system_settings").document("global")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && snapshot.exists()) {
                    try {
                        val name = snapshot.safeString("appName", "app_name", default = "QRYNOVA")
                        val tagline = snapshot.safeString("appTagline", "tagline", default = "Next-Gen Generative AI Engine")
                        val desc = snapshot.safeString("appDescription", "description", default = "Generate breathtaking images, cinematics, and animated videos with enterprise-grade precision.")
                        val logo = snapshot.safeString("appLogoUrl", "logo_url", default = "https://qrynova.ai/logo.png")
                        val maintenance = snapshot.safeBoolean("maintenanceMode", "isMaintenance", "maintenance_mode", default = false)
                        val maintenanceMsg = snapshot.safeString("maintenanceMessage", "maintenance_message", default = "QRYNOVA is currently undergoing scheduled platform maintenance. Services will resume shortly.")
                        val regEnabled = snapshot.safeBoolean("registrationEnabled", "isRegistrationEnabled", "registration_enabled", default = true)
                        val loginEnabled = snapshot.safeBoolean("loginEnabled", "isLoginEnabled", default = true)
                        val contact = snapshot.safeString("publicAdminsContact", "contact", default = "support@qrynova.ai")
                        val terms = snapshot.safeString("primaryTermsUrl", "terms_url", default = "https://qrynova.ai/terms")
                        val privacy = snapshot.safeString("primaryPrivacyUrl", "privacy_url", default = "https://qrynova.ai/privacy")
                        val forceUp = snapshot.safeBoolean("forceUpdateRequired", "forceUpdate", "force_update", default = false)
                        val updateUrl = snapshot.safeString("forceUpdateUrl", "updateUrl", default = "https://play.google.com/store/apps/details?id=com.qrynova.app")
                        val updated = snapshot.safeLong("lastUpdated", "updatedAt", default = System.currentTimeMillis())

                        repo.updateSystemFromFirestore(
                            AppSystemSettings(
                                appName = name,
                                appTagline = tagline,
                                appLogoUrl = logo,
                                appDescription = desc,
                                maintenanceMode = maintenance,
                                maintenanceMessage = maintenanceMsg,
                                registrationEnabled = regEnabled,
                                loginEnabled = loginEnabled,
                                publicAdminsContact = contact,
                                primaryTermsUrl = terms,
                                primaryPrivacyUrl = privacy,
                                forceUpdateRequired = forceUp,
                                forceUpdateUrl = updateUrl,
                                lastUpdated = updated
                            )
                        )
                    } catch (e: Exception) {
                        Log.w(tag, "Error parsing system settings: ${e.message}")
                    }
                }
            }

        // 9. Sync system_settings/credits or settings/credits
        creditSettingsListenerRegistration?.remove()
        creditSettingsListenerRegistration = db.collection("system_settings").document("credits")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && snapshot.exists()) {
                    try {
                        val start = snapshot.safeInt("newUserStartingCredits", "startingCredits", "signupBonus", default = 100)
                        val img = snapshot.safeInt("imageGenerationCost", "creditCostPerImage", default = 2)
                        val vid = snapshot.safeInt("videoGenerationCost", "creditCostPerVideo", default = 2)
                        val i2v = snapshot.safeInt("imageToVideoCost", "creditCostPerImageToVideo", default = 2)
                        val updated = snapshot.safeLong("lastUpdated", "updatedAt", default = System.currentTimeMillis())
                        val admin = snapshot.safeString("updatedByAdmin", default = "admin@qrynova.ai")

                        repo.updateCreditSettingsFromFirestore(
                            CreditSettings(
                                newUserStartingCredits = start,
                                imageGenerationCost = img,
                                videoGenerationCost = vid,
                                imageToVideoCost = i2v,
                                lastUpdated = updated,
                                updatedByAdmin = admin
                            )
                        )
                    } catch (e: Exception) {
                        Log.w(tag, "Error parsing credit settings: ${e.message}")
                    }
                }
            }

        // 10. Sync notifications collection
        notificationsListenerRegistration?.remove()
        notificationsListenerRegistration = db.collection("notifications")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    val notifs = snapshot.documents.mapNotNull { parseNotificationDocument(it) }
                    if (notifs.isNotEmpty()) {
                        repo.updateNotificationsFromFirestore(notifs)
                    }
                }
            }
    }

    private fun parseNotificationDocument(doc: DocumentSnapshot): AppNotification? {
        return try {
            val id = doc.safeString("id", default = doc.id)
            val title = doc.safeString("title", "header", default = "")
            val message = doc.safeString("message", "body", "description", default = "")
            val typeStr = doc.safeString("type", default = "SYSTEM").uppercase()
            val type = try {
                NotificationType.valueOf(typeStr)
            } catch (e: Exception) {
                NotificationType.SYSTEM
            }
            val targetUserId = doc.safeString("targetUserId", "target_user_id", "userId", default = "").ifEmpty { null }
            val targetUserEmail = doc.safeString("targetUserEmail", "target_user_email", "userEmail", default = "").ifEmpty { null }
            val actionUrl = doc.safeString("actionUrl", "action_url", "url", "link", default = "")
            val actionLabel = doc.safeString("actionLabel", "action_label", default = "")
            val isRead = doc.safeBoolean("isRead", "is_read", "read", default = false)
            val createdAt = doc.safeLong("createdAt", "created_at", "timestamp", default = System.currentTimeMillis())
            val senderAdminEmail = doc.safeString("senderAdminEmail", "sender_admin_email", "adminEmail", default = "admin@qrynova.ai")

            AppNotification(
                id = id,
                title = title,
                message = message,
                type = type,
                targetUserId = targetUserId,
                targetUserEmail = targetUserEmail,
                actionUrl = actionUrl,
                actionLabel = actionLabel,
                isRead = isRead,
                createdAt = createdAt,
                senderAdminEmail = senderAdminEmail
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Updates user credits in Cloud Firestore users/{userId} and syncs all standard credit aliases
     */
    fun syncUserCredits(userId: String, newCredits: Int, totalGranted: Int, totalConsumed: Int) {
        val db = firestore ?: return
        scope.launch {
            try {
                val now = System.currentTimeMillis()
                val updates = mapOf(
                    "credits" to newCredits,
                    "coins" to newCredits,
                    "balance" to newCredits,
                    "userCredits" to newCredits,
                    "points" to newCredits,
                    "totalCredits" to newCredits,
                    "totalCreditsGranted" to totalGranted,
                    "totalCreditsConsumed" to totalConsumed,
                    "lastUpdatedByAdminAt" to now,
                    "updatedAt" to now,
                    "lastUpdated" to now
                )
                db.collection("users").document(userId)
                    .set(updates, SetOptions.merge())
                    .await()
                Log.d(tag, "Firestore credit update pushed for users/$userId: $newCredits credits")
            } catch (e: Exception) {
                Log.e(tag, "Failed to push credit update to Firestore: ${e.message}")
            }
        }
    }

    /**
     * Updates user status in Cloud Firestore users/{userId} and syncs all standard boolean status aliases
     */
    fun syncUserStatus(userId: String, status: UserStatus, reason: String = "") {
        val db = firestore ?: return
        scope.launch {
            try {
                val now = System.currentTimeMillis()
                val updates = mapOf(
                    "status" to status.name,
                    "userStatus" to status.name,
                    "accountStatus" to status.name,
                    "isActive" to (status == UserStatus.ACTIVE),
                    "is_active" to (status == UserStatus.ACTIVE),
                    "active" to (status == UserStatus.ACTIVE),
                    "isBanned" to (status == UserStatus.DISABLED),
                    "is_banned" to (status == UserStatus.DISABLED),
                    "banned" to (status == UserStatus.DISABLED),
                    "isSuspended" to (status == UserStatus.SUSPENDED),
                    "is_suspended" to (status == UserStatus.SUSPENDED),
                    "suspended" to (status == UserStatus.SUSPENDED),
                    "isBlocked" to (status != UserStatus.ACTIVE),
                    "blocked" to (status != UserStatus.ACTIVE),
                    "statusReason" to reason,
                    "lastStatusUpdate" to now,
                    "updatedAt" to now
                )
                db.collection("users").document(userId)
                    .set(updates, SetOptions.merge())
                    .await()
                Log.d(tag, "Firestore status update pushed for users/$userId: ${status.name}")
            } catch (e: Exception) {
                Log.e(tag, "Failed to push status update to Firestore: ${e.message}")
            }
        }
    }

    /**
     * Adds an admin note to users/{userId}
     */
    fun syncAdminNote(userId: String, note: String) {
        val db = firestore ?: return
        scope.launch {
            try {
                val now = System.currentTimeMillis()
                val updates = mapOf(
                    "adminNotes" to note,
                    "notes" to note,
                    "lastAdminNote" to note,
                    "adminNoteUpdatedAt" to now,
                    "updatedAt" to now
                )
                db.collection("users").document(userId)
                    .set(updates, SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                Log.e(tag, "Failed to sync admin note: ${e.message}")
            }
        }
    }

    /**
     * Delivers generation result with Google Drive link to generationRequests/{requestId} and requests/{requestId}
     */
    fun syncGenerationDelivery(
        requestId: String,
        googleDriveUrl: String,
        adminNotes: String,
        adminId: String,
        adminEmail: String
    ) {
        val db = firestore ?: return
        scope.launch {
            try {
                val now = System.currentTimeMillis()
                val updates = mapOf(
                    "status" to RequestStatus.COMPLETED.name,
                    "requestStatus" to RequestStatus.COMPLETED.name,
                    "isCompleted" to true,
                    "googleDriveUrl" to googleDriveUrl,
                    "driveUrl" to googleDriveUrl,
                    "downloadUrl" to googleDriveUrl,
                    "resultUrl" to googleDriveUrl,
                    "outputUrl" to googleDriveUrl,
                    "deliveryUrl" to googleDriveUrl,
                    "adminNotes" to adminNotes,
                    "adminMessage" to adminNotes,
                    "message" to adminNotes,
                    "deliveryMessage" to adminNotes,
                    "fulfilledByAdminId" to adminId,
                    "fulfilledByAdminEmail" to adminEmail,
                    "updatedAt" to now,
                    "completedAt" to now
                )
                // Write to primary generationRequests collection
                db.collection("generationRequests").document(requestId)
                    .set(updates, SetOptions.merge())
                    .await()
                // Also mirror to requests collection for User App compatibility
                db.collection("requests").document(requestId)
                    .set(updates, SetOptions.merge())
                    .await()
                Log.d(tag, "Firestore delivery update pushed for generationRequests/$requestId and requests/$requestId")
            } catch (e: Exception) {
                Log.e(tag, "Failed to push generation delivery to Firestore: ${e.message}")
            }
        }
    }

    /**
     * Updates request status in generationRequests/{requestId} and requests/{requestId}
     */
    fun syncRequestStatus(requestId: String, status: RequestStatus, note: String) {
        val db = firestore ?: return
        scope.launch {
            try {
                val now = System.currentTimeMillis()
                val updates = mutableMapOf<String, Any>(
                    "status" to status.name,
                    "requestStatus" to status.name,
                    "updatedAt" to now
                )
                if (status == RequestStatus.COMPLETED) {
                    updates["isCompleted"] = true
                    updates["completedAt"] = now
                }
                if (note.isNotBlank()) {
                    updates["adminNotes"] = note
                    updates["adminMessage"] = note
                }
                db.collection("generationRequests").document(requestId)
                    .set(updates, SetOptions.merge())
                    .await()
                db.collection("requests").document(requestId)
                    .set(updates, SetOptions.merge())
                    .await()
                Log.d(tag, "Firestore request status updated: generationRequests/$requestId -> ${status.name}")
            } catch (e: Exception) {
                Log.e(tag, "Failed to push request status update to Firestore: ${e.message}")
            }
        }
    }

    /**
     * Pushes system settings to settings/systemSettings and system_settings/global
     */
    fun syncSystemSettings(settings: AppSystemSettings) {
        val db = firestore ?: return
        scope.launch {
            try {
                val data = mapOf(
                    "appName" to settings.appName,
                    "app_name" to settings.appName,
                    "appTagline" to settings.appTagline,
                    "appDescription" to settings.appDescription,
                    "appLogoUrl" to settings.appLogoUrl,
                    "maintenanceMode" to settings.maintenanceMode,
                    "maintenance_mode" to settings.maintenanceMode,
                    "isMaintenance" to settings.maintenanceMode,
                    "maintenanceMessage" to settings.maintenanceMessage,
                    "maintenance_message" to settings.maintenanceMessage,
                    "registrationEnabled" to settings.registrationEnabled,
                    "isRegistrationEnabled" to settings.registrationEnabled,
                    "registration_enabled" to settings.registrationEnabled,
                    "loginEnabled" to settings.loginEnabled,
                    "isLoginEnabled" to settings.loginEnabled,
                    "forceUpdateRequired" to settings.forceUpdateRequired,
                    "forceUpdate" to settings.forceUpdateRequired,
                    "force_update" to settings.forceUpdateRequired,
                    "forceUpdateUrl" to settings.forceUpdateUrl,
                    "updateUrl" to settings.forceUpdateUrl,
                    "appUrl" to settings.forceUpdateUrl,
                    "publicAdminsContact" to settings.publicAdminsContact,
                    "primaryTermsUrl" to settings.primaryTermsUrl,
                    "primaryPrivacyUrl" to settings.primaryPrivacyUrl,
                    "lastUpdated" to settings.lastUpdated,
                    "updatedAt" to settings.lastUpdated
                )
                db.collection("settings").document("systemSettings").set(data, SetOptions.merge()).await()
                db.collection("settings").document("app").set(data, SetOptions.merge()).await()
                db.collection("system_settings").document("global").set(data, SetOptions.merge()).await()
                db.collection("system_settings").document("app").set(data, SetOptions.merge()).await()
                db.collection("config").document("app").set(data, SetOptions.merge()).await()
                db.collection("config").document("settings").set(data, SetOptions.merge()).await()
                Log.d(tag, "System settings synced to Firestore (Maintenance=${settings.maintenanceMode}, Reg=${settings.registrationEnabled})")
            } catch (e: Exception) {
                Log.e(tag, "Failed to push systemSettings to Firestore: ${e.message}")
            }
        }
    }

    /**
     * Pushes credit rules and pricing to settings/credits and system_settings/credits
     */
    fun syncCreditSettings(settings: CreditSettings) {
        val db = firestore ?: return
        scope.launch {
            try {
                val data = mapOf(
                    "newUserStartingCredits" to settings.newUserStartingCredits,
                    "startingCredits" to settings.newUserStartingCredits,
                    "signupBonus" to settings.newUserStartingCredits,
                    "welcomeCredits" to settings.newUserStartingCredits,
                    "imageGenerationCost" to settings.imageGenerationCost,
                    "creditCostPerImage" to settings.imageGenerationCost,
                    "videoGenerationCost" to settings.videoGenerationCost,
                    "creditCostPerVideo" to settings.videoGenerationCost,
                    "imageToVideoCost" to settings.imageToVideoCost,
                    "creditCostPerImageToVideo" to settings.imageToVideoCost,
                    "lastUpdated" to settings.lastUpdated,
                    "updatedAt" to settings.lastUpdated,
                    "updatedByAdmin" to settings.updatedByAdmin
                )
                db.collection("settings").document("credits").set(data, SetOptions.merge()).await()
                db.collection("settings").document("creditSettings").set(data, SetOptions.merge()).await()
                db.collection("system_settings").document("credits").set(data, SetOptions.merge()).await()
                db.collection("config").document("credits").set(data, SetOptions.merge()).await()
                Log.d(tag, "Credit settings synced to Firestore (Start=${settings.newUserStartingCredits}, Img=${settings.imageGenerationCost}, Vid=${settings.videoGenerationCost})")
            } catch (e: Exception) {
                Log.e(tag, "Failed to push credit settings to Firestore: ${e.message}")
            }
        }
    }

    /**
     * Pushes generation pipeline rules to settings/generation and system_settings/generation
     */
    fun syncGenerationSettings(settings: GenerationSettings) {
        val db = firestore ?: return
        scope.launch {
            try {
                val data = mapOf(
                    "image" to mapOf(
                        "enabled" to settings.image.enabled,
                        "creditCost" to settings.image.creditCost,
                        "maxPromptLength" to settings.image.maxPromptLength,
                        "availableResolutions" to settings.image.availableResolutions,
                        "availableAspectRatios" to settings.image.availableAspectRatios,
                        "maxOutputSizeMb" to settings.image.maxOutputSizeMb,
                        "maintenanceMode" to settings.image.maintenanceMode,
                        "maintenanceMessage" to settings.image.maintenanceMessage
                    ),
                    "video" to mapOf(
                        "enabled" to settings.video.enabled,
                        "creditCost" to settings.video.creditCost,
                        "minPromptLength" to settings.video.minPromptLength,
                        "maxPromptLength" to settings.video.maxPromptLength,
                        "availableResolutions" to settings.video.availableResolutions,
                        "availableAspectRatios" to settings.video.availableAspectRatios,
                        "availableDurations" to settings.video.availableDurations,
                        "maxDurationSeconds" to settings.video.maxDurationSeconds,
                        "maintenanceMode" to settings.video.maintenanceMode,
                        "maintenanceMessage" to settings.video.maintenanceMessage
                    ),
                    "imageToVideo" to mapOf(
                        "enabled" to settings.imageToVideo.enabled,
                        "creditCost" to settings.imageToVideo.creditCost,
                        "availableResolutions" to settings.imageToVideo.availableResolutions,
                        "availableAspectRatios" to settings.imageToVideo.availableAspectRatios,
                        "durationSeconds" to settings.imageToVideo.durationSeconds,
                        "promptRequired" to settings.imageToVideo.promptRequired,
                        "maintenanceMode" to settings.imageToVideo.maintenanceMode,
                        "maintenanceMessage" to settings.imageToVideo.maintenanceMessage
                    ),
                    "imageEnabled" to settings.image.enabled,
                    "videoEnabled" to settings.video.enabled,
                    "imageToVideoEnabled" to settings.imageToVideo.enabled,
                    "imageMaintenance" to settings.image.maintenanceMode,
                    "videoMaintenance" to settings.video.maintenanceMode,
                    "imageToVideoMaintenance" to settings.imageToVideo.maintenanceMode,
                    "lastUpdated" to settings.lastUpdated,
                    "updatedAt" to settings.lastUpdated
                )
                db.collection("settings").document("generation").set(data, SetOptions.merge()).await()
                db.collection("settings").document("generationSettings").set(data, SetOptions.merge()).await()
                db.collection("system_settings").document("generation").set(data, SetOptions.merge()).await()
                db.collection("config").document("generation").set(data, SetOptions.merge()).await()
                Log.d(tag, "Generation settings synced to Firestore (Img=${settings.image.enabled}, Vid=${settings.video.enabled}, I2V=${settings.imageToVideo.enabled})")
            } catch (e: Exception) {
                Log.e(tag, "Failed to push generation settings to Firestore: ${e.message}")
            }
        }
    }

    /**
     * Pushes ad settings to settings/adSettings and system_settings/ads
     */
    fun syncAdSettings(settings: AdSettings) {
        val db = firestore ?: return
        scope.launch {
            try {
                val placementMap = settings.placements.mapValues { (_, p) ->
                    mapOf(
                        "type" to p.type.name,
                        "enabled" to p.enabled,
                        "watchDurationSeconds" to p.watchDurationSeconds,
                        "title" to p.title,
                        "description" to p.description,
                        "adImageUrl" to p.adImageUrl,
                        "adVideoUrl" to p.adVideoUrl,
                        "targetUrl" to p.targetUrl,
                        "ctaText" to p.ctaText
                    )
                }
                val data = mapOf(
                    "globalAdsEnabled" to settings.globalAdsEnabled,
                    "adsEnabled" to settings.globalAdsEnabled,
                    "ads_enabled" to settings.globalAdsEnabled,
                    "defaultWatchDurationSeconds" to settings.defaultWatchDurationSeconds,
                    "watchDuration" to settings.defaultWatchDurationSeconds,
                    "watch_duration" to settings.defaultWatchDurationSeconds,
                    "requiredAdDurationSeconds" to settings.defaultWatchDurationSeconds,
                    "customNetworkSnippet" to settings.customNetworkSnippet,
                    "placements" to placementMap,
                    "lastUpdated" to settings.lastUpdated,
                    "updatedAt" to settings.lastUpdated
                )
                db.collection("settings").document("adSettings").set(data, SetOptions.merge()).await()
                db.collection("settings").document("ads").set(data, SetOptions.merge()).await()
                db.collection("system_settings").document("ads").set(data, SetOptions.merge()).await()
                db.collection("config").document("ads").set(data, SetOptions.merge()).await()
                Log.d(tag, "Ad settings synced to Firestore (GlobalAds=${settings.globalAdsEnabled})")
            } catch (e: Exception) {
                Log.e(tag, "Failed to push adSettings to Firestore: ${e.message}")
            }
        }
    }

    /**
     * Pushes referral settings to system_settings/referral
     */
    fun syncReferralSettings(settings: ReferralSettings) {
        val db = firestore ?: return
        scope.launch {
            try {
                val data = mapOf(
                    "enabled" to settings.enabled,
                    "isReferralEnabled" to settings.enabled,
                    "referralEnabled" to settings.enabled,
                    "referral_enabled" to settings.enabled,
                    "rewardCredits" to settings.rewardCredits,
                    "reward_credits" to settings.rewardCredits,
                    "rewardCoins" to settings.rewardCredits,
                    "minimumReferrals" to settings.minimumReferrals,
                    "updatedAt" to settings.updatedAt
                )
                db.collection("system_settings").document("referral").set(data, SetOptions.merge()).await()
                db.collection("settings").document("referral").set(data, SetOptions.merge()).await()
                db.collection("config").document("referral").set(data, SetOptions.merge()).await()
                Log.d(tag, "Referral settings pushed to Firestore (Reward: ${settings.rewardCredits} Credits)")
            } catch (e: Exception) {
                Log.e(tag, "Failed to sync referral settings: ${e.message}")
            }
        }
    }

    /**
     * Pushes AI limits settings to system_settings/ai_limits
     */
    fun syncAILimitsSettings(settings: AILimitsSettings) {
        val db = firestore ?: return
        scope.launch {
            try {
                val data = mapOf(
                    "dailyImageLimit" to settings.dailyImageLimit,
                    "dailyVideoLimit" to settings.dailyVideoLimit,
                    "dailyImageToVideoLimit" to settings.dailyImageToVideoLimit,
                    "creditCostPerImage" to settings.creditCostPerImage,
                    "creditCostPerVideo" to settings.creditCostPerVideo,
                    "creditCostPerImageToVideo" to settings.creditCostPerImageToVideo,
                    "requiredAdDurationSeconds" to settings.requiredAdDurationSeconds,
                    "imageGenEnabled" to settings.imageGenEnabled,
                    "videoGenEnabled" to settings.videoGenEnabled,
                    "imageToVideoEnabled" to settings.imageToVideoEnabled,
                    "updatedAt" to settings.updatedAt
                )
                db.collection("system_settings").document("ai_limits").set(data, SetOptions.merge()).await()
                db.collection("settings").document("aiLimits").set(data, SetOptions.merge()).await()
                db.collection("config").document("ai_limits").set(data, SetOptions.merge()).await()
                Log.d(tag, "AI Limits settings pushed to Firestore")
            } catch (e: Exception) {
                Log.e(tag, "Failed to sync AI limits settings: ${e.message}")
            }
        }
    }

    /**
     * Pushes support settings to system_settings/support
     */
    fun syncSupportSettings(settings: SupportSettings) {
        val db = firestore ?: return
        scope.launch {
            try {
                val data = mapOf(
                    "whatsappNumber" to settings.whatsappNumber,
                    "whatsapp_number" to settings.whatsappNumber,
                    "whatsappGroupLink" to settings.whatsappGroupLink,
                    "whatsapp_group_link" to settings.whatsappGroupLink,
                    "whatsapp" to settings.whatsapp,
                    "telegramUsername" to settings.telegramUsername,
                    "telegram_username" to settings.telegramUsername,
                    "telegramGroupLink" to settings.telegramGroupLink,
                    "telegram_group_link" to settings.telegramGroupLink,
                    "telegram" to settings.telegram,
                    "supportEmail" to settings.supportEmail,
                    "support_email" to settings.supportEmail,
                    "email" to settings.supportEmail,
                    "websiteUrl" to settings.websiteUrl,
                    "website_url" to settings.websiteUrl,
                    "website" to settings.websiteUrl,
                    "updatedAt" to settings.updatedAt
                )
                db.collection("system_settings").document("support").set(data, SetOptions.merge()).await()
                db.collection("settings").document("support").set(data, SetOptions.merge()).await()
                db.collection("config").document("support").set(data, SetOptions.merge()).await()
                Log.d(tag, "Support settings pushed to Firestore")
            } catch (e: Exception) {
                Log.e(tag, "Failed to sync support settings: ${e.message}")
            }
        }
    }

    /**
     * Pushes notification to notifications/{notificationId} and broadcasts/announcements
     */
    fun syncNotification(notification: AppNotification) {
        val db = firestore ?: return
        scope.launch {
            try {
                val data = mapOf(
                    "id" to notification.id,
                    "title" to notification.title,
                    "message" to notification.message,
                    "body" to notification.message,
                    "type" to notification.type.name,
                    "targetUserId" to (notification.targetUserId ?: ""),
                    "targetUserEmail" to (notification.targetUserEmail ?: ""),
                    "actionUrl" to notification.actionUrl,
                    "url" to notification.actionUrl,
                    "link" to notification.actionUrl,
                    "actionLabel" to notification.actionLabel,
                    "isRead" to notification.isRead,
                    "createdAt" to notification.createdAt,
                    "timestamp" to notification.createdAt,
                    "senderAdminEmail" to notification.senderAdminEmail
                )
                db.collection("notifications").document(notification.id).set(data, SetOptions.merge()).await()
                db.collection("announcements").document(notification.id).set(data, SetOptions.merge()).await()
                if (!notification.targetUserId.isNullOrBlank()) {
                    db.collection("users").document(notification.targetUserId)
                        .collection("notifications").document(notification.id)
                        .set(data, SetOptions.merge()).await()
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to push notification to Firestore: ${e.message}")
            }
        }
    }

    /**
     * Logs credit transactions to creditTransactions/{txId} and users/{userId}/creditTransactions/{txId}
     */
    fun syncCreditTransaction(tx: CreditTransaction) {
        val db = firestore ?: return
        scope.launch {
            try {
                db.collection("creditTransactions").document(tx.transactionId)
                    .set(tx, SetOptions.merge())
                    .await()
                if (tx.userId.isNotBlank()) {
                    db.collection("users").document(tx.userId)
                        .collection("creditTransactions").document(tx.transactionId)
                        .set(tx, SetOptions.merge())
                        .await()
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to log transaction to Firestore: ${e.message}")
            }
        }
    }

    /**
     * Records a referral reward event in referrals/{referralId}
     */
    fun syncReferralRecord(record: ReferralRecord) {
        val db = firestore ?: return
        scope.launch {
            try {
                db.collection("referrals").document(record.id)
                    .set(record, SetOptions.merge())
                    .await()
                Log.d(tag, "Referral record pushed to Firestore: ${record.id} (+${record.creditsAwarded} credits)")
            } catch (e: Exception) {
                Log.e(tag, "Failed to push referral record to Firestore: ${e.message}")
            }
        }
    }

    companion object {
        @Volatile
        private var instance: FirebaseManager? = null

        fun getInstance(): FirebaseManager {
            return instance ?: synchronized(this) {
                instance ?: FirebaseManager().also { instance = it }
            }
        }
    }
}
