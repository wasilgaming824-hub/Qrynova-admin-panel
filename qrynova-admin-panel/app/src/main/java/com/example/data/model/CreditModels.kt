package com.example.data.model

enum class TransactionType {
    MANUAL_ADD,
    MANUAL_DEDUCT,
    MANUAL_SET,
    NEW_USER_BONUS,
    GENERATION_SPEND,
    REFUND,
    REFERRAL_BONUS
}

data class CreditSettings(
    val newUserStartingCredits: Int = 100,
    val imageGenerationCost: Int = 2,
    val videoGenerationCost: Int = 2,
    val imageToVideoCost: Int = 2,
    val lastUpdated: Long = System.currentTimeMillis(),
    val updatedByAdmin: String = "admin@qrynova.ai"
)

data class CreditTransaction(
    val transactionId: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val amount: Int = 0,
    val previousBalance: Int = 0,
    val newBalance: Int = 0,
    val type: TransactionType = TransactionType.MANUAL_ADD,
    val reason: String = "",
    val adminId: String = "",
    val adminEmail: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
