package com.ptit.expensetracker.backend.ai.service.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptit.expensetracker.backend.ai.dto.ChatResponse;
import com.ptit.expensetracker.backend.ai.dto.context.FinancialContextDto;
import com.ptit.expensetracker.backend.ai.gemini.GeminiClient;
import com.ptit.expensetracker.backend.ai.service.UserContextService;
import com.ptit.expensetracker.backend.ai.service.provider.InvestmentProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Provides investment recommendations based on user's financial situation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvestmentAdvisorAgent {

    private final InvestmentProviderService investmentProviderService;
    private final UserContextService contextService;
    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    public ChatResponse recommend(
            String userId,
            String investmentType,
            String riskTolerance,
            String financialContext
    ) {
        try {
            // Get financial context
            FinancialContextDto context = contextService.getContext(userId);
            Double totalBalance = context.getTotalBalance();
            Double monthlyIncome = context.getMonthlyAvgIncome();
            Double monthlyExpense = context.getMonthlyAvgExpense();

            // Calculate investable amount (recommend investing 20-30% of available balance)
            Double investableAmount = calculateInvestableAmount(totalBalance, monthlyIncome, monthlyExpense);

            // Determine risk tolerance if not provided
            if (riskTolerance == null || riskTolerance.isBlank()) {
                riskTolerance = determineRiskTolerance(context);
            }

            // Get recommended investment options
            List<InvestmentProviderService.InvestmentOption> options = investmentProviderService.getRecommendedOptions(
                    investableAmount,
                    riskTolerance,
                    investmentType
            );

            // Limit to top 5 options
            options = options.stream().limit(5).toList();

            // Calculate recommended allocation
            InvestmentProviderService.InvestmentAllocation allocation = investmentProviderService.calculateAllocation(
                    investableAmount,
                    riskTolerance
            );

            // Build response data
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("type", "INVESTMENT_RECOMMENDATION");
            responseData.put("investableAmount", investableAmount);
            responseData.put("riskTolerance", riskTolerance);
            responseData.put("options", options.stream()
                    .map(o -> {
                        Map<String, Object> optionMap = new HashMap<>();
                        optionMap.put("name", o.getName());
                        optionMap.put("type", o.getType());
                        optionMap.put("riskLevel", o.getRiskLevel());
                        optionMap.put("minAmount", o.getMinAmount());
                        optionMap.put("maxAmount", o.getMaxAmount());
                        optionMap.put("expectedReturn", o.getExpectedReturn());
                        optionMap.put("termMonths", o.getTermMonths());
                        optionMap.put("description", o.getDescription());
                        optionMap.put("providers", o.getProviders());
                        optionMap.put("pros", o.getPros());
                        optionMap.put("cons", o.getCons());
                        return optionMap;
                    })
                    .toList());
            Map<String, Object> allocationMap = new HashMap<>();
            allocationMap.put("totalAmount", allocation.getTotalAmount());
            allocationMap.put("riskTolerance", allocation.getRiskTolerance());
            allocationMap.put("allocations", allocation.getAllocations().stream()
                    .map(a -> {
                        Map<String, Object> allocMap = new HashMap<>();
                        allocMap.put("type", a.getType());
                        allocMap.put("percentage", a.getPercentage());
                        allocMap.put("amount", a.getAmount());
                        return allocMap;
                    })
                    .toList());
            responseData.put("allocation", allocationMap);

            // Generate natural language response
            String naturalLanguageReply = generateNaturalLanguageResponse(
                    investableAmount,
                    riskTolerance,
                    investmentType,
                    options,
                    allocation,
                    financialContext
            );

            // Build suggestions
            List<String> suggestions = new ArrayList<>();
            suggestions.add("Xem các kênh đầu tư");
            suggestions.add("Tính lợi nhuận dự kiến");
            if (riskTolerance.equals("LOW") || riskTolerance.equals("CONSERVATIVE")) {
                suggestions.add("Khám phá các kênh rủi ro cao hơn");
            }

            return ChatResponse.builder()
                    .reply(naturalLanguageReply)
                    .suggestions(suggestions)
                    .data(responseData)
                    .build();

        } catch (Exception e) {
            log.error("Error in investment advisor agent for user {}: {}", userId, e.getMessage(), e);
            return ChatResponse.builder()
                    .reply("Xin lỗi, tôi gặp lỗi khi tư vấn đầu tư. Vui lòng thử lại sau.")
                    .suggestions(Collections.emptyList())
                    .data(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    private Double calculateInvestableAmount(
            Double totalBalance,
            Double monthlyIncome,
            Double monthlyExpense
    ) {
        if (totalBalance == null || totalBalance <= 0) {
            return 0.0;
        }

        // Keep 3 months of expenses as emergency fund
        double emergencyFund = (monthlyExpense != null ? monthlyExpense : 0) * 3;
        double availableForInvestment = Math.max(0, totalBalance - emergencyFund);

        // Recommend investing 50-70% of available amount
        return availableForInvestment * 0.6;
    }

    private String determineRiskTolerance(FinancialContextDto context) {
        Double savingsRate = context.getSavingsRate();
        Double monthlyIncome = context.getMonthlyAvgIncome();

        // Conservative if low savings rate or low income
        if (savingsRate == null || savingsRate < 0.1) {
            return "LOW";
        }
        if (monthlyIncome != null && monthlyIncome < 10_000_000) {
            return "LOW";
        }
        if (savingsRate >= 0.2) {
            return "MEDIUM";
        }

        return "MEDIUM"; // Default
    }

    private String generateNaturalLanguageResponse(
            Double investableAmount,
            String riskTolerance,
            String investmentType,
            List<InvestmentProviderService.InvestmentOption> options,
            InvestmentProviderService.InvestmentAllocation allocation,
            String financialContext
    ) {
        try {
            String optionsJson = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(options);
            String allocationJson = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(allocation);

            String prompt = String.format(
                    """
                    Bạn là một chuyên gia tư vấn đầu tư tài chính. Hãy tư vấn cho người dùng dựa trên thông tin dưới đây.
                    
                    Số tiền có thể đầu tư: %s VND
                    Mức độ chấp nhận rủi ro: %s
                    Loại đầu tư quan tâm: %s
                    
                    Các lựa chọn đầu tư được đề xuất:
                    %s
                    
                    Phân bổ đầu tư được khuyến nghị:
                    %s
                    
                    Bối cảnh tài chính người dùng:
                    %s
                    
                    Yêu cầu: tất cả đều ngắn gọn
                    1. Giải thích rõ ràng về khả năng đầu tư của người dùng
                    2. Giới thiệu 3-4 lựa chọn đầu tư phù hợp nhất với ưu/nhược điểm
                    3. Giải thích phân bổ đầu tư được khuyến nghị và lý do
                    4. So sánh lợi nhuận kỳ vọng và rủi ro giữa các lựa chọn
                    5. Đưa ra lời khuyên cụ thể dựa trên mức độ chấp nhận rủi ro
                    6. Cảnh báo về rủi ro và nguyên tắc đầu tư an toàn
                    7. Sử dụng tiếng Việt, giọng điệu thân thiện, chuyên nghiệp
                    8. Định dạng số tiền với dấu phẩy ngăn cách hàng nghìn (ví dụ: 10,000,000 VND)
                    9. Nhấn mạnh nguyên tắc "không bỏ tất cả trứng vào một giỏ"
                    
                    Trả lời:
                    """,
                    investableAmount != null ? String.format("%,.0f", investableAmount) : "0",
                    riskTolerance != null ? riskTolerance : "MEDIUM",
                    investmentType != null ? investmentType : "Tất cả các loại",
                    optionsJson,
                    allocationJson,
                    financialContext != null ? financialContext : "Không có bối cảnh"
            );

            return geminiClient.generateText(prompt);

        } catch (Exception e) {
            log.error("Error generating natural language response: {}", e.getMessage(), e);
            return buildFallbackResponse(options, allocation);
        }
    }

    private String buildFallbackResponse(
            List<InvestmentProviderService.InvestmentOption> options,
            InvestmentProviderService.InvestmentAllocation allocation
    ) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("## Các lựa chọn đầu tư được đề xuất\n\n");
        for (InvestmentProviderService.InvestmentOption o : options) {
            sb.append("### ").append(o.getName()).append("\n");
            sb.append("- Rủi ro: ").append(o.getRiskLevel()).append("\n");
            sb.append("- Lợi nhuận kỳ vọng: ").append(o.getExpectedReturn()).append("%/năm\n");
            sb.append("- Mô tả: ").append(o.getDescription()).append("\n\n");
        }
        
        sb.append("## Phân bổ đầu tư được khuyến nghị\n\n");
        for (InvestmentProviderService.AllocationItem item : allocation.getAllocations()) {
            sb.append("- ").append(item.getType()).append(": ")
                    .append(String.format("%.0f%%", item.getPercentage()))
                    .append(" (").append(String.format("%,.0f", item.getAmount())).append(" VND)\n");
        }
        
        return sb.toString();
    }
}

