package com.ptit.expensetracker.backend.ai.service.agent;

import com.ptit.expensetracker.backend.ai.dto.ChatResponse;
import com.ptit.expensetracker.backend.ai.gemini.GeminiClient;
import com.ptit.expensetracker.backend.ai.gemini.GeminiPromptBuilder;
import com.ptit.expensetracker.backend.ai.service.AiMemoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Handles general chat conversations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeneralChatAgent {

    private final GeminiClient geminiClient;
    private final GeminiPromptBuilder promptBuilder;
    private final AiMemoryService memoryService;

    public ChatResponse handle(String userId, String message, String financialContext) {
        String contextHeader = memoryService.buildContextHeader(userId);
        String recent = memoryService.buildRecentVisibleTranscript(userId);

        String prompt = promptBuilder.buildChatPrompt(
                (contextHeader.isBlank() ? "" : contextHeader + "\n\n")
                        + (financialContext != null && !financialContext.isBlank() 
                            ? "User Financial Context:\n" + financialContext + "\n\n" 
                            : "")
                        + (recent.isBlank() ? "" : ("Recent messages:\n" + recent + "\n\n"))
                        + "User message: " + message,
                userId,
                "vi-VN"
        );

        String reply = geminiClient.generateChatReply(prompt, userId, "vi-VN");

        return ChatResponse.builder()
                .reply(reply)
                .suggestions(Collections.emptyList())
                .build();
    }
}

