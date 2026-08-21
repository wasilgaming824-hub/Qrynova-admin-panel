package com.example.data.model

enum class NotificationType(val label: String) {
    INFO("Information"),
    SUCCESS("Success"),
    WARNING("Warning / Alert"),
    GENERATION("Generation Complete"),
    SYSTEM("System Announcement")
}

data class AppNotification(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: NotificationType = NotificationType.INFO,
    val targetUserId: String? = null, // null or empty means global announcement broadcast
    val targetUserEmail: String? = null,
    val actionUrl: String = "",
    val actionLabel: String = "",
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val senderAdminEmail: String = "admin@qrynova.ai"
) {
    val isGlobal: Boolean get() = targetUserId.isNullOrBlank()
}
