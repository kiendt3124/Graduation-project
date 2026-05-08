package com.uet.expensetracker.features.ai.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.uet.expensetracker.features.ai.ui.chat.MarkdownText
import com.uet.expensetracker.R
import com.uet.expensetracker.ui.theme.AppColor
import com.uet.expensetracker.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatAiScreen(
    onPrefillTransaction: (parsed: com.uet.expensetracker.features.ai.data.remote.dto.ParsedTransactionDto) -> Unit = {},
    onNavigate: (String, Map<String, Any>?) -> Unit = { _, _ -> },
    onNavigateBack: () -> Unit = {},
    viewModel: ChatAiViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.viewState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val isRecordingState = remember { mutableStateOf(false) }
    val baseInputBeforeVoice = remember { mutableStateOf("") }
    val pendingStartAfterPermission = remember { mutableStateOf(false) }

    val voiceController = remember(context) {
        VoiceToTextController(
            context = context,
            onPartial = { partial ->
                // Keep user's typed prefix and replace the voice part while recording.
                val prefix = baseInputBeforeVoice.value
                val combined = if (prefix.isBlank()) partial else "$prefix $partial"
                viewModel.processIntent(ChatAiIntent.UpdateInput(combined.trim()))
            },
            onFinal = { finalText ->
                val prefix = baseInputBeforeVoice.value
                val combined = if (prefix.isBlank()) finalText else "$prefix $finalText"
                viewModel.processIntent(ChatAiIntent.UpdateInput(combined.trim()))
                isRecordingState.value = false
            },
            onError = { msg ->
                isRecordingState.value = false
                scope.launch { snackbarHostState.showSnackbar(msg) }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose { voiceController.destroy() }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && pendingStartAfterPermission.value) {
            pendingStartAfterPermission.value = false
            baseInputBeforeVoice.value = state.input.trim()
            isRecordingState.value = true
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            voiceController.start(locale = "vi-VN")
        } else {
            pendingStartAfterPermission.value = false
            scope.launch { snackbarHostState.showSnackbar("Cần quyền micro để dùng Voice-to-text") }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.event.collectLatest { event ->
            when (event) {
                is ChatAiEvent.PrefillTransaction -> onPrefillTransaction(event.parsed)
                is ChatAiEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
                ChatAiEvent.HistoryCleared -> snackbarHostState.showSnackbar("Đã xóa lịch sử")
                else -> {}
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AppColor.Light.PrimaryColor.containerColor,
        topBar = {
            TopAppBar(
                title = { Text("AI Assistant", color = AppColor.Light.PrimaryColor.contentColor) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AppColor.Light.PrimaryColor.contentColor
                        )
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = AppColor.Light.PrimaryColor.contentColor,
                    navigationIconContentColor = AppColor.Light.PrimaryColor.contentColor,
                    actionIconContentColor = AppColor.Light.PrimaryColor.contentColor
                ),
                actions = {
                    IconButton(onClick = { viewModel.processIntent(ChatAiIntent.ClearHistory) }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear history")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            // Message list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                reverseLayout = true,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (state.isLoading) {
                    item {
                        BotBubbleTyping()
                    }
                }
                items(state.messages.asReversed()) { msg ->
                    if (msg.isUser) {
                        UserBubble(text = msg.text)
                    } else {
                        BotBubbleRich(
                            text = msg.text,
                            suggestions = msg.suggestions,
                            data = msg.data,
                            onSuggestionClick = { suggestion ->
                                viewModel.processIntent(ChatAiIntent.ApplySuggestion(text = suggestion, sendImmediately = true))
                            },
                            onActionClick = { action, payload ->
                                viewModel.processIntent(ChatAiIntent.HandleAction(action = action, payload = payload))
                            }
                        )
                    }
                }
            }

            // Global suggestion chips (only show if no message-specific suggestions and in CHAT mode)
            val lastMessage = state.messages.lastOrNull()
            if (lastMessage?.suggestions.isNullOrEmpty() && !state.isLoading && state.mode == ChatAiMode.CHAT) {
                SuggestionRow(
                    onSuggestion = { text ->
                        viewModel.processIntent(ChatAiIntent.ApplySuggestion(text = text, sendImmediately = true))
                    }
                )
            }

            // Input area
            InputArea(
                input = state.input,
                mode = state.mode,
                isLoading = state.isLoading,
                onInputChange = { viewModel.processIntent(ChatAiIntent.UpdateInput(it)) },
                onSelectMode = { viewModel.processIntent(ChatAiIntent.SelectMode(it)) },
                onSend = { viewModel.processIntent(ChatAiIntent.Send) },
                isRecording = isRecordingState.value,
                onMicPressAndHold = {
                    // Start recording on press if permission granted; stop on release.
                    if (state.isLoading) return@InputArea
                    if (!voiceController.isAvailable) {
                        scope.launch { snackbarHostState.showSnackbar("Voice-to-text không khả dụng trên thiết bị này") }
                        return@InputArea
                    }
                    val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.RECORD_AUDIO
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (!hasPermission) {
                        pendingStartAfterPermission.value = true
                        permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                        return@InputArea
                    }
                    baseInputBeforeVoice.value = state.input.trim()
                    isRecordingState.value = true
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    voiceController.start(locale = "vi-VN")
                },
                onMicRelease = {
                    if (isRecordingState.value) {
                        voiceController.stop()
                        isRecordingState.value = false
                    }
                }
            )
        }
    }
}

@Composable
private fun UserBubble(text: String) {
    val bg = AppColor.Light.PrimaryColor.TextButtonColor
    val fg = Color.White
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 6.dp))
                .background(bg)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(text = text, color = fg, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun BotBubbleRich(
    text: String,
    suggestions: List<String> = emptyList(),
    data: Map<String, Any>? = null,
    onSuggestionClick: (String) -> Unit,
    onActionClick: (String, Map<String, Any>?) -> Unit
) {
    val bg = Color(0xFFE3F2FD) // Pale blue background to differentiate from user bubble
    val fg = AppColor.Light.PrimaryColor.contentColor

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_ai_chatbot),
                    contentDescription = "AI Chatbot",
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                modifier = Modifier
                    .widthIn(max = 320.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 6.dp, bottomEnd = 18.dp))
                        .background(bg)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Column {
                        MarkdownText(markdown = text)
                        
                        // Render structured data if available
                        data?.let { renderStructuredData(it, onActionClick) }
                    }
                }
                
                // Render suggestions below the bubble
                if (suggestions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(suggestions) { suggestion ->
                            SuggestionChip(
                                onClick = { onSuggestionClick(suggestion) },
                                label = { Text(suggestion, maxLines = 1, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun renderStructuredData(data: Map<String, Any>, onActionClick: (String, Map<String, Any>?) -> Unit) {
    // Extract type from data
    val type = data["type"] as? String
    
    when (type) {
        "CATEGORY_SPENDING" -> {
            CategorySpendingCard(data = data)
        }
        "MONTHLY_COMPARISON" -> {
            MonthlyComparisonCard(data = data)
        }
        "SAVINGS_POTENTIAL" -> {
            SavingsPotentialCard(data = data)
        }
        "LOAN_RECOMMENDATION" -> {
            LoanRecommendationCard(data = data, onActionClick = onActionClick)
        }
        "INVESTMENT_RECOMMENDATION" -> {
            InvestmentRecommendationCard(data = data, onActionClick = onActionClick)
        }
        else -> {
            // Generic data rendering - could show as JSON or simple text
            if (data.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "[Dữ liệu phân tích có sẵn]",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColor.Light.PrimaryColor.contentColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun CategorySpendingCard(data: Map<String, Any>) {
    val topCategories = (data["topCategories"] as? List<*>)?.filterIsInstance<Map<String, Any>>() ?: emptyList()
    
    if (topCategories.isEmpty()) return
    
    Spacer(modifier = Modifier.height(12.dp))
    Column {
        Text(
            text = "Top danh mục chi tiêu",
            style = MaterialTheme.typography.labelMedium,
            color = AppColor.Light.PrimaryColor.contentColor
        )
        Spacer(modifier = Modifier.height(8.dp))
        topCategories.take(5).forEach { category ->
            val name = (category["category"] as? String) ?: ""
            val amount = (category["amount"] as? Number)?.toDouble() ?: 0.0
            val percentage = (category["percentage"] as? Number)?.toDouble() ?: 0.0
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "${String.format("%.1f", percentage)}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColor.Light.PrimaryColor.TextButtonColor
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun MonthlyComparisonCard(data: Map<String, Any>) {
    val monthlyData = (data["monthlyData"] as? List<*>)?.filterIsInstance<Map<String, Any>>() ?: emptyList()
    
    if (monthlyData.isEmpty()) return
    
    Spacer(modifier = Modifier.height(12.dp))
    Column {
        Text(
            text = "So sánh theo tháng",
            style = MaterialTheme.typography.labelMedium,
            color = AppColor.Light.PrimaryColor.contentColor
        )
        Spacer(modifier = Modifier.height(8.dp))
        monthlyData.take(6).forEach { month ->
            val monthName = (month["month"] as? String) ?: ""
            val income = (month["income"] as? Number)?.toDouble() ?: 0.0
            val expense = (month["expense"] as? Number)?.toDouble() ?: 0.0
            
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = monthName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Thu: ${String.format("%,.0f", income)} VND",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColor.Light.IncomeAmountColor
                    )
                    Text(
                        text = "Chi: ${String.format("%,.0f", expense)} VND",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColor.Light.ExpenseAmountColor
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun SavingsPotentialCard(data: Map<String, Any>) {
    val currentSavings = (data["currentSavings"] as? Number)?.toDouble() ?: 0.0
    val potentialSavings = (data["potentialSavings"] as? Number)?.toDouble() ?: 0.0
    val savingsRate = (data["savingsRate"] as? Number)?.toDouble() ?: 0.0
    
    Spacer(modifier = Modifier.height(12.dp))
    Column {
        Text(
            text = "Tiềm năng tiết kiệm",
            style = MaterialTheme.typography.labelMedium,
            color = AppColor.Light.PrimaryColor.contentColor
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Tiết kiệm hiện tại:",
                style = MaterialTheme.typography.bodySmall
            )
                    Text(
                        text = "${String.format("%,.0f", currentSavings)} VND",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColor.Light.PrimaryColor.TextButtonColor
                    )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Tỷ lệ tiết kiệm:",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "${String.format("%.1f", savingsRate * 100)}%",
                style = MaterialTheme.typography.bodySmall,
                color = AppColor.Light.PrimaryColor.TextButtonColor
            )
        }
    }
}

@Composable
private fun LoanRecommendationCard(
    data: Map<String, Any>,
    onActionClick: (String, Map<String, Any>?) -> Unit
) {
    val eligibility = (data["eligibility"] as? Map<String, Any>) ?: emptyMap()
    val providers = (data["providers"] as? List<*>)?.filterIsInstance<Map<String, Any>>() ?: emptyList()
    
    if (providers.isEmpty()) return
    
    Spacer(modifier = Modifier.height(12.dp))
    Column {
        // Eligibility section
        val isEligible = (eligibility["eligible"] as? Boolean) ?: false
        Text(
            text = if (isEligible) "✅ Đủ điều kiện vay" else "❌ Chưa đủ điều kiện vay",
            style = MaterialTheme.typography.labelMedium,
            color = if (isEligible) AppColor.Light.PrimaryColor.TextButtonColor else MaterialTheme.colorScheme.error
        )
        
        if (!isEligible) {
            val reason = (eligibility["reason"] as? String) ?: ""
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = reason,
                style = MaterialTheme.typography.bodySmall,
                color = AppColor.Light.PrimaryColor.contentColor
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Các nhà cung cấp được đề xuất",
            style = MaterialTheme.typography.labelMedium,
            color = AppColor.Light.PrimaryColor.contentColor
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        providers.take(3).forEach { provider ->
            val name = (provider["name"] as? String) ?: ""
            val interestRate = (provider["interestRate"] as? Number)?.toDouble() ?: 0.0
            val termMonths = (provider["termMonths"] as? Number)?.toInt() ?: 0
            val pros = (provider["pros"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Lãi suất: ${String.format("%.1f", interestRate)}%/năm",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Kỳ hạn: $termMonths tháng",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (pros.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ưu điểm: ${pros.take(2).joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColor.Light.PrimaryColor.TextButtonColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InvestmentRecommendationCard(
    data: Map<String, Any>,
    onActionClick: (String, Map<String, Any>?) -> Unit
) {
    val options = (data["options"] as? List<*>)?.filterIsInstance<Map<String, Any>>() ?: emptyList()
    val allocation = (data["allocation"] as? Map<String, Any>) ?: emptyMap()
    
    Spacer(modifier = Modifier.height(12.dp))
    Column {
        Text(
            text = "Lựa chọn đầu tư được đề xuất",
            style = MaterialTheme.typography.labelMedium,
            color = AppColor.Light.PrimaryColor.contentColor
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        options.take(3).forEach { option ->
            val name = (option["name"] as? String) ?: ""
            val riskLevel = (option["riskLevel"] as? String) ?: ""
            val expectedReturn = (option["expectedReturn"] as? Number)?.toDouble() ?: 0.0
            val description = (option["description"] as? String) ?: ""
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                        RiskLevelText(riskLevel = riskLevel)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Lợi nhuận kỳ vọng: ${String.format("%.1f", expectedReturn)}%/năm",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColor.Light.PrimaryColor.TextButtonColor
                        )
                    }
                    if (description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
        
        // Allocation section
        val allocations = (allocation["allocations"] as? List<*>)?.filterIsInstance<Map<String, Any>>() ?: emptyList()
        if (allocations.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Phân bổ đầu tư được khuyến nghị",
                style = MaterialTheme.typography.labelMedium,
                color = AppColor.Light.PrimaryColor.contentColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            allocations.forEach { alloc ->
                val type = (alloc["type"] as? String) ?: ""
                val percentage = (alloc["percentage"] as? Number)?.toDouble() ?: 0.0
                val amount = (alloc["amount"] as? Number)?.toDouble() ?: 0.0
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = type,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "${String.format("%.0f", percentage)}% (${String.format("%,.0f", amount)} VND)",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColor.Light.PrimaryColor.TextButtonColor
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun RiskLevelText(riskLevel: String) {
    val color = when (riskLevel) {
        "LOW" -> AppColor.Light.PrimaryColor.TextButtonColor
        "MEDIUM" -> MaterialTheme.colorScheme.secondary
        "HIGH" -> MaterialTheme.colorScheme.error
        else -> AppColor.Light.PrimaryColor.contentColor
    }
    Text(
        text = riskLevel,
        style = MaterialTheme.typography.labelSmall,
        color = color
    )
}

@Composable
private fun BotBubbleTyping() {
    val bg = Color(0xFFE3F2FD) // Pale blue background to differentiate from user bubble
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_ai_chatbot),
                contentDescription = "AI Chatbot",
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 6.dp, bottomEnd = 18.dp))
                .background(bg)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            TypingIndicator()
        }
    }
}

@Composable
private fun SuggestionRow(
    onSuggestion: (String) -> Unit
) {
    val suggestions = listOf(
        "Thống kê chi tiêu tháng này",
        "So sánh chi tiêu 3 tháng gần đây",
        "Tư vấn cho tôi nơi vay tiền uy tín ",
        "Cách chi tiêu hiệu quả"
    )
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(suggestions) { s ->
            SuggestionChip(
                onClick = { onSuggestion(s) },
                label = { Text(s, maxLines = 1) }
            )
        }
    }
}

@Composable
private fun InputArea(
    input: String,
    mode: ChatAiMode,
    isLoading: Boolean,
    isRecording: Boolean,
    onInputChange: (String) -> Unit,
    onSelectMode: (ChatAiMode) -> Unit,
    onMicPressAndHold: () -> Unit,
    onMicRelease: () -> Unit,
    onSend: () -> Unit,
) {
    val sendEnabled = input.trim().isNotEmpty() && !isLoading

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = mode == ChatAiMode.CHAT,
                onClick = { onSelectMode(ChatAiMode.CHAT) },
                label = { Text("Chat") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFFFF3E0), // Light orange background when selected
                    selectedLabelColor = AppColor.Light.PrimaryColor.TextButtonColor, // Orange text when selected
                    containerColor = Color.White, // White background when unselected
                    labelColor = TextSecondary // Gray text when unselected
                )
            )
            FilterChip(
                selected = mode == ChatAiMode.TRANSACTION,
                onClick = { onSelectMode(ChatAiMode.TRANSACTION) },
                label = { Text("Giao dịch") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFFFF3E0), // Light orange background when selected
                    selectedLabelColor = AppColor.Light.PrimaryColor.TextButtonColor, // Orange text when selected
                    containerColor = Color.White, // White background when unselected
                    labelColor = TextSecondary // Gray text when unselected
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = if (mode == ChatAiMode.TRANSACTION)
                            "Nhập mô tả giao dịch (vd: Hôm nay ăn phở 50k)..."
                        else
                            "Nhập tin nhắn..."
                    )
                },
                minLines = 1,
                maxLines = 5,
                shape = RoundedCornerShape(24.dp),
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            onMicPressAndHold()
                                            try {
                                                awaitRelease()
                                            } finally {
                                                onMicRelease()
                                            }
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val tint = if (isRecording) AppColor.Light.PrimaryColor.TextButtonColor else AppColor.Light.PrimaryColor.contentColor
                            Icon(imageVector = Icons.Default.Mic, contentDescription = "Mic", tint = tint)
                        }
                        IconButton(onClick = onSend, enabled = sendEnabled) {
                            Icon(imageVector = Icons.Default.Send, contentDescription = "Send")
                        }
                    }
                }
            )
        }

        Text(
            text = if (mode == ChatAiMode.TRANSACTION) "Mô tả giao dịch của bạn" else "",
            style = MaterialTheme.typography.labelSmall,
            color = AppColor.Light.PrimaryColor.contentColor,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
        )
    }
}


