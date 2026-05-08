package com.uet.expensetracker.features.ai.ui.chat

import com.uet.expensetracker.core.platform.MviEventBase
import com.uet.expensetracker.core.platform.MviIntentBase
import com.uet.expensetracker.core.platform.MviStateBase
import com.uet.expensetracker.features.ai.data.remote.dto.ParsedTransactionDto

enum class ChatAiMode {
    CHAT,
    TRANSACTION
}

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val suggestions: List<String> = emptyList(),
    val data: Map<String, Any>? = null // Structured data (analytics results, loan options, etc.)
)

data class ChatAiState(
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val mode: ChatAiMode = ChatAiMode.CHAT
) : MviStateBase

sealed interface ChatAiIntent : MviIntentBase {
    data class UpdateInput(val text: String) : ChatAiIntent
    data object Send : ChatAiIntent
    data object ClearHistory : ChatAiIntent
    data class SelectMode(val mode: ChatAiMode) : ChatAiIntent
    data class ApplySuggestion(val text: String, val sendImmediately: Boolean = true) : ChatAiIntent
    data class HandleAction(val action: String, val payload: Map<String, Any>? = null) : ChatAiIntent
}

sealed interface ChatAiEvent : MviEventBase {
    data class ShowError(val message: String) : ChatAiEvent
    data class PrefillTransaction(val parsed: ParsedTransactionDto) : ChatAiEvent
    data object HistoryCleared : ChatAiEvent
    data class NavigateToScreen(val route: String, val arguments: Map<String, Any>? = null) : ChatAiEvent
}


