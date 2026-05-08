package com.ptit.expensetracker.backend.ai.service.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptit.expensetracker.backend.ai.dto.ChatResponse;
import com.ptit.expensetracker.backend.ai.dto.context.FinancialContextDto;
import com.ptit.expensetracker.backend.ai.gemini.GeminiClient;
import com.ptit.expensetracker.backend.ai.gemini.GeminiPromptBuilder;
import com.ptit.expensetracker.backend.ai.service.UserContextService;
import com.ptit.expensetracker.backend.ai.service.analytics.BackendAnalyticsEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Handles spending analysis requests with full analytics engine.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsAgent {

    private final BackendAnalyticsEngine analyticsEngine;
    private final UserContextService contextService;
    private final GeminiClient geminiClient;
    private final GeminiPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    public ChatResponse analyze(
            String userId,
            String analysisType,
            String timeRange,
            String financialContext
    ) {
        try {
            // Get financial context
            FinancialContextDto context = contextService.getContext(userId);

            // Perform analysis based on type
            Map<String, Object> analysisResult = performAnalysis(context, analysisType, timeRange);

            // Generate natural language response using Gemini
            String naturalLanguageReply = generateNaturalLanguageResponse(
                    analysisType,
                    timeRange,
                    analysisResult,
                    financialContext
            );

            // Build suggestions based on analysis
            List<String> suggestions = buildSuggestions(analysisType, analysisResult);

            return ChatResponse.builder()
                    .reply(naturalLanguageReply)
                    .suggestions(suggestions)
                    .data(analysisResult)
                    .build();

        } catch (Exception e) {
            log.error("Error in analytics agent for user {}: {}", userId, e.getMessage(), e);
            return ChatResponse.builder()
                    .reply("Xin lỗi, tôi gặp lỗi khi phân tích dữ liệu. Vui lòng thử lại sau.")
                    .suggestions(Collections.emptyList())
                    .data(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    private Map<String, Object> performAnalysis(
            FinancialContextDto context,
            String analysisType,
            String timeRange
    ) {
        return switch (analysisType.toUpperCase()) {
            case "CATEGORY_SPENDING", "SPENDING_BY_CATEGORY" -> 
                analyticsEngine.analyzeCategorySpending(context, timeRange);
            
            case "MONTHLY_COMPARISON", "COMPARE_MONTHS" -> 
                analyticsEngine.analyzeMonthlyComparison(context, 3);
            
            case "SPENDING_TREND", "TREND" -> 
                analyticsEngine.analyzeSpendingTrend(context, timeRange);
            
            case "BUDGET_VS_ACTUAL", "BUDGET" -> 
                analyticsEngine.analyzeBudgetVsActual(context, timeRange);
            
            case "SAVINGS_POTENTIAL", "SAVINGS" -> 
                analyticsEngine.analyzeSavingsPotential(context);
            
            case "SPENDING_PATTERNS", "PATTERNS" -> 
                analyticsEngine.analyzeSpendingPatterns(context, timeRange);
            
            default -> {
                // Default to category spending
                log.warn("Unknown analysis type: {}, defaulting to CATEGORY_SPENDING", analysisType);
                yield analyticsEngine.analyzeCategorySpending(context, timeRange);
            }
        };
    }

    private String generateNaturalLanguageResponse(
            String analysisType,
            String timeRange,
            Map<String, Object> analysisResult,
            String financialContext
    ) {
        try {
            String analysisJson = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(analysisResult);

            String prompt = String.format(
                    """
                    Bạn là một chuyên gia tài chính cá nhân. Hãy phân tích và giải thích kết quả phân tích chi tiêu dưới đây một cách dễ hiểu, thân thiện và có hành động cụ thể.
                    
                    Loại phân tích: %s
                    Khoảng thời gian: %s
                    
                    Kết quả phân tích (JSON):
                    %s
                    
                    Bối cảnh tài chính người dùng:
                    %s
                    
                    Yêu cầu tất cả đều ngắn gọn:
                    1. Giải thích kết quả phân tích một cách ngắn gọn, rõ ràng, dễ hiểu
                    2. Chỉ ra các điểm đáng chú ý (tích cực hoặc cần cải thiện)
                    3. Đưa ra 2-3 gợi ý hành động cụ thể dựa trên kết quả
                    4. Sử dụng tiếng Việt, giọng điệu thân thiện
                    5. Định dạng số tiền với dấu phẩy ngăn cách hàng nghìn (ví dụ: 1,000,000 VND)
                    6. Không cần lặp lại toàn bộ dữ liệu, chỉ tập trung vào insights quan trọng
                    
                    Trả lời:
                    """,
                    analysisType,
                    timeRange,
                    analysisJson,
                    financialContext != null ? financialContext : "Không có bối cảnh"
            );

            return geminiClient.generateText(prompt);

        } catch (Exception e) {
            log.error("Error generating natural language response: {}", e.getMessage(), e);
            return buildFallbackResponse(analysisType, analysisResult);
        }
    }

    private String buildFallbackResponse(String analysisType, Map<String, Object> analysisResult) {
        StringBuilder sb = new StringBuilder();
        sb.append("Kết quả phân tích ").append(analysisType).append(":\n\n");

        if (analysisResult.containsKey("error")) {
            sb.append("Lỗi: ").append(analysisResult.get("error"));
        } else {
            sb.append("Dữ liệu phân tích đã được xử lý. Vui lòng xem chi tiết trong phần dữ liệu.");
        }

        return sb.toString();
    }

    private List<String> buildSuggestions(String analysisType, Map<String, Object> analysisResult) {
        List<String> suggestions = new ArrayList<>();

        // Add suggestions based on analysis type
        switch (analysisType.toUpperCase()) {
            case "CATEGORY_SPENDING":
                suggestions.add("Xem báo cáo chi tiết");
                suggestions.add("Tạo ngân sách mới");
                break;
            
            case "MONTHLY_COMPARISON", "SPENDING_TREND":
                suggestions.add("Xem biểu đồ theo tháng");
                suggestions.add("Đặt mục tiêu chi tiêu");
                break;
            
            case "SAVINGS_POTENTIAL":
                suggestions.add("Tạo mục tiêu tiết kiệm");
                suggestions.add("Xem các lựa chọn đầu tư");
                break;
            
            case "BUDGET_VS_ACTUAL":
                suggestions.add("Cập nhật ngân sách");
                suggestions.add("Xem chi tiết ngân sách");
                break;
        }

        // Add context-specific suggestions
        if (analysisResult.containsKey("recommendation")) {
            String recommendation = analysisResult.get("recommendation").toString();
            if ("LOW".equals(recommendation)) {
                suggestions.add("Giảm chi tiêu");
            }
        }

        return suggestions;
    }
}

