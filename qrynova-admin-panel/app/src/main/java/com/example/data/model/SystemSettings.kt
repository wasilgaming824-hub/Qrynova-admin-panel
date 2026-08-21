package com.example.data.model

data class AppSystemSettings(
    val appName: String = "QRYNOVA",
    val appTagline: String = "Next-Gen Generative AI Engine",
    val appLogoUrl: String = "https://qrynova.ai/logo.png",
    val appDescription: String = "Generate breathtaking images, cinematics, and animated videos with enterprise-grade precision.",
    val maintenanceMode: Boolean = false,
    val maintenanceMessage: String = "QRYNOVA is currently undergoing scheduled platform maintenance. Services will resume shortly.",
    val registrationEnabled: Boolean = true,
    val loginEnabled: Boolean = true,
    val publicAdminsContact: String = "support@qrynova.ai",
    val primaryTermsUrl: String = "https://qrynova.ai/terms",
    val primaryPrivacyUrl: String = "https://qrynova.ai/privacy",
    val forceUpdateRequired: Boolean = false,
    val forceUpdateUrl: String = "https://play.google.com/store/apps/details?id=com.qrynova.app",
    val lastUpdated: Long = System.currentTimeMillis()
)
