package com.ptit.expensetracker.backend.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptit.expensetracker.backend.ai.function.AiFunction;
import com.ptit.expensetracker.backend.ai.function.AiFunctionRegistry;
import com.ptit.expensetracker.backend.ai.function.FunctionCall;
import com.ptit.expensetracker.backend.ai.gemini.GeminiClient;
import com.ptit.expensetracker.backend.ai.gemini.GeminiPromptBuilder;
import com.ptit.expensetracker.backend.ai.gemini.dto.GeminiRequest;
import com.ptit.expensetracker.backend.ai.gemini.dto.GeminiResponse;
import com.ptit.expensetracker.backend.common.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Routes user intents to appropriate handlers using Gemini function calling.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IntelligentRoutingService {

    private final GeminiClient geminiClient;
    private final AiFunctionRegistry functionRegistry;
    private final GeminiPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    /**
     * Analyze user message and route to appropriate function.
     */
    public FunctionCall routeIntent(String userId, String userMessage, String financialContext) {
        try {
            // Build prompt xác định function
            String prompt = buildRoutingPrompt(userMessage, financialContext);

            // Chuẩn bị danh sách function
            List<GeminiRequest.Tool> tools = functionRegistry.getAllFunctions().stream()
                    .map(this::toTool)
                    .collect(Collectors.toList());

            // Build request with function calling
            GeminiRequest request = GeminiRequest.builder()
                    .contents(List.of(GeminiRequest.Content.builder()
                                    .parts(List.of(GeminiRequest.Part.builder().text(prompt).build()))
                                    .build()
                    ))
                    .tools(tools)
                    .build();

            // Call Gemini with function calling
            GeminiResponse response = geminiClient.callWithFunctions(request);

            // Extract function call from response
            FunctionCall functionCall = extractFunctionCall(response);
            if (functionCall != null) {
                log.info("Routed to function: {} with args: {}", functionCall.getName(), functionCall.getArgs());
                return functionCall;
            }

            // Fallback to general_chat if no function call
            log.warn("No function call detected, defaulting to general_chat");
            return FunctionCall.builder()
                    .name("general_chat")
                    .args(Map.of("message", userMessage))
                    .build();

        } catch (Exception e) {
            log.error("Error routing intent: {}", e.getMessage(), e);
            // Fallback to general_chat on error
            return FunctionCall.builder()
                    .name("general_chat")
                    .args(Map.of("message", userMessage))
                    .build();
        }
    }

    private String buildRoutingPrompt(String userMessage, String context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Bạn là hệ thống routing thông minh cho ứng dụng quản lý chi tiêu. ");
        sb.append("Nhiệm vụ: phân tích câu hỏi của user và chọn function phù hợp nhất.\n\n");

        if (context != null && !context.isBlank()) {
            sb.append("User Financial Context:\n");
            sb.append(context).append("\n\n");
        }

        sb.append("User Message: ").append(userMessage).append("\n\n");
        sb.append("Hãy chọn function phù hợp nhất:\n");
        sb.append("- general_chat: Câu hỏi chung, tips, trò chuyện\n");
        sb.append("- analyze_spending: Phân tích, so sánh, thống kê chi tiêu\n");
        sb.append("- recommend_loan: Tư vấn vay vốn\n");
        sb.append("- recommend_investment: Tư vấn đầu tư, tiết kiệm\n\n");
        sb.append("Nếu không chắc chắn, dùng general_chat.");

        return sb.toString();
    }

    private GeminiRequest.Tool toTool(AiFunction function) {
        return GeminiRequest.Tool.builder()
                .function_declarations(List.of(GeminiRequest.FunctionDeclaration.builder()
                        .name(function.getName())
                        .description(function.getDescription())
                        .parameters(function.getParameters())
                        .build()))
                .build();
    }

    private FunctionCall extractFunctionCall(GeminiResponse response) {
        if (response == null || response.getCandidates() == null || response.getCandidates().isEmpty()) {
            return null;
        }

        for (GeminiResponse.Candidate candidate : response.getCandidates()) {
            if (candidate.getContent() != null && candidate.getContent().getParts() != null) {
                for (GeminiResponse.Part part : candidate.getContent().getParts()) {
                    if (part.getFunctionCall() != null) {
                        GeminiResponse.FunctionCall fc = part.getFunctionCall();
                        return FunctionCall.builder()
                                .name(fc.getName())
                                .args(parseArgs(fc.getArgs()))
                                .build();
                    }
                }
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseArgs(Map<String, Object> args) {
        if (args == null) {
            return Map.of();
        }
        // Convert args to proper types
        return objectMapper.convertValue(args, new TypeReference<Map<String, Object>>() {});
    }
}

