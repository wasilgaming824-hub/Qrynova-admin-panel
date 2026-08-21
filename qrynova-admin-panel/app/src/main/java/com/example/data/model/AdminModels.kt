package com.example.data.model

enum class AdminRole(val label: String) {
    SUPER_ADMIN("Super Administrator"),
    ADMIN("Administrator"),
    OPERATOR("Generation Operator")
}

data class AdminAccount(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: AdminRole = AdminRole.SUPER_ADMIN,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis()
)

data class AdminAuditLog(
    val logId: String = "",
    val adminId: String = "",
    val adminEmail: String = "",
    val action: String = "",
    val targetType: String = "", // USER, CREDIT, REQUEST, SETTINGS, ADS, NOTIFICATION
    val targetId: String = "",
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class SystemHealthState(
    val firebaseAuthConnected: Boolean = true,
    val firestoreConnected: Boolean = true,
    val configLoaded: Boolean = true,
    val activeListenersCount: Int = 8,
    val projectId: String = "qrynova-production-app",
    val environmentMode: String = "Cloud Firestore + Live Reactive Bridge",
    val latencyMs: Long = 42,
    val lastSyncedAt: Long = System.currentTimeMillis()
)
