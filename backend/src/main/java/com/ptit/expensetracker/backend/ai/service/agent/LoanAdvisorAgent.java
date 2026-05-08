package com.ptit.expensetracker.backend.ai.service.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptit.expensetracker.backend.ai.dto.ChatResponse;
import com.ptit.expensetracker.backend.ai.dto.context.FinancialContextDto;
import com.ptit.expensetracker.backend.ai.gemini.GeminiClient;
import com.ptit.expensetracker.backend.ai.service.UserContextService;
import com.ptit.expensetracker.backend.ai.service.provider.LoanProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Provides loan recommendations based on user's financial situation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoanAdvisorAgent {

    private final LoanProviderService loanProviderService;
    private final UserContextService contextService;
    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    public ChatResponse recommend(
            String userId,
            Double loanAmount,
            String purpose,
            String financialContext
    ) {
        try {
            // Get financial context
            FinancialContextDto context = contextService.getContext(userId);
            Double monthlyIncome = context.getMonthlyAvgIncome();
            Double monthlyExpense = context.getMonthlyAvgExpense();
            Double totalBalance = context.getTotalBalance();

            // Check eligibility
            LoanProviderService.LoanEligibility eligibility = loanProviderService.checkEligibility(
                    loanAmount,
                    monthlyIncome,
                    monthlyExpense,
                    totalBalance
            );

            // Get recommended providers
            List<LoanProviderService.LoanProvider> providers = loanProviderService.getRecommendedProviders(
                    loanAmount,
                    purpose,
                    monthlyIncome,
                    null // Credit score not available
            );

            // Limit to top 3 providers
            providers = providers.stream().limit(3).toList();

            // Build response data
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("type", "LOAN_RECOMMENDATION");
            responseData.put("eligibility", Map.of(
                    "eligible", eligibility.getEligible(),
                    "reason", eligibility.getReason(),
                    "debtToIncomeRatio", eligibility.getDebtToIncomeRatio() != null 
                            ? eligibility.getDebtToIncomeRatio() : 0.0,
                    "recommendedMaxAmount", eligibility.getRecommendedMaxAmount() != null 
                            ? eligibility.getRecommendedMaxAmount() : 0.0,
                    "recommendation", eligibility.getRecommendation()
            ));
            responseData.put("providers", providers.stream()
                    .map(p -> Map.of(
                            "name", p.getName(),
                            "type", p.getType(),
                            "minAmount", p.getMinAmount(),
                            "maxAmount", p.getMaxAmount(),
                            "interestRate", p.getInterestRate(),
                            "termMonths", p.getTermMonths(),
                            "eligibility", p.getEligibility(),
                            "pros", p.getPros(),
                            "cons", p.getCons()
                    ))
                    .toList());
            responseData.put("loanAmount", loanAmount);
            responseData.put("purpose", purpose);

            // Generate natural language response
            String naturalLanguageReply = generateNaturalLanguageResponse(
                    loanAmount,
                    purpose,
                    eligibility,
                    providers,
                    financialContext
            );

            // Build suggestions
            List<String> suggestions = new ArrayList<>();
            if (eligibility.getEligible()) {
                suggestions.add("Xem danh sách ngân hàng");
                suggestions.add("Tính toán khoản vay");
            } else {
                suggestions.add("Cải thiện sức khỏe tài chính");
                suggestions.add("Giảm chi phí");
            }

            return ChatResponse.builder()
                    .reply(naturalLanguageReply)
                    .suggestions(suggestions)
                    .data(responseData)
                    .build();

        } catch (Exception e) {
            log.error("Error in loan advisor agent for user {}: {}", userId, e.getMessage(), e);
            return ChatResponse.builder()
                    .reply("Xin lỗi, tôi gặp lỗi khi tư vấn vay vốn. Vui lòng thử lại sau.")
                    .suggestions(Collections.emptyList())
                    .data(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    private String generateNaturalLanguageResponse(
            Double loanAmount,
            String purpose,
            LoanProviderService.LoanEligibility eligibility,
            List<LoanProviderService.LoanProvider> providers,
            String financialContext
    ) {
        try {
            String eligibilityJson = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(eligibility);
            String providersJson = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(providers);

            String prompt = String.format(
                    """
                    Bạn là một chuyên gia tư vấn tài chính chuyên về vay vốn. Hãy tư vấn cho người dùng dựa trên thông tin dưới đây.
                    
                    Số tiền muốn vay: %s
                    Mục đích vay: %s
                    
                    Kết quả đánh giá khả năng vay:
                    %s
                    
                    Danh sách các nhà cung cấp vay được đề xuất:
                    %s
                    
                    Bối cảnh tài chính người dùng:
                    %s
                    
                    Yêu cầu: tất cả đều ngắn gọn, súc tích
                    1. Giải thích rõ ràng về khả năng vay của người dùng
                    2. Nếu đủ điều kiện: Giới thiệu 2-3 nhà cung cấp tốt nhất với ưu/nhược điểm
                    3. Nếu không đủ điều kiện: Giải thích lý do và đưa ra lời khuyên cụ thể
                    4. So sánh lãi suất và điều kiện giữa các nhà cung cấp
                    5. Đưa ra khuyến nghị cụ thể dựa trên mục đích vay
                    6. Sử dụng tiếng Việt, giọng điệu thân thiện, chuyên nghiệp
                    7. Định dạng số tiền với dấu phẩy ngăn cách hàng nghìn (ví dụ: 10,000,000 VND)
                    8. Đưa ra cảnh báo về rủi ro nếu cần
                    
                    Trả lời:
                    """,
                    loanAmount != null ? String.format("%,.0f", loanAmount) + " VND" : "Chưa xác định",
                    purpose != null ? purpose : "Chưa xác định",
                    eligibilityJson,
                    providersJson,
                    financialContext != null ? financialContext : "Không có bối cảnh"
            );

            return geminiClient.generateText(prompt);

        } catch (Exception e) {
            log.error("Error generating natural language response: {}", e.getMessage(), e);
            return buildFallbackResponse(eligibility, providers);
        }
    }

    private String buildFallbackResponse(
            LoanProviderService.LoanEligibility eligibility,
            List<LoanProviderService.LoanProvider> providers
    ) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("## Đánh giá khả năng vay\n\n");
        if (eligibility.getEligible()) {
            sb.append("✅ ").append(eligibility.getReason()).append("\n\n");
        } else {
            sb.append("❌ ").append(eligibility.getReason()).append("\n\n");
        }
        
        sb.append(eligibility.getRecommendation()).append("\n\n");
        
        if (!providers.isEmpty()) {
            sb.append("## Các nhà cung cấp được đề xuất\n\n");
            for (LoanProviderService.LoanProvider p : providers) {
                sb.append("### ").append(p.getName()).append("\n");
                sb.append("- Lãi suất: ").append(p.getInterestRate()).append("%/năm\n");
                sb.append("- Kỳ hạn: ").append(p.getTermMonths()).append(" tháng\n");
                sb.append("- Điều kiện: ").append(p.getEligibility()).append("\n\n");
            }
        }
        
        return sb.toString();
    }
}

