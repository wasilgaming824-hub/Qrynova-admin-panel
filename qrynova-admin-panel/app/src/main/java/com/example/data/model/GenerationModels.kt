package com.example.data.model

enum class RequestType {
    IMAGE,
    TEXT_TO_VIDEO,
    IMAGE_TO_VIDEO
}

enum class RequestStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class GenerationRequest(
    val requestId: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val userName: String = "",
    val requestType: RequestType = RequestType.IMAGE,
    val prompt: String = "",
    val resolution: String = "1080x1080",
    val aspectRatio: String = "1:1",
    val pixelSize: String = "1080 x 1080",
    val durationSeconds: Int = 5,
    val sourceImageUrl: String = "",
    val creditCost: Int = 2,
    val adCompleted: Boolean = true,
    val status: RequestStatus = RequestStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val googleDriveUrl: String = "",
    val adminMessage: String = "",
    val adminNotes: String = ""
)

data class ImageGenerationConfig(
    val enabled: Boolean = true,
    val creditCost: Int = 2,
    val maxPromptLength: Int = 1000,
    val availableResolutions: List<String> = listOf("1080x1080", "1920x1080", "1080x1920", "512x512", "768x768"),
    val availableAspectRatios: List<String> = listOf("1:1", "16:9", "9:16", "4:3", "3:4"),
    val maxOutputSizeMb: Int = 25,
    val maintenanceMode: Boolean = false,
    val maintenanceMessage: String = "Image generation engine is temporarily undergoing GPU upgrade. Please check back shortly."
)

data class VideoGenerationConfig(
    val enabled: Boolean = true,
    val creditCost: Int = 2,
    val minPromptLength: Int = 10,
    val maxPromptLength: Int = 1200,
    val availableResolutions: List<String> = listOf("1080p (1920x1080)", "720p (1280x720)", "Square (1080x1080)", "Portrait (1080x1920)"),
    val availableAspectRatios: List<String> = listOf("16:9", "9:16", "1:1"),
    val availableDurations: List<Int> = listOf(5, 10, 15),
    val maxDurationSeconds: Int = 15,
    val maintenanceMode: Boolean = false,
    val maintenanceMessage: String = "Video rendering cluster is running at peak capacity. Text-to-Video will resume soon."
)

data class ImageToVideoConfig(
    val enabled: Boolean = true,
    val creditCost: Int = 2,
    val availableResolutions: List<String> = listOf("1080p (1920x1080)", "720p (1280x720)", "1080x1080"),
    val availableAspectRatios: List<String> = listOf("16:9", "9:16", "1:1"),
    val durationSeconds: Int = 5,
    val promptRequired: Boolean = true,
    val maintenanceMode: Boolean = false,
    val maintenanceMessage: String = "Image-to-Video generation pipeline is currently in maintenance."
)

data class GenerationSettings(
    val image: ImageGenerationConfig = ImageGenerationConfig(),
    val video: VideoGenerationConfig = VideoGenerationConfig(),
    val imageToVideo: ImageToVideoConfig = ImageToVideoConfig(),
    val lastUpdated: Long = System.currentTimeMillis()
)
