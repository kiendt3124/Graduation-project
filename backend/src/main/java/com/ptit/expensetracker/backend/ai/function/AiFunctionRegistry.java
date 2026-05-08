package com.ptit.expensetracker.backend.ai.function;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of available AI functions for function calling.
 */
@Service
@Slf4j
public class AiFunctionRegistry {

    private final Map<String, AiFunction> functions = new HashMap<>();

    @PostConstruct
    public void registerFunctions() {
        // Function 1: General chat
        functions.put("general_chat", AiFunction.builder()
                .name("general_chat")
                .description("Trả lời câu hỏi chung về quản lý tài chính, gợi ý, tips, hoặc trò chuyện thông thường")
                .parameters(buildJsonSchema(Map.of(
                        "message", Map.of("type", "string", "description", "User's question or message")
                )))
                .build());

        // Function 2: Analyze spending
        functions.put("analyze_spending", AiFunction.builder()
                .name("analyze_spending")
                .description("Phân tích chi tiêu với dữ liệu cụ thể (so sánh tháng, xu hướng, báo cáo, thống kê)")
                .parameters(buildJsonSchema(Map.of(
                        "analysisType", Map.of(
                                "type", "string",
                                "enum", List.of("COMPARE_MONTHS", "TREND_ANALYSIS", "CATEGORY_BREAKDOWN", "GENERIC"),
                                "description", "Type of analysis to perform"
                        ),
                        "timeRange", Map.of(
                                "type", "string",
                                "enum", List.of("THIS_MONTH", "LAST_3_MONTHS", "LAST_6_MONTHS", "LAST_12_MONTHS"),
                                "description", "Time range for analysis"
                        ),
                        "category", Map.of(
                                "type", "string",
                                "description", "Optional category filter (metadata)"
                        )
                )))
                .build());

        // Function 3: Recommend loan
        functions.put("recommend_loan", AiFunction.builder()
                .name("recommend_loan")
                .description("Tư vấn vay vốn dựa trên tình hình tài chính của người dùng")
                .parameters(buildJsonSchema(Map.of(
                        "loanAmount", Map.of(
                                "type", "number",
                                "description", "Desired loan amount (optional)"
                        ),
                        "purpose", Map.of(
                                "type", "string",
                                "description", "Loan purpose (optional)"
                        )
                )))
                .build());

        // Function 4: Recommend investment
        functions.put("recommend_investment", AiFunction.builder()
                .name("recommend_investment")
                .description("Tư vấn đầu tư và tiết kiệm dựa trên số dư và savings rate")
                .parameters(buildJsonSchema(Map.of(
                        "investmentType", Map.of(
                                "type", "string",
                                "enum", List.of("SAVINGS_ACCOUNT", "STOCKS", "BONDS", "MIXED"),
                                "description", "Type of investment (optional)"
                        ),
                        "riskTolerance", Map.of(
                                "type", "string",
                                "enum", List.of("LOW", "MEDIUM", "HIGH"),
                                "description", "Risk tolerance level (optional)"
                        )
                )))
                .build());

        log.info("Registered {} AI functions", functions.size());
    }

    public List<AiFunction> getAllFunctions() {
        return new ArrayList<>(functions.values());
    }

    public AiFunction getFunction(String name) {
        return functions.get(name);
    }

    /**
     * Build JSON schema for function parameters.
     */
    private Map<String, Object> buildJsonSchema(Map<String, Map<String, Object>> properties) {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", List.of()); // No required fields for flexibility
        return schema;
    }
}

