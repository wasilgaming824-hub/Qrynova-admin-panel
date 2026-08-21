package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CreditSettings
import com.example.data.model.CreditTransaction
import com.example.data.model.TransactionType
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CreditSettingsScreen(
    creditSettings: CreditSettings,
    transactions: List<CreditTransaction>,
    onSaveSettings: (CreditSettings) -> Unit
) {
    var startingCreditsText by remember(creditSettings) { mutableStateOf(creditSettings.newUserStartingCredits.toString()) }
    var imageCostText by remember(creditSettings) { mutableStateOf(creditSettings.imageGenerationCost.toString()) }
    var videoCostText by remember(creditSettings) { mutableStateOf(creditSettings.videoGenerationCost.toString()) }
    var i2vCostText by remember(creditSettings) { mutableStateOf(creditSettings.imageToVideoCost.toString()) }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    val startingCredits = startingCreditsText.toIntOrNull() ?: creditSettings.newUserStartingCredits
    val imageCost = imageCostText.toIntOrNull() ?: creditSettings.imageGenerationCost
    val videoCost = videoCostText.toIntOrNull() ?: creditSettings.videoGenerationCost
    val i2vCost = i2vCostText.toIntOrNull() ?: creditSettings.imageToVideoCost

    val hasChanges = startingCredits != creditSettings.newUserStartingCredits ||
            imageCost != creditSettings.imageGenerationCost ||
            videoCost != creditSettings.videoGenerationCost ||
            i2vCost != creditSettings.imageToVideoCost

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
    ) {
        item {
            SectionHeader(
                title = "Credit Economy Rules",
                description = "Configure starting grant & per-generation costs across the platform"
            )
        }

        // New User Starting Credits Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CardGiftcard, contentDescription = null, tint = SuccessEmerald)
                        Column {
                            Text("New User Starting Credits", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Granted automatically upon initial registration", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    OutlinedTextField(
                        value = startingCreditsText,
                        onValueChange = { if (it.all { c -> c.isDigit() }) startingCreditsText = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.MonetizationOn, contentDescription = null, tint = CyanGlow)
                        },
                        trailingIcon = { Text("credits", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 12.dp)) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Per-Generation Cost Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PriceChange, contentDescription = null, tint = CyanGlow)
                        Column {
                            Text("Generation Cost Rules", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Deducted from user credit balance per generation", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Image Cost
                    CostRowField(
                        label = "Image Generation Cost",
                        sublabel = "Standard & High-Res prompt synthesis",
                        value = imageCostText,
                        icon = Icons.Default.Image,
                        onValueChange = { if (it.all { c -> c.isDigit() }) imageCostText = it }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    // Video Cost
                    CostRowField(
                        label = "Text-to-Video Generation Cost",
                        sublabel = "Cinematic video generation",
                        value = videoCostText,
                        icon = Icons.Default.Videocam,
                        onValueChange = { if (it.all { c -> c.isDigit() }) videoCostText = it }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    // Image-to-Video Cost
                    CostRowField(
                        label = "Image-to-Video Cost",
                        sublabel = "Image animation synthesis",
                        value = i2vCostText,
                        icon = Icons.Default.Animation,
                        onValueChange = { if (it.all { c -> c.isDigit() }) i2vCostText = it }
                    )
                }
            }
        }

        // Save Button Bar
        item {
            Button(
                onClick = {
                    onSaveSettings(
                        creditSettings.copy(
                            newUserStartingCredits = startingCredits,
                            imageGenerationCost = imageCost,
                            videoGenerationCost = videoCost,
                            imageToVideoCost = i2vCost
                        )
                    )
                },
                enabled = hasChanges,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanGlowDark,
                    contentColor = Color.White
                )
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Credit Pricing to Firestore", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        // Recent Credit Transactions History
        item {
            SectionHeader(
                title = "Recent Credit Transactions",
                description = "Manual adjustments & generation consumption history"
            )
        }

        if (transactions.isEmpty()) {
            item {
                Text("No credit transactions recorded yet.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(transactions.take(8)) { tx ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val (color, label) = when (tx.type) {
                                    TransactionType.MANUAL_ADD -> Pair(SuccessEmerald, "+${tx.amount} Added")
                                    TransactionType.MANUAL_DEDUCT -> Pair(ErrorRose, "-${tx.amount} Deducted")
                                    TransactionType.MANUAL_SET -> Pair(IndigoVibrant, "Set to ${tx.newBalance}")
                                    TransactionType.GENERATION_SPEND -> Pair(WarningAmber, "-${tx.amount} Spent")
                                    else -> Pair(CyanGlow, "${tx.amount} Credits")
                                }
                                Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
                                Text("• ${tx.userEmail}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = "${tx.previousBalance} → ${tx.newBalance}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Text(
                            text = tx.reason,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "By: ${tx.adminEmail}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = dateFormat.format(Date(tx.timestamp)),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CostRowField(
    label: String,
    sublabel: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = IndigoVibrant, modifier = Modifier.size(20.dp))
            Column {
                Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(sublabel, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.width(90.dp)
        )
    }
}
