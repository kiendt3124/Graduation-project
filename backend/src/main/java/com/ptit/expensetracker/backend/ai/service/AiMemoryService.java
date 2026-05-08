package com.ptit.expensetracker.backend.ai.service;

import com.ptit.expensetracker.backend.ai.domain.AiChatMessage;
import com.ptit.expensetracker.backend.ai.domain.AiConversation;
import com.ptit.expensetracker.backend.ai.gemini.GeminiClient;
import com.ptit.expensetracker.backend.ai.gemini.GeminiPromptBuilder;
import com.ptit.expensetracker.backend.ai.gemini.MemoryJsonParser;
import com.ptit.expensetracker.backend.ai.repository.AiChatMessageRepository;
import com.ptit.expensetracker.backend.ai.repository.AiConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.stream.Collectors;

/**
 * Maintains long-term memory as compact summary + pinnedFacts.
 * Transcript can be soft-deleted (hidden from UI) but memory persists.
 */
@Service
@RequiredArgsConstructor
public class AiMemoryService {

    private final AiConversationRepository conversationRepository;
    private final AiChatMessageRepository chatMessageRepository;
    private final GeminiPromptBuilder promptBuilder;
    private final GeminiClient geminiClient;
    private final MemoryJsonParser memoryJsonParser;

    public AiConversation getOrCreate(String userId) {
        return conversationRepository.findByUserId(userId)
                .orElseGet(() -> conversationRepository.save(
                        AiConversation.builder()
                                .userId(userId)
                                .summary("")
                                .pinnedFactsJson(null)
                                .lastSummarizedMessageId(0L)
                                .updatedAt(OffsetDateTime.now())
                                .build()
                ));
    }

    /**
     * Incrementally update memory using new transcript since lastSummarizedMessageId.
     * To keep cost low, only updates when at least N new messages exist.
     */
    public void updateMemoryIfNeeded(String userId) {
        AiConversation conv = getOrCreate(userId);
        Long lastId = conv.getLastSummarizedMessageId() == null ? 0L : conv.getLastSummarizedMessageId();
        var newMessages = chatMessageRepository.findByUserIdAndIdGreaterThanOrderByIdAsc(userId, lastId);
        if (newMessages == null || newMessages.size() < 6) {
            return;
        }

        String block = newMessages.stream()
                .map(m -> m.getRole().name() + ": " + m.getContent())
                .collect(Collectors.joining("\n"));

        String prompt = promptBuilder.buildMemoryUpdatePrompt(conv.getSummary(), conv.getPinnedFactsJson(), block);
        String raw = geminiClient.generateText(prompt);
        var parsed = memoryJsonParser.parse(raw);

        conv.setSummary(parsed.getSummary());
        conv.setPinnedFactsJson(parsed.getPinnedFactsJson());
        conv.setLastSummarizedMessageId(newMessages.get(newMessages.size() - 1).getId());
        conv.setUpdatedAt(OffsetDateTime.now());
        conversationRepository.save(conv);
    }

    public String buildContextHeader(String userId) {
        AiConversation conv = getOrCreate(userId);
        StringBuilder sb = new StringBuilder();
        if (conv.getPinnedFactsJson() != null && !conv.getPinnedFactsJson().isBlank()) {
            sb.append("PinnedFacts (JSON): ").append(conv.getPinnedFactsJson()).append("\n");
        }
        if (conv.getSummary() != null && !conv.getSummary().isBlank()) {
            sb.append("Conversation summary:\n").append(conv.getSummary()).append("\n");
        }
        return sb.toString().trim();
    }

    public String buildRecentVisibleTranscript(String userId) {
        var recent = chatMessageRepository.findTop12ByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);
        if (recent == null || recent.isEmpty()) return "";
        // reverse to chronological
        java.util.Collections.reverse(recent);
        return recent.stream()
                .map(m -> m.getRole().name() + ": " + m.getContent())
                .collect(Collectors.joining("\n"));
    }
}


























