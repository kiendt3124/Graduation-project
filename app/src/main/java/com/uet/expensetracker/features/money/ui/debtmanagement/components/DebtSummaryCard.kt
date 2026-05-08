package com.uet.expensetracker.features.money.ui.debtmanagement.components//package com.uet.expensetracker.features.money.ui.debtmanagement.components
//
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Person
//import androidx.compose.material3.*
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.tooling.preview.Preview
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import com.uet.expensetracker.features.money.domain.model.DebtSummary
//import com.uet.expensetracker.features.money.domain.model.DebtType
//import com.uet.expensetracker.features.money.domain.model.PaymentRecord
//import com.uet.expensetracker.ui.theme.*
//import com.uet.expensetracker.utils.formatAmount
//import java.text.SimpleDateFormat
//import java.util.*
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun DebtSummaryCard(
//    debtSummary: DebtSummary,
//    onCardClick: () -> Unit = {},
//    onPaymentClick: () -> Unit = {},
////    onHistoryClick: () -> Unit = {},
//    modifier: Modifier = Modifier
//) {
//    Card(
//        modifier = modifier.fillMaxWidth(),
//        onClick = onCardClick,
//        shape = RoundedCornerShape(12.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = AppColor.Dark.PrimaryColor.cardColor
//        ),
//        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
//    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(16.dp),
//            verticalArrangement = Arrangement.spacedBy(12.dp)
//        ) {
//            // Header with person info and status
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                // Person info
//                Row(
//                    verticalAlignment = Alignment.CenterVertically,
//                    horizontalArrangement = Arrangement.spacedBy(12.dp)
//                ) {
//                    // Avatar
//                    PersonAvatar(
//                        name = debtSummary.personName,
//                        modifier = Modifier.size(40.dp)
//                    )
//
//                    Column {
//                        Text(
//                            text = debtSummary.personName,
//                            color = MaterialTheme.colorScheme.onSurface,
//                            fontSize = 16.sp,
//                            fontWeight = FontWeight.SemiBold,
//                            maxLines = 1,
//                            overflow = TextOverflow.Ellipsis
//                        )
//
//                        Text(
//                            text = when (debtSummary.debtType) {
//                                DebtType.PAYABLE -> "Bạn nợ"
//                                DebtType.RECEIVABLE -> "Nợ bạn"
//                            },
//                            color = TextSecondary,
//                            fontSize = 12.sp
//                        )
//                    }
//                }
//
//                // Status badge
//                StatusBadge(
//                    isFullyPaid = debtSummary.isFullyPaid,
//                    debtType = debtSummary.debtType
//                )
//            }
//
//            // Amount information
//            Column(
//                verticalArrangement = Arrangement.spacedBy(8.dp)
//            ) {
//                // Original and remaining amounts
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween
//                ) {
//                    AmountInfo(
//                        label = "Gốc",
//                        amount = debtSummary.originalAmount,
//                        currencySymbol = getCurrencySymbol(debtSummary),
//                        textColor = MaterialTheme.colorScheme.onSurface
//                    )
//
//                    AmountInfo(
//                        label = when (debtSummary.debtType) {
//                            DebtType.PAYABLE -> "Còn nợ"
//                            DebtType.RECEIVABLE -> "Còn thu"
//                        },
//                        amount = debtSummary.remainingAmount,
//                        currencySymbol = getCurrencySymbol(debtSummary),
//                        textColor = when (debtSummary.debtType) {
//                            DebtType.PAYABLE -> MaterialTheme.colorScheme.error
//                            DebtType.RECEIVABLE -> MaterialTheme.colorScheme.primary
//                        }
//                    )
//                }
//
//                // Progress bar
//                if (debtSummary.originalAmount > 0) {
//                    val progressPercentage = (debtSummary.paidAmount / debtSummary.originalAmount).toFloat()
//
//                    Column(
//                        verticalArrangement = Arrangement.spacedBy(4.dp)
//                    ) {
//                        Row(
//                            modifier = Modifier.fillMaxWidth(),
//                            horizontalArrangement = Arrangement.SpaceBetween
//                        ) {
//                            Text(
//                                text = when (debtSummary.debtType) {
//                                    DebtType.PAYABLE -> "Đã trả"
//                                    DebtType.RECEIVABLE -> "Đã thu"
//                                },
//                                color = TextSecondary,
//                                fontSize = 11.sp
//                            )
//                            Text(
//                                text = "${(progressPercentage * 100).toInt()}%",
//                                color = TextSecondary,
//                                fontSize = 11.sp,
//                                fontWeight = FontWeight.Medium
//                            )
//                        }
//
//                        LinearProgressIndicator(
//                            progress = progressPercentage,
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .height(4.dp),
//                            color = when (debtSummary.debtType) {
//                                DebtType.PAYABLE -> MaterialTheme.colorScheme.primary
//                                DebtType.RECEIVABLE -> MaterialTheme.colorScheme.secondary
//                            },
//                            trackColor = MaterialTheme.colorScheme.surfaceVariant
//                        )
//                    }
//                }
//            }
//
//            // Last payment info
//            debtSummary.lastPaymentDate?.let { lastPaymentDate ->
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Text(
//                        text = "Thanh toán cuối: ${formatDate(lastPaymentDate)}",
//                        color = TextSecondary,
//                        fontSize = 11.sp
//                    )
//
//                    if (debtSummary.paymentHistory.isNotEmpty()) {
//                        Text(
//                            text = "${debtSummary.paymentHistory.size} lần",
//                            color = MaterialTheme.colorScheme.primary,
//                            fontSize = 11.sp,
//                            fontWeight = FontWeight.Medium
//                        )
//                    }
//                }
//            }
//
//            // Action buttons
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.spacedBy(8.dp)
//            ) {
//                // Payment button
//                if (!debtSummary.isFullyPaid) {
//                    OutlinedButton(
//                        onClick = onPaymentClick,
//                        modifier = Modifier.weight(1f),
//                        colors = ButtonDefaults.outlinedButtonColors(
//                            contentColor = when (debtSummary.debtType) {
//                                DebtType.PAYABLE -> MaterialTheme.colorScheme.primary
//                                DebtType.RECEIVABLE -> MaterialTheme.colorScheme.secondary
//                            }
//                        )
//                    ) {
//                        Icon(
//                            imageVector = Icons.Default.Payment,
//                            contentDescription = null,
//                            modifier = Modifier.size(16.dp)
//                        )
//                        Spacer(modifier = Modifier.width(4.dp))
//                        Text(
//                            text = when (debtSummary.debtType) {
//                                DebtType.PAYABLE -> "Trả nợ"
//                                DebtType.RECEIVABLE -> "Thu nợ"
//                            },
//                            fontSize = 12.sp
//                        )
//                    }
//                }
//
//                // History button
//                OutlinedButton(
//                    onClick = onHistoryClick,
//                    modifier = Modifier.weight(1f),
//                    colors = ButtonDefaults.outlinedButtonColors(
//                        contentColor = MaterialTheme.colorScheme.outline
//                    )
//                ) {
//                    Icon(
//                        imageVector = Icons.Default.History,
//                        contentDescription = null,
//                        modifier = Modifier.size(16.dp)
//                    )
//                    Spacer(modifier = Modifier.width(4.dp))
//                    Text(
//                        text = "Lịch sử",
//                        fontSize = 12.sp
//                    )
//                }
//            }
//        }
//    }
//}
//
//@Composable
//private fun PersonAvatar(
//    name: String,
//    modifier: Modifier = Modifier
//) {
//    val initial = name.take(1).uppercase()
//
//    Box(
//        modifier = modifier
//            .clip(CircleShape)
//            .background(MaterialTheme.colorScheme.primaryContainer),
//        contentAlignment = Alignment.Center
//    ) {
//        Text(
//            text = initial,
//            color = MaterialTheme.colorScheme.onPrimaryContainer,
//            fontSize = 16.sp,
//            fontWeight = FontWeight.Bold
//        )
//    }
//}
//
//@Composable
//private fun StatusBadge(
//    isFullyPaid: Boolean,
//    debtType: DebtType
//) {
//    Surface(
//        shape = RoundedCornerShape(12.dp),
//        color = if (isFullyPaid) {
//            MaterialTheme.colorScheme.primaryContainer
//        } else {
//            when (debtType) {
//                DebtType.PAYABLE -> MaterialTheme.colorScheme.errorContainer
//                DebtType.RECEIVABLE -> MaterialTheme.colorScheme.secondaryContainer
//            }
//        }
//    ) {
//        Text(
//            text = if (isFullyPaid) "Hoàn thành" else "Chưa hoàn thành",
//            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
//            color = if (isFullyPaid) {
//                MaterialTheme.colorScheme.onPrimaryContainer
//            } else {
//                when (debtType) {
//                    DebtType.PAYABLE -> MaterialTheme.colorScheme.onErrorContainer
//                    DebtType.RECEIVABLE -> MaterialTheme.colorScheme.onSecondaryContainer
//                }
//            },
//            fontSize = 10.sp,
//            fontWeight = FontWeight.Medium
//        )
//    }
//}
//
//@Composable
//private fun AmountInfo(
//    label: String,
//    amount: Double,
//    currencySymbol: String,
//    textColor: androidx.compose.ui.graphics.Color
//) {
//    Column(
//        horizontalAlignment = Alignment.Start
//    ) {
//        Text(
//            text = label,
//            color = TextSecondary,
//            fontSize = 11.sp
//        )
//        Spacer(modifier = Modifier.height(2.dp))
//        Text(
//            text = "${formatAmount(amount)} $currencySymbol",
//            color = textColor,
//            fontSize = 14.sp,
//            fontWeight = FontWeight.Bold
//        )
//    }
//}
//
//private fun getCurrencySymbol(debtSummary: DebtSummary): String {
//    return if (debtSummary.relatedTransactions.isNotEmpty()) {
//        debtSummary.relatedTransactions.first().wallet.currency.symbol
//    } else {
//        "đ"
//    }
//}
//
//private fun formatDate(date: Date): String {
//    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
//    return formatter.format(date)
//}
//
//@Preview(showBackground = true)
//@Composable
//fun DebtSummaryCardPreview() {
//    MaterialTheme {
//        Column(
//            modifier = Modifier.padding(16.dp),
//            verticalArrangement = Arrangement.spacedBy(16.dp)
//        ) {
//            // Unpaid debt
//            DebtSummaryCard(
//                debtSummary = DebtSummary(
//                    personId = "1",
//                    personName = "Nguyễn Văn A",
//                    debtType = DebtType.PAYABLE,
//                    originalAmount = 5000000.0,
//                    paidAmount = 2000000.0,
//                    remainingAmount = 3000000.0,
//                    isFullyPaid = false,
//                    lastPaymentDate = Date(),
//                    paymentHistory = listOf(
//                        PaymentRecord(Date(), 1000000.0, "Trả lần 1", 1),
//                        PaymentRecord(Date(), 1000000.0, "Trả lần 2", 2)
//                    ),
//                    relatedTransactions = emptyList()
//                )
//            )
//
//            // Paid debt
//            DebtSummaryCard(
//                debtSummary = DebtSummary(
//                    personId = "2",
//                    personName = "Trần Thị B",
//                    debtType = DebtType.RECEIVABLE,
//                    originalAmount = 2000000.0,
//                    paidAmount = 2000000.0,
//                    remainingAmount = 0.0,
//                    isFullyPaid = true,
//                    lastPaymentDate = Date(),
//                    paymentHistory = listOf(
//                        PaymentRecord(Date(), 2000000.0, "Thu đầy đủ", 1)
//                    ),
//                    relatedTransactions = emptyList()
//                )
//            )
//        }
//    }
//}