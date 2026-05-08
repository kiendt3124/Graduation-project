package com.ptit.expensetracker.backend.ai.service.provider;

import lombok.Builder;
import lombok.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service that provides investment recommendations.
 * In a real implementation, this would integrate with external APIs
 * or maintain a database of investment options.
 */
@Service
public class InvestmentProviderService {

    /**
     * Get recommended investment options based on user's profile.
     */
    public List<InvestmentOption> getRecommendedOptions(
            Double investableAmount,
            String riskTolerance,
            String investmentType
    ) {
        // Mock data - In production, this would query a database or external API
        List<InvestmentOption> options = List.of(
                // Low risk options
                InvestmentOption.builder()
                        .name("Gửi tiết kiệm ngân hàng")
                        .type("SAVINGS")
                        .riskLevel("LOW")
                        .minAmount(1_000_000.0)
                        .maxAmount(1_000_000_000_000.0)
                        .expectedReturn(6.5) // Annual percentage
                        .termMonths(12)
                        .description("An toàn, lãi suất ổn định, thanh khoản tốt")
                        .providers(List.of("Vietcombank", "Techcombank", "VPBank"))
                        .pros(List.of("An toàn tuyệt đối", "Lãi suất ổn định", "Rút tiền linh hoạt"))
                        .cons(List.of("Lãi suất thấp", "Không chống lạm phát tốt"))
                        .build(),
                
                InvestmentOption.builder()
                        .name("Trái phiếu Chính phủ")
                        .type("BOND")
                        .riskLevel("LOW")
                        .minAmount(1_000_000.0)
                        .maxAmount(100_000_000_000.0)
                        .expectedReturn(7.5)
                        .termMonths(36)
                        .description("Rủi ro thấp, lãi suất cao hơn tiết kiệm")
                        .providers(List.of("SSI", "VNDirect", "HSC"))
                        .pros(List.of("Rủi ro thấp", "Lãi suất tốt", "Được Chính phủ bảo lãnh"))
                        .cons(List.of("Kỳ hạn dài", "Thanh khoản kém hơn tiết kiệm"))
                        .build(),
                
                // Medium risk options
                InvestmentOption.builder()
                        .name("Quỹ đầu tư cân bằng")
                        .type("MUTUAL_FUND")
                        .riskLevel("MEDIUM")
                        .minAmount(1_000_000.0)
                        .maxAmount(1_000_000_000.0)
                        .expectedReturn(10.0)
                        .termMonths(12)
                        .description("Đa dạng hóa đầu tư, quản lý chuyên nghiệp")
                        .providers(List.of("VFM", "SSI AM", "Dragon Capital"))
                        .pros(List.of("Đa dạng hóa", "Quản lý chuyên nghiệp", "Linh hoạt"))
                        .cons(List.of("Có phí quản lý", "Rủi ro trung bình"))
                        .build(),
                
                InvestmentOption.builder()
                        .name("Chứng chỉ quỹ ETF")
                        .type("ETF")
                        .riskLevel("MEDIUM")
                        .minAmount(100_000.0)
                        .maxAmount(1_000_000_000.0)
                        .expectedReturn(12.0)
                        .termMonths(0) // Flexible
                        .description("Đầu tư vào chỉ số thị trường, thanh khoản tốt")
                        .providers(List.of("SSI", "VNDirect", "HSC"))
                        .pros(List.of("Chi phí thấp", "Thanh khoản tốt", "Đa dạng hóa"))
                        .cons(List.of("Rủi ro trung bình", "Phụ thuộc thị trường"))
                        .build(),
                
                // High risk options
                InvestmentOption.builder()
                        .name("Cổ phiếu")
                        .type("STOCK")
                        .riskLevel("HIGH")
                        .minAmount(100_000.0)
                        .maxAmount(1_000_000_000_000.0)
                        .expectedReturn(15.0)
                        .termMonths(0) // Flexible
                        .description("Tiềm năng lợi nhuận cao nhưng rủi ro lớn")
                        .providers(List.of("SSI", "VNDirect", "HSC", "VCBS"))
                        .pros(List.of("Tiềm năng lợi nhuận cao", "Thanh khoản tốt", "Kiểm soát trực tiếp"))
                        .cons(List.of("Rủi ro cao", "Cần kiến thức", "Biến động lớn"))
                        .build(),
                
                InvestmentOption.builder()
                        .name("Bất động sản")
                        .type("REAL_ESTATE")
                        .riskLevel("MEDIUM_HIGH")
                        .minAmount(100_000_000.0)
                        .maxAmount(10_000_000_000_000.0)
                        .expectedReturn(8.0)
                        .termMonths(60)
                        .description("Đầu tư dài hạn, giá trị tăng trưởng ổn định")
                        .providers(List.of("Các công ty BĐS uy tín"))
                        .pros(List.of("Giá trị tăng trưởng", "Có thể cho thuê", "Tài sản thực"))
                        .cons(List.of("Vốn lớn", "Thanh khoản kém", "Cần quản lý"))
                        .build()
        );

        // Filter by risk tolerance
        if (riskTolerance != null) {
            String risk = riskTolerance.toUpperCase();
            if ("LOW".equals(risk) || "CONSERVATIVE".equals(risk)) {
                options = options.stream()
                        .filter(o -> "LOW".equals(o.getRiskLevel()))
                        .toList();
            } else if ("MEDIUM".equals(risk) || "MODERATE".equals(risk)) {
                options = options.stream()
                        .filter(o -> "LOW".equals(o.getRiskLevel()) || "MEDIUM".equals(o.getRiskLevel()))
                        .toList();
            }
            // HIGH risk tolerance accepts all options
        }

        // Filter by investment type if provided
        if (investmentType != null && !investmentType.isBlank()) {
            String type = investmentType.toUpperCase();
            options = options.stream()
                    .filter(o -> o.getType().equals(type))
                    .toList();
        }

        // Filter by amount if provided
        if (investableAmount != null) {
            options = options.stream()
                    .filter(o -> investableAmount >= o.getMinAmount() && investableAmount <= o.getMaxAmount())
                    .toList();
        }

        // Sort by expected return (descending)
        options = options.stream()
                .sorted((a, b) -> Double.compare(b.getExpectedReturn(), a.getExpectedReturn()))
                .toList();

        return options;
    }

