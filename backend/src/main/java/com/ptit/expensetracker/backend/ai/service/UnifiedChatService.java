package com.ptit.expensetracker.backend.ai.service;

import com.ptit.expensetracker.backend.ai.domain.AiChatMessage;
import com.ptit.expensetracker.backend.ai.domain.MessageRole;
import com.ptit.expensetracker.backend.ai.dto.ChatRequest;
import com.ptit.expensetracker.backend.ai.dto.ChatResponse;
import com.ptit.expensetracker.backend.ai.function.FunctionCall;
import com.ptit.expensetracker.backend.ai.repository.AiChatMessageRepository;
import com.ptit.expensetracker.backend.ai.service.agent.AnalyticsAgent;
import com.ptit.expensetracker.backend.ai.service.agent.GeneralChatAgent;
import com.ptit.expensetracker.backend.ai.service.agent.InvestmentAdvisorAgent;
import com.ptit.expensetracker.backend.ai.service.agent.LoanAdvisorAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Unified chat service that routes requests to appropriate agents using intelligent routing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UnifiedChatService {

    private final IntelligentRoutingService routingService;
    private final UserContextService contextService;
    private final GeneralChatAgent generalChatAgent;
    private final AnalyticsAgent analyticsAgent;
    private final LoanAdvisorAgent loanAdvisorAgent;
    private final InvestmentAdvisorAgent investmentAdvisorAgent;
    private final AiChatMessageRepository chatMessageRepository;
    private final AiMemoryService memoryService;

    /**
     * Handle chat request with intelligent routing.
     */
    public ChatResponse handleChat(String userId, ChatRequest request) {
        try {
            // 1. Xây dựng context
            String financialContext = contextService.buildEnrichedContext(userId);

            // 2. Route đến bộ xử lý phù hợp
            FunctionCall functionCall = routingService.routeIntent(
                    userId,
                    request.getMessage(),
                    financialContext
            );

            log.info("Routing to function: {} for user: {}", functionCall.getName(), userId);

            // 3. Save user message
            AiChatMessage userMessage = AiChatMessage.builder()
                    .userId(userId)
                    .role(MessageRole.USER)
                    .content(request.getMessage())
                    .createdAt(OffsetDateTime.now())
                    .build();
            chatMessageRepository.save(userMessage);

            // 4. Execute function
            ChatResponse response = executeFunction(functionCall, userId, financialContext);

            // 5. Save assistant response
            AiChatMessage assistantMessage = AiChatMessage.builder()
                    .userId(userId)
                    .role(MessageRole.ASSISTANT)
                    .content(response.getReply())
                    .createdAt(OffsetDateTime.now())
                    .build();
            chatMessageRepository.save(assistantMessage);

            // 6. Update memory if needed
            memoryService.updateMemoryIfNeeded(userId);

            // 7. Update analytics cache if this was an analytics request
            if ("analyze_spending".equals(functionCall.getName()) && response.getData() != null) {
                contextService.updateAnalyticsCache(userId, response.getData());
            }

            return response;

        } catch (Exception e) {
            log.error("Error handling chat for user {}: {}", userId, e.getMessage(), e);
            // Fallback to general chat on error
            return generalChatAgent.handle(userId, request.getMessage(), null);
        }
    }

    private ChatResponse executeFunction(FunctionCall functionCall, String userId, String financialContext) {
        Map<String, Object> args = functionCall.getArgs();

        return switch (functionCall.getName()) {
            case "general_chat" -> {
                String message = (String) args.getOrDefault("message", "");
                yield generalChatAgent.handle(userId, message, financialContext);
            }

            case "analyze_spending" -> {
                String analysisType = (String) args.getOrDefault("analysisType", "GENERIC");
                String timeRange = (String) args.getOrDefault("timeRange", "THIS_MONTH");
                String category = (String) args.get("category");
                yield analyticsAgent.analyze(userId, analysisType, timeRange, financialContext);
            }

            case "recommend_loan" -> {
                Double loanAmount = args.get("loanAmount") != null
                        ? ((Number) args.get("loanAmount")).doubleValue()
                        : null;
                String purpose = (String) args.get("purpose");
                yield loanAdvisorAgent.recommend(userId, loanAmount, purpose, financialContext);
            }

            case "recommend_investment" -> {
                String investmentType = (String) args.get("investmentType");
                String riskTolerance = (String) args.get("riskTolerance");
                yield investmentAdvisorAgent.recommend(userId, investmentType, riskTolerance, financialContext);
            }

            default -> {
                log.warn("Unknown function: {}, defaulting to general_chat", functionCall.getName());
                String message = (String) args.getOrDefault("message", "");
                yield generalChatAgent.handle(userId, message, financialContext);
            }
        };
    }
}

