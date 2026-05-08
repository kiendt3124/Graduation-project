package com.ptit.expensetracker.backend.ai.controller;

import com.ptit.expensetracker.backend.ai.dto.ChatRequest;
import com.ptit.expensetracker.backend.ai.dto.ChatResponse;
import com.ptit.expensetracker.backend.ai.dto.InsightsRequest;
import com.ptit.expensetracker.backend.ai.dto.InsightsResponse;
import com.ptit.expensetracker.backend.ai.dto.ParseTransactionRequest;
import com.ptit.expensetracker.backend.ai.dto.ParsedTransactionDto;
import com.ptit.expensetracker.backend.ai.dto.ChatHistoryDto;
import com.ptit.expensetracker.backend.ai.dto.analyze.AiAnswerRequest;
import com.ptit.expensetracker.backend.ai.dto.analyze.AiAnswerResponse;
import com.ptit.expensetracker.backend.ai.dto.analyze.AiPlanRequest;
import com.ptit.expensetracker.backend.ai.dto.analyze.AiPlanResponse;
import com.ptit.expensetracker.backend.ai.service.AiService;
import com.ptit.expensetracker.backend.ai.service.UnifiedChatService;
import com.ptit.expensetracker.backend.common.exception.UnauthorizedException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API entrypoint for AI-related operations (chat, transaction parsing, insights).
 * <p>
 * Authentication:
 * In later steps, each request will be authenticated via Firebase ID token and
 * the user id will be resolved from the security context. For now we accept
 * a simple "X-Debug-User-Id" header for local testing.
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService; // For backward compatibility (parse-transaction, insights, etc.)
    private final UnifiedChatService unifiedChatService; // New unified chat handler

    /**
     * Unified chat endpoint with intelligent routing.
     * Automatically routes to appropriate handler (general chat, analytics, loan, investment).
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(
            Authentication authentication,
            @Valid @RequestBody ChatRequest request) {

        String userId = resolveUserId(authentication);
        ChatResponse response = unifiedChatService.handleChat(userId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/parse-transaction")
    public ResponseEntity<ParsedTransactionDto> parseTransaction(
            Authentication authentication,
            @Valid @RequestBody ParseTransactionRequest request) {

        String userId = resolveUserId(authentication);
        ParsedTransactionDto dto = aiService.parseTransaction(userId, request);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/insights")
    public ResponseEntity<InsightsResponse> generateInsights(
            Authentication authentication,
            @Valid @RequestBody InsightsRequest request) {

        String userId = resolveUserId(authentication);
        InsightsResponse response = aiService.generateInsights(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * @deprecated Use {@link #chat(Authentication, ChatRequest)} instead.
     * This endpoint is kept for backward compatibility only.
     * The unified chat endpoint automatically handles analytics queries.
     */
    @Deprecated
    @PostMapping("/plan")
    public ResponseEntity<AiPlanResponse> plan(
            Authentication authentication,
            @Valid @RequestBody AiPlanRequest request) {
        String userId = resolveUserId(authentication);
        return ResponseEntity.ok(aiService.plan(userId, request));
    }

    /**
     * @deprecated Use {@link #chat(Authentication, ChatRequest)} instead.
     * This endpoint is kept for backward compatibility only.
     * The unified chat endpoint automatically handles analytics queries.
     */
    @Deprecated
    @PostMapping("/answer")
    public ResponseEntity<AiAnswerResponse> answer(
            Authentication authentication,
            @Valid @RequestBody AiAnswerRequest request) {
        String userId = resolveUserId(authentication);
        return ResponseEntity.ok(aiService.answer(userId, request));
    }

    @GetMapping("/history")
    public ResponseEntity<List<ChatHistoryDto>> history(Authentication authentication) {
        String userId = resolveUserId(authentication);
        return ResponseEntity.ok(aiService.getHistory(userId));
    }

    @DeleteMapping("/history")
    public ResponseEntity<Void> clearHistory(Authentication authentication) {
        String userId = resolveUserId(authentication);
        aiService.clearHistory(userId);
        return ResponseEntity.noContent().build();
    }

    private String resolveUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Unauthorized");
        }
        return authentication.getName();
    }
}


