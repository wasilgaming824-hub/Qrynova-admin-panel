package com.example.data.model

/**
 * Configuration for the QRYNOVA Credit-Only Referral Program.
 * Every successful referral yields EXACTLY 20 QRYNOVA Credits.
 */
data class ReferralSettings(
    val enabled: Boolean = true,
    val rewardCredits: Int = 20,
    val minimumReferrals: Int = 1,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Record of a verified referral event awarding +20 Credits to the inviter.
 */
data class ReferralRecord(
    val id: String = "",
    val referrerUid: String = "",
    val referrerName: String = "",
    val referrerEmail: String = "",
    val referredUid: String = "",
    val referredName: String = "",
    val referredEmail: String = "",
    val referralCode: String = "",
    val creditsAwarded: Int = 20,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Daily limits and credit costs for AI generation pipelines.
 */
data class AILimitsSettings(
    val imageGenEnabled: Boolean = true,
    val videoGenEnabled: Boolean = true,
    val imageToVideoEnabled: Boolean = true,
    val dailyImageLimit: Int = 20,
    val dailyVideoLimit: Int = 5,
    val dailyImageToVideoLimit: Int = 5,
    val requiredAdDurationSeconds: Int = 10,
    val creditCostPerImage: Int = 10,
    val creditCostPerVideo: Int = 50,
    val creditCostPerImageToVideo: Int = 60,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Official support, community, and social channels.
 */
data class SupportSettings(
    val whatsappNumber: String = "+8801700000000",
    val whatsappGroupLink: String = "https://chat.whatsapp.com/QrynovaOfficial",
    val telegramUsername: String = "qrynova_support",
    val telegramGroupLink: String = "https://t.me/qrynova_community",
    val supportEmail: String = "support@qrynova.ai",
    val websiteUrl: String = "https://qrynova.ai",
    val updatedAt: Long = System.currentTimeMillis()
) {
    val whatsapp: String
        get() = if (whatsappGroupLink.isNotBlank()) whatsappGroupLink else "https://wa.me/${whatsappNumber.replace("+", "").replace(" ", "")}"

    val telegram: String
        get() = if (telegramGroupLink.isNotBlank()) telegramGroupLink else "https://t.me/$telegramUsername"
}
