package com.example.data.model

enum class AdPlacementType(val label: String, val description: String) {
    BEFORE_IMAGE("Before Image Generation", "Requires watching ad before queueing image prompt"),
    BEFORE_VIDEO("Before Video Generation", "Requires watching ad before queueing text-to-video"),
    BEFORE_IMAGE_TO_VIDEO("Before Image-to-Video", "Requires watching ad before image animation"),
    BEFORE_BONUS_REWARD("Before Daily Bonus", "Requires watching ad to claim free bonus credits")
}

data class AdPlacement(
    val type: AdPlacementType = AdPlacementType.BEFORE_IMAGE,
    val enabled: Boolean = true,
    val watchDurationSeconds: Int = 10,
    val title: String = "Sponsor Spotlight",
    val description: String = "Watch this brief 10-second sponsor message to unlock high-speed GPU generation for free.",
    val adImageUrl: String = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800",
    val adVideoUrl: String = "",
    val targetUrl: String = "https://qrynova.ai/partners",
    val ctaText: String = "Visit Sponsor Website"
)

data class AdSettings(
    val globalAdsEnabled: Boolean = true,
    val defaultWatchDurationSeconds: Int = 10,
    val placements: Map<String, AdPlacement> = mapOf(
        AdPlacementType.BEFORE_IMAGE.name to AdPlacement(
            type = AdPlacementType.BEFORE_IMAGE,
            enabled = true,
            watchDurationSeconds = 10,
            title = "Neural Cloud GPU Compute",
            description = "Explore ultra-fast rendering on QRYNOVA Cloud GPUs. Instant deployment & zero setup.",
            adImageUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800",
            targetUrl = "https://qrynova.ai",
            ctaText = "Explore Solutions"
        ),
        AdPlacementType.BEFORE_VIDEO.name to AdPlacement(
            type = AdPlacementType.BEFORE_VIDEO,
            enabled = true,
            watchDurationSeconds = 10,
            title = "Generative Cinema AI Suite",
            description = "Transform storyboards into photorealistic 4K scenes with QRYNOVA Studio.",
            adImageUrl = "https://images.unsplash.com/photo-1620641788421-7a1c342ea42e?w=800",
            targetUrl = "https://qrynova.ai/video",
            ctaText = "Learn More"
        ),
        AdPlacementType.BEFORE_IMAGE_TO_VIDEO.name to AdPlacement(
            type = AdPlacementType.BEFORE_IMAGE_TO_VIDEO,
            enabled = true,
            watchDurationSeconds = 10,
            title = "Motion Synthesis Engine",
            description = "Breathe fluid cinematic life into still concept art and digital photos.",
            adImageUrl = "https://images.unsplash.com/photo-1634017839464-5c339ebe3cb4?w=800",
            targetUrl = "https://qrynova.ai/motion",
            ctaText = "Watch Demo"
        ),
        AdPlacementType.BEFORE_BONUS_REWARD.name to AdPlacement(
            type = AdPlacementType.BEFORE_BONUS_REWARD,
            enabled = true,
            watchDurationSeconds = 10,
            title = "Daily Power Credits",
            description = "Claim your daily bonus compute credits by supporting our partners.",
            adImageUrl = "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=800",
            targetUrl = "https://qrynova.ai/rewards",
            ctaText = "Claim Reward"
        )
    ),
    val customNetworkSnippet: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
)
