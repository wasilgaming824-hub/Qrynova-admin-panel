package com.example.data.model

enum class UserStatus {
    ACTIVE,
    SUSPENDED,
    DISABLED
}

enum class OnlinePresence {
    ONLINE,
    RECENTLY_ACTIVE,
    OFFLINE
}

data class UserAccount(
    val uid: String = "",
    val name: String = "",
    val nickname: String = "",
    val email: String = "",
    val profileImageUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis(),
    val presence: OnlinePresence = OnlinePresence.ONLINE,
    val credits: Int = 100,
    val totalCreditsGranted: Int = 100,
    val totalCreditsConsumed: Int = 0,
    val totalGenerations: Int = 0,
    val imageGenerations: Int = 0,
    val videoGenerations: Int = 0,
    val imageToVideoGenerations: Int = 0,
    val pendingGenerations: Int = 0,
    val completedGenerations: Int = 0,
    val failedGenerations: Int = 0,
    val status: UserStatus = UserStatus.ACTIVE,
    val referralCode: String = "",
    val referredBy: String = "",
    val referralCount: Int = 0,
    val dailyGenerationsUsed: Int = 0,
    val adminNotes: List<AdminNote> = emptyList()
)

data class AdminNote(
    val id: String = "",
    val adminId: String = "",
    val adminEmail: String = "",
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
