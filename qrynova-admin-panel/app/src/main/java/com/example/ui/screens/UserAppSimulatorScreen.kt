package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.*
import com.example.ui.components.RequestStatusBadge
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserAppSimulatorScreen(
    activeUser: UserAccount,
    creditSettings: CreditSettings,
    adSettings: AdSettings,
    generationSettings: GenerationSettings,
    systemSettings: AppSystemSettings,
    referralSettings: ReferralSettings,
    aiLimitsSettings: AILimitsSettings,
    supportSettings: SupportSettings,
    userRequests: List<GenerationRequest>,
    notifications: List<AppNotification>,
    onSubmitGeneration: (prompt: String, type: RequestType, resolution: String, aspect: String) -> Unit,
    onClaimReferral: (code: String) -> Unit,
    onBackToAdmin: () -> Unit
) {
    val context = LocalContext.current
    var activeSimulatorTab by remember { mutableIntStateOf(0) }
    val simulatorTabs = listOf("AI Studio", "Credits & Stats", "Referral", "Inbox")

    // Generation State
    var selectedType by remember { mutableStateOf(RequestType.IMAGE) }
    var promptText by remember { mutableStateOf("Hyper-realistic cyberpunk street with neon reflections in rain, 8k cinematic masterpiece") }
    var selectedResolution by remember { mutableStateOf("1080x1080") }
    var selectedAspect by remember { mutableStateOf("1:1") }

    // Ad Interstitial Modal State
    var showAdModal by remember { mutableStateOf(false) }
    var adCountdown by remember { mutableIntStateOf(10) }
    var adFinished by remember { mutableStateOf(false) }

    // Maintenance checks
    val isSystemMaint = systemSettings.maintenanceMode
    val isPipelineMaint = when (selectedType) {
        RequestType.IMAGE -> generationSettings.image.maintenanceMode
        RequestType.TEXT_TO_VIDEO -> generationSettings.video.maintenanceMode
        RequestType.IMAGE_TO_VIDEO -> generationSettings.imageToVideo.maintenanceMode
    }

    val currentCost = when (selectedType) {
        RequestType.IMAGE -> creditSettings.imageGenerationCost
        RequestType.TEXT_TO_VIDEO -> creditSettings.videoGenerationCost
        RequestType.IMAGE_TO_VIDEO -> creditSettings.imageToVideoCost
    }

    val hasSufficientCredits = activeUser.credits >= currentCost

    // Ad Countdown Timer effect
    LaunchedEffect(showAdModal) {
        if (showAdModal) {
            val placement = adSettings.placements[selectedType.name]
            val duration = aiLimitsSettings.requiredAdDurationSeconds.coerceAtLeast(placement?.watchDurationSeconds ?: 10)
            adCountdown = duration
            adFinished = false
            while (adCountdown > 0) {
                delay(1000L)
                adCountdown -= 1
            }
            adFinished = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // App Simulator Header Bar
        Surface(
            color = Color(0xFF1E293B),
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBackToAdmin) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = systemSettings.appName.ifEmpty { "Qrynova AI" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = Color(0xFF3B82F6).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "LIVE USER SIMULATOR",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF60A5FA)
                                    )
                                }
                            }
                            Text(
                                text = "Active User: ${activeUser.name} (${activeUser.email})",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    // User Balance Pill
                    Surface(
                        color = Color(0xFF334155),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${activeUser.credits} Credits",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Navigation Tabs inside User App
                PrimaryTabRow(
                    selectedTabIndex = activeSimulatorTab,
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                ) {
                    simulatorTabs.forEachIndexed { idx, label ->
                        Tab(
                            selected = activeSimulatorTab == idx,
                            onClick = { activeSimulatorTab = idx },
                            text = {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (activeSimulatorTab == idx) FontWeight.Bold else FontWeight.Normal,
                                    color = if (activeSimulatorTab == idx) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                                )
                            }
                        )
                    }
                }
            }
        }

        // Global Maintenance Warning if triggered from Admin
        if (isSystemMaint) {
            Surface(
                color = Color(0xFFEF4444).copy(alpha = 0.2f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF87171), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = systemSettings.maintenanceMessage.ifEmpty { "System is undergoing scheduled maintenance." },
                        color = Color(0xFFFCA5A5),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Tab Content
        Box(modifier = Modifier.fillMaxSize()) {
            when (activeSimulatorTab) {
                0 -> SimulatorAIStudioTab(
                    activeUser = activeUser,
                    selectedType = selectedType,
                    onSelectType = { selectedType = it },
                    promptText = promptText,
                    onPromptChange = { promptText = it },
                    selectedResolution = selectedResolution,
                    onResolutionChange = { selectedResolution = it },
                    selectedAspect = selectedAspect,
                    onAspectChange = { selectedAspect = it },
                    currentCost = currentCost,
                    hasSufficientCredits = hasSufficientCredits,
                    isSystemMaint = isSystemMaint,
                    isPipelineMaint = isPipelineMaint,
                    userRequests = userRequests,
                    onTriggerAd = { showAdModal = true },
                    onOpenLink = { url ->
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Ignored
                        }
                    }
                )

                1 -> SimulatorCreditsTab(
                    activeUser = activeUser,
                    creditSettings = creditSettings
                )

                2 -> SimulatorReferralTab(
                    activeUser = activeUser,
                    referralSettings = referralSettings,
                    onClaimReferral = onClaimReferral
                )

                3 -> SimulatorInboxTab(
                    notifications = notifications,
                    supportSettings = supportSettings,
                    onOpenLink = { url ->
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Ignored
                        }
                    }
                )
            }
        }
    }

    // 10-Second Ad Interstitial Overlay Modal
    if (showAdModal) {
        Dialog(
            onDismissRequest = {
                if (adFinished) showAdModal = false
            },
            properties = DialogProperties(dismissOnBackPress = adFinished, dismissOnClickOutside = false)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "SPONSORED ADVERTISEMENT",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFBBF24)
                            )
                        }

                        Text(
                            text = if (adFinished) "Reward Ready ✓" else "${adCountdown}s",
                            fontWeight = FontWeight.Bold,
                            color = if (adFinished) Color(0xFF10B981) else Color(0xFF38BDF8),
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Video Ad Simulation Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.SmartDisplay,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Qrynova AI Super Resolution",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "High Performance Neural Rendering Engine",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LinearProgressIndicator(
                        progress = { if (adFinished) 1f else (10 - adCountdown) / 10f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF10B981),
                        trackColor = Color(0xFF334155),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            showAdModal = false
                            onSubmitGeneration(promptText, selectedType, selectedResolution, selectedAspect)
                        },
                        enabled = adFinished,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            disabledContainerColor = Color(0xFF334155)
                        )
                    ) {
                        if (adFinished) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ad Watched — Submit Job to Firestore", fontWeight = FontWeight.Bold)
                        } else {
                            Text("Please watch ad ($adCountdown s remaining)...", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SimulatorAIStudioTab(
    activeUser: UserAccount,
    selectedType: RequestType,
    onSelectType: (RequestType) -> Unit,
    promptText: String,
    onPromptChange: (String) -> Unit,
    selectedResolution: String,
    onResolutionChange: (String) -> Unit,
    selectedAspect: String,
    onAspectChange: (String) -> Unit,
    currentCost: Int,
    hasSufficientCredits: Boolean,
    isSystemMaint: Boolean,
    isPipelineMaint: Boolean,
    userRequests: List<GenerationRequest>,
    onTriggerAd: () -> Unit,
    onOpenLink: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Generation Type Selector
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Select Generation Pipeline",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RequestType.entries.forEach { type ->
                        val isSelected = selectedType == type
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFF38BDF8).copy(alpha = 0.2f) else Color(0xFF334155),
                            border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF38BDF8))) else null,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSelectType(type) }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = when (type) {
                                        RequestType.IMAGE -> "Text to Image"
                                        RequestType.TEXT_TO_VIDEO -> "Text to Video"
                                        RequestType.IMAGE_TO_VIDEO -> "Image to Video"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color(0xFF38BDF8) else Color.White,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        // Prompt Input Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Creative Prompt", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = promptText,
                    onValueChange = onPromptChange,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF475569)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = selectedResolution,
                        onValueChange = onResolutionChange,
                        label = { Text("Resolution", color = Color(0xFF94A3B8)) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF475569)
                        )
                    )
                    OutlinedTextField(
                        value = selectedAspect,
                        onValueChange = onAspectChange,
                        label = { Text("Aspect Ratio", color = Color(0xFF94A3B8)) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF475569)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onTriggerAd,
                    enabled = !isSystemMaint && !isPipelineMaint && hasSufficientCredits,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (!hasSufficientCredits) "Insufficient Credits ($currentCost required)"
                        else "Watch 10s Ad & Generate ($currentCost Credits)",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Live User Generation Queue
        Text(
            text = "User Generation Queue & History (${userRequests.size})",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )

        if (userRequests.isEmpty()) {
            Surface(
                color = Color(0xFF1E293B),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No generation requests submitted yet.", color = Color(0xFF94A3B8), fontSize = 12.sp)
                }
            }
        } else {
            userRequests.forEach { req ->
                UserRequestSimulatorCard(request = req, onOpenLink = onOpenLink)
            }
        }
    }
}