    /**
     * Calculate recommended investment allocation based on risk tolerance.
     */
    public InvestmentAllocation calculateAllocation(
            Double totalAmount,
            String riskTolerance
    ) {
        if (totalAmount == null || totalAmount <= 0) {
            return InvestmentAllocation.builder()
                    .totalAmount(0.0)
                    .allocations(List.of())
                    .build();
        }

        String risk = riskTolerance != null ? riskTolerance.toUpperCase() : "MEDIUM";

        List<AllocationItem> allocations = switch (risk) {
            case "LOW", "CONSERVATIVE" -> List.of(
                    AllocationItem.builder()
                            .type("SAVINGS")
                            .percentage(60.0)
                            .amount(totalAmount * 0.6)
                            .build(),
                    AllocationItem.builder()
                            .type("BOND")
                            .percentage(40.0)
                            .amount(totalAmount * 0.4)
                            .build()
            );
            case "MEDIUM", "MODERATE" -> List.of(
                    AllocationItem.builder()
                            .type("SAVINGS")
                            .percentage(30.0)
                            .amount(totalAmount * 0.3)
                            .build(),
                    AllocationItem.builder()
                            .type("MUTUAL_FUND")
                            .percentage(50.0)
                            .amount(totalAmount * 0.5)
                            .build(),
                    AllocationItem.builder()
                            .type("STOCK")
                            .percentage(20.0)
                            .amount(totalAmount * 0.2)
                            .build()
            );
            case "HIGH", "AGGRESSIVE" -> List.of(
                    AllocationItem.builder()
                            .type("SAVINGS")
                            .percentage(10.0)
                            .amount(totalAmount * 0.1)
                            .build(),
                    AllocationItem.builder()
                            .type("MUTUAL_FUND")
                            .percentage(30.0)
                            .amount(totalAmount * 0.3)
                            .build(),
                    AllocationItem.builder()
                            .type("STOCK")
                            .percentage(60.0)
                            .amount(totalAmount * 0.6)
                            .build()
            );
            default -> List.of(
                    AllocationItem.builder()
                            .type("SAVINGS")
                            .percentage(40.0)
                            .amount(totalAmount * 0.4)
                            .build(),
                    AllocationItem.builder()
                            .type("MUTUAL_FUND")
                            .percentage(40.0)
                            .amount(totalAmount * 0.4)
                            .build(),
                    AllocationItem.builder()
                            .type("STOCK")
                            .percentage(20.0)
                            .amount(totalAmount * 0.2)
                            .build()
            );
        };

        return InvestmentAllocation.builder()
                .totalAmount(totalAmount)
                .riskTolerance(risk)
                .allocations(allocations)
                .build();
    }

    @Value
    @Builder
    public static class InvestmentOption {
        String name;
        String type; // SAVINGS, BOND, MUTUAL_FUND, ETF, STOCK, REAL_ESTATE
        String riskLevel; // LOW, MEDIUM, HIGH, MEDIUM_HIGH
        Double minAmount;
        Double maxAmount;
        Double expectedReturn; // Annual percentage
        Integer termMonths;
        String description;
        List<String> providers;
        List<String> pros;
        List<String> cons;
    }

    @Value
    @Builder
    public static class InvestmentAllocation {
        Double totalAmount;
        String riskTolerance;
        List<AllocationItem> allocations;
    }

    @Value
    @Builder
    public static class AllocationItem {
        String type;
        Double percentage;
        Double amount;
    }
}

