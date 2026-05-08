package com.ptit.expensetracker.backend.ai.service.provider;

import lombok.Builder;
import lombok.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service that provides loan provider recommendations.
 * In a real implementation, this would integrate with external APIs
 * or maintain a database of loan providers.
 */
@Service
public class LoanProviderService {

    /**
     * Get recommended loan providers based on user's financial profile.
     */
    public List<LoanProvider> getRecommendedProviders(
            Double loanAmount,
            String purpose,
            Double monthlyIncome,
            Double creditScore
    ) {
        // Mock data - In production, this would query a database or external API
        List<LoanProvider> providers = List.of(
                LoanProvider.builder()
                        .name("Vietcombank")
                        .type("BANK")
                        .minAmount(10_000_000.0)
                        .maxAmount(500_000_000.0)
                        .interestRate(8.5)
                        .termMonths(12)
                        .eligibility("Cần chứng minh thu nhập từ 10 triệu/tháng")
                        .pros(List.of("Lãi suất thấp", "Uy tín cao", "Hỗ trợ tốt"))
                        .cons(List.of("Thủ tục phức tạp", "Thời gian xét duyệt lâu"))
                        .build(),
                
                LoanProvider.builder()
                        .name("Techcombank")
                        .type("BANK")
                        .minAmount(5_000_000.0)
                        .maxAmount(300_000_000.0)
                        .interestRate(9.0)
                        .termMonths(12)
                        .eligibility("Cần chứng minh thu nhập từ 8 triệu/tháng")
                        .pros(List.of("Xét duyệt nhanh", "Lãi suất cạnh tranh", "App tiện lợi"))
                        .cons(List.of("Yêu cầu tài sản thế chấp cho khoản vay lớn"))
                        .build(),
                
                LoanProvider.builder()
                        .name("VPBank")
                        .type("BANK")
                        .minAmount(3_000_000.0)
                        .maxAmount(200_000_000.0)
                        .interestRate(9.5)
                        .termMonths(6)
                        .eligibility("Cần chứng minh thu nhập từ 6 triệu/tháng")
                        .pros(List.of("Thủ tục đơn giản", "Xét duyệt online", "Linh hoạt"))
                        .cons(List.of("Lãi suất cao hơn một chút"))
                        .build(),
                
                LoanProvider.builder()
                        .name("FE Credit")
                        .type("FINANCE_COMPANY")
                        .minAmount(1_000_000.0)
                        .maxAmount(100_000_000.0)
                        .interestRate(18.0)
                        .termMonths(12)
                        .eligibility("Cần CMND/CCCD và chứng minh thu nhập")
                        .pros(List.of("Xét duyệt rất nhanh", "Thủ tục đơn giản", "Không cần tài sản thế chấp"))
                        .cons(List.of("Lãi suất cao", "Phí phát sinh nhiều"))
                        .build()
        );

        // Filter by loan amount if provided
        if (loanAmount != null) {
            providers = providers.stream()
                    .filter(p -> loanAmount >= p.getMinAmount() && loanAmount <= p.getMaxAmount())
                    .toList();
        }

        // Sort by interest rate (ascending)
        providers = providers.stream()
                .sorted((a, b) -> Double.compare(a.getInterestRate(), b.getInterestRate()))
                .toList();

        return providers;
    }

    /**
     * Check if user is eligible for a loan based on their financial profile.
     */
    public LoanEligibility checkEligibility(
            Double loanAmount,
            Double monthlyIncome,
            Double monthlyExpense,
            Double totalBalance
    ) {
        if (monthlyIncome == null || monthlyIncome <= 0) {
            return LoanEligibility.builder()
                    .eligible(false)
                    .reason("Không có thông tin thu nhập")
                    .recommendation("Vui lòng cập nhật thông tin thu nhập để được tư vấn chính xác hơn")
                    .build();
        }

        // Calculate debt-to-income ratio
        double availableIncome = monthlyIncome - (monthlyExpense != null ? monthlyExpense : 0);
        double monthlyPayment = loanAmount != null ? loanAmount / 12 : 0; // Simple calculation
        double debtToIncomeRatio = monthlyPayment / monthlyIncome;

        boolean eligible = availableIncome > monthlyPayment * 1.5; // At least 1.5x buffer
        String reason = eligible
                ? "Thu nhập đủ để trả nợ hàng tháng"
                : "Thu nhập không đủ để trả nợ hàng tháng một cách an toàn";

        String recommendation = eligible
                ? "Bạn có thể vay, nhưng nên vay số tiền phù hợp với khả năng trả nợ"
                : "Nên tăng thu nhập hoặc giảm chi tiêu trước khi vay, hoặc vay số tiền nhỏ hơn";

        return LoanEligibility.builder()
                .eligible(eligible)
                .reason(reason)
                .debtToIncomeRatio(debtToIncomeRatio)
                .recommendedMaxAmount(availableIncome * 12 * 0.7) // 70% of annual available income
                .recommendation(recommendation)
                .build();
    }

    @Value
    @Builder
    public static class LoanProvider {
        String name;
        String type; // BANK, FINANCE_COMPANY, P2P
        Double minAmount;
        Double maxAmount;
        Double interestRate; // Annual percentage rate
        Integer termMonths;
        String eligibility;
        List<String> pros;
        List<String> cons;
    }

    @Value
    @Builder
    public static class LoanEligibility {
        Boolean eligible;
        String reason;
        Double debtToIncomeRatio;
        Double recommendedMaxAmount;
        String recommendation;
    }
}

