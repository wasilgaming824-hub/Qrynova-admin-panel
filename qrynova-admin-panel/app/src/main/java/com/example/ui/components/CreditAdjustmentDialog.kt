package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.TransactionType
import com.example.data.model.UserAccount
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditAdjustmentDialog(
    user: UserAccount,
    onDismiss: () -> Unit,
    onConfirm: (amount: Int, type: TransactionType, reason: String) -> Unit
) {
    var selectedType by remember { mutableStateOf(TransactionType.MANUAL_ADD) }
    var amountText by remember { mutableStateOf("50") }
    var reasonText by remember { mutableStateOf("") }

    val amountInt = amountText.toIntOrNull() ?: 0
    val newCalculatedBalance = when (selectedType) {
        TransactionType.MANUAL_ADD -> user.credits + amountInt
        TransactionType.MANUAL_DEDUCT -> (user.credits - amountInt).coerceAtLeast(0)
        TransactionType.MANUAL_SET -> amountInt.coerceAtLeast(0)
        else -> user.credits + amountInt
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Manual Credit Adjustment",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${user.name} (${user.email})",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Adjustment Mode Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AdjustmentModeTab(
                        label = "Add (+)",
                        isSelected = selectedType == TransactionType.MANUAL_ADD,
                        accentColor = SuccessEmerald,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedType = TransactionType.MANUAL_ADD }
                    )
                    AdjustmentModeTab(
                        label = "Deduct (-)",
                        isSelected = selectedType == TransactionType.MANUAL_DEDUCT,
                        accentColor = ErrorRose,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedType = TransactionType.MANUAL_DEDUCT }
                    )
                    AdjustmentModeTab(
                        label = "Set (=)",
                        isSelected = selectedType == TransactionType.MANUAL_SET,
                        accentColor = IndigoVibrant,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedType = TransactionType.MANUAL_SET }
                    )
                }

                // Balance Calculation Preview Card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Current", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${user.credits}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = CyanGlow,
                            modifier = Modifier.size(16.dp)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("New Balance", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "$newCalculatedBalance",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (selectedType) {
                                    TransactionType.MANUAL_ADD -> SuccessEmerald
                                    TransactionType.MANUAL_DEDUCT -> ErrorRose
                                    TransactionType.MANUAL_SET -> IndigoVibrant
                                    else -> CyanGlow
                                }
                            )
                        }
                    }
                }

                // Amount Input
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Credits Amount *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { if (it.all { char -> char.isDigit() }) amountText = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.MonetizationOn, contentDescription = null, tint = CyanGlow)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Reason for Audit Log
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Audit Reason / Note *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = reasonText,
                        onValueChange = { reasonText = it },
                        placeholder = { Text("e.g. VIP partner grant / refund for failed job") },
                        singleLine = false,
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (amountInt > 0 || selectedType == TransactionType.MANUAL_SET) {
                                onConfirm(amountInt, selectedType, reasonText)
                            }
                        },
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (selectedType) {
                                TransactionType.MANUAL_ADD -> SuccessEmerald
                                TransactionType.MANUAL_DEDUCT -> ErrorRose
                                TransactionType.MANUAL_SET -> IndigoVibrant
                                else -> CyanGlow
                            }
                        )
                    ) {
                        Text("Confirm Change", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdjustmentModeTab(
    label: String,
    isSelected: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) accentColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(accentColor)) else null,
        modifier = modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