@Composable
fun SimulatorCreditsTab(
    activeUser: UserAccount,
    creditSettings: CreditSettings
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Balance Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("Available QRYNOVA Credits", color = Color(0xFF94A3B8), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${activeUser.credits}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Surface(
                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Active & Ready",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFF334155))
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Total Granted", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Text("+${activeUser.totalCreditsGranted}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Total Consumed", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Text("${activeUser.totalCreditsConsumed}", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        // Credit Pricing Rules Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("AI Generation Costs", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Text to Image Cost", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Text("${creditSettings.imageGenerationCost} Credits", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Text to Video Cost", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Text("${creditSettings.videoGenerationCost} Credits", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Image to Video Cost", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Text("${creditSettings.imageToVideoCost} Credits", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // Generation Activity Breakdown
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Creation Stats", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Creations", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Text("${activeUser.totalGenerations}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Images Generated", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Text("${activeUser.imageGenerations}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Videos Generated", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Text("${activeUser.videoGenerations}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun SimulatorReferralTab(
    activeUser: UserAccount,
    referralSettings: ReferralSettings,
    onClaimReferral: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    var inputReferralCode by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("Your Personal Referral Code", color = Color(0xFF94A3B8), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = activeUser.referralCode.ifEmpty { "REF-${activeUser.uid.take(6).uppercase()}" },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF38BDF8),
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Earn +${referralSettings.rewardCredits} QRYNOVA Credits for every friend who joins using your code.",
                    fontSize = 12.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Total Referrals: ${activeUser.referralCount} (${activeUser.referralCount * referralSettings.rewardCredits} Credits Earned)",
                    fontSize = 11.sp,
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("Enter Inviter's Code", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (activeUser.referredBy.isNotBlank()) "Referral already claimed for this account"
                    else "Enter an invite code to reward your referrer with +${referralSettings.rewardCredits} Credits.",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = inputReferralCode,
                    onValueChange = { inputReferralCode = it.uppercase() },
                    placeholder = { Text("e.g. REF-ABC123", color = Color(0xFF64748B)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = activeUser.referredBy.isBlank(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF475569)
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onClaimReferral(inputReferralCode) },
                    enabled = inputReferralCode.isNotBlank() && activeUser.referredBy.isBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                ) {
                    Text("Apply Referral Code")
                }
            }
        }
    }
}

@Composable
fun SimulatorInboxTab(
    notifications: List<AppNotification>,
    supportSettings: SupportSettings,
    onOpenLink: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Support buttons
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Help & Community Support", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (supportSettings.whatsappNumber.isNotBlank()) {
                        Button(
                            onClick = { onOpenLink("https://wa.me/${supportSettings.whatsappNumber.replace("+", "")}") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                        ) {
                            Text("WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (supportSettings.telegramUsername.isNotBlank()) {
                        Button(
                            onClick = { onOpenLink("https://t.me/${supportSettings.telegramUsername.replace("@", "")}") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF229ED9))
                        ) {
                            Text("Telegram", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Text("In-App Announcements (${notifications.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)

        if (notifications.isEmpty()) {
            Text("No messages in inbox.", color = Color(0xFF94A3B8), fontSize = 12.sp)
        } else {
            notifications.forEach { notif ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(notif.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(notif.message, color = Color(0xFFCBD5E1), fontSize = 12.sp)
                        if (notif.actionUrl.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { onOpenLink(notif.actionUrl) }) {
                                Text(notif.actionLabel.ifEmpty { "Learn More" }, color = Color(0xFF38BDF8))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserRequestSimulatorCard(
    request: GenerationRequest,
    onOpenLink: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${request.requestType.name} • ${request.requestId.takeLast(6)}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8)
                )
                RequestStatusBadge(status = request.status)
            }

            Text(
                text = request.prompt,
                fontSize = 12.sp,
                color = Color.White,
                maxLines = 2
            )

            if (request.status == RequestStatus.COMPLETED && request.googleDriveUrl.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                            Text("Ready for Download via Google Drive", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        }

                        if (request.adminMessage.isNotBlank()) {
                            Text(
                                text = "Admin: \"${request.adminMessage}\"",
                                fontSize = 11.sp,
                                color = Color.White
                            )
                        }

                        Button(
                            onClick = { onOpenLink(request.googleDriveUrl) },
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open & Download Result File", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (request.status == RequestStatus.PENDING || request.status == RequestStatus.PROCESSING) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF59E0B).copy(alpha = 0.10f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFFF59E0B)
                        )
                        Text(
                            text = if (request.status == RequestStatus.PENDING) "In Queue — Awaiting Admin Generation" else "GPU Rendering in Progress...",
                            fontSize = 11.sp,
                            color = Color(0xFFF59E0B)
                        )
                    }
                }
            }
        }
    }
}
