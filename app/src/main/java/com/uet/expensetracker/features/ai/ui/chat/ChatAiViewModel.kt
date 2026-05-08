package com.uet.expensetracker.features.ai.ui.chat

import androidx.lifecycle.viewModelScope
import com.uet.expensetracker.core.failure.Failure
import com.uet.expensetracker.core.platform.BaseViewModel
import com.uet.expensetracker.features.ai.domain.usecase.ChatAiUseCase
import com.uet.expensetracker.features.ai.domain.usecase.ParseTransactionAiUseCase
import com.uet.expensetracker.features.ai.domain.usecase.AiHistoryUseCase
import com.uet.expensetracker.features.ai.domain.usecase.AiClearHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatAiViewModel @Inject constructor(
    private val chatAiUseCase: ChatAiUseCase,
    private val parseTransactionAiUseCase: ParseTransactionAiUseCase,
    private val aiHistoryUseCase: AiHistoryUseCase,
    private val aiClearHistoryUseCase: AiClearHistoryUseCase,
) : BaseViewModel<ChatAiState, ChatAiIntent, ChatAiEvent>() {

    override val _viewState = MutableStateFlow(ChatAiState())

    init {
        loadHistory()
    }

    override fun processIntent(intent: ChatAiIntent) {
        when (intent) {
            is ChatAiIntent.UpdateInput -> _viewState.update { it.copy(input = intent.text) }
            ChatAiIntent.Send -> send()
            ChatAiIntent.ClearHistory -> clearHistory()
            is ChatAiIntent.SelectMode -> _viewState.update { it.copy(mode = intent.mode) }
            is ChatAiIntent.ApplySuggestion -> applySuggestion(intent.text, intent.sendImmediately)
            is ChatAiIntent.HandleAction -> handleAction(intent.action, intent.payload)
        }
    }

    private fun send() {
        val text = viewState.value.input.trim()
        if (text.isBlank()) return
        when (viewState.value.mode) {
            ChatAiMode.CHAT -> sendChat(text)
            ChatAiMode.TRANSACTION -> parseTransaction(text)
        }
    }

    private fun sendChat(text: String) {
        viewModelScope.launch {
            _viewState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    messages = it.messages + ChatMessage(text = text, isUser = true),
                    input = ""
                )
            }
            chatAiUseCase(ChatAiUseCase.Params(message = text)) { result ->
                result.fold(
                    { failure -> handleFailure(failure, "Gửi chat thất bại") },
                    { resp ->
                        _viewState.update { state ->
                            state.copy(
                                isLoading = false,
                                messages = state.messages + ChatMessage(
                                    text = resp.reply,
                                    isUser = false,
                                    suggestions = resp.suggestions ?: emptyList(),
                                    data = resp.data
                                )
                            )
                        }
                    }
                )
            }
        }
    }

    private fun handleAction(action: String, payload: Map<String, Any>?) {
        when (action) {
            "CREATE_BUDGET" -> {
                emitEvent(ChatAiEvent.NavigateToScreen("add_budget"))
            }
            "VIEW_LOAN_PROVIDERS" -> {
                // Could navigate to a loan providers screen, or show in chat
                // For now, just log
            }
            "VIEW_INVESTMENT_OPTIONS" -> {
                // Could navigate to investment options screen
                // For now, just log
            }
            "VIEW_DETAILED_REPORT" -> {
                emitEvent(ChatAiEvent.NavigateToScreen("monthly_report"))
            }
            "VIEW_MONTHLY_CHART" -> {
                emitEvent(ChatAiEvent.NavigateToScreen("home"))
            }
            else -> {
                // Unknown action, ignore
            }
        }
    }

    private fun parseTransaction(text: String) {
        viewModelScope.launch {
            _viewState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    messages = it.messages + ChatMessage(text = text, isUser = true),
                    input = ""
                )
            }
            parseTransactionAiUseCase(ParseTransactionAiUseCase.Params(text = text)) { result ->
                result.fold(
                    { failure -> handleFailure(failure, "Phân tích giao dịch thất bại") },
                    { parsed ->
                        _viewState.update { it.copy(isLoading = false) }
                        emitEvent(ChatAiEvent.PrefillTransaction(parsed))
                    }
                )
            }
        }
    }

    private fun clearHistory() {
        viewModelScope.launch {
            aiClearHistoryUseCase(com.uet.expensetracker.core.interactor.UseCase.None()) { result ->
                result.fold(
                    { failure -> handleFailure(failure, "Xóa lịch sử thất bại") },
                    {
                        _viewState.update { it.copy(messages = emptyList(), error = null) }
                        emitEvent(ChatAiEvent.HistoryCleared)
                    }
                )
            }
        }
    }

    private fun applySuggestion(text: String, sendImmediately: Boolean) {
        _viewState.update { it.copy(input = text) }
        if (sendImmediately) {
            processIntent(ChatAiIntent.Send)
        }
    }

    private fun handleFailure(failure: Failure, defaultMsg: String) {
        val msg = when (failure) {
            is Failure.NetworkConnection -> "Không có kết nối mạng"
            is Failure.ServerError -> "Lỗi máy chủ"
            is Failure.NotFound -> "Không tìm thấy"
            else -> defaultMsg
        }
        _viewState.update { it.copy(isLoading = false, error = msg) }
        emitEvent(ChatAiEvent.ShowError(msg))
    }


    private fun loadHistory() {
        viewModelScope.launch {
            aiHistoryUseCase(com.uet.expensetracker.core.interactor.UseCase.None()) { result ->
                result.fold(
                    { /* ignore on cold start */ },
                    { history ->
                        val mapped = history.map { h ->
                            ChatMessage(
                                text = h.content,
                                isUser = h.role.equals("USER", ignoreCase = true),
                                suggestions = emptyList(),
                                data = null
                            )
                        }
                        _viewState.update { it.copy(messages = mapped) }
                    }
                )
            }
        }
    }
}


