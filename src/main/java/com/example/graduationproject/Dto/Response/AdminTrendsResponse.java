package com.example.graduationproject.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response cho GET /api/admin/stats/trends
 * Chứa dữ liệu xu hướng tài chính ẩn danh toàn platform.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminTrendsResponse {

    // ─── Tần suất sử dụng tính năng ──────────────────────────────────────────
    private FeatureUsageStat featureUsage;

    // ─── Tăng trưởng user theo tháng trong năm ───────────────────────────────
    private int year;
    private List<MonthlyNewUserStat> newUsersByMonth;

    // ─── Xu hướng tài chính ẩn danh tháng hiện tại ───────────────────────────
    private PlatformFinancialStat currentMonthFinancial;

    // ─── Top 5 danh mục chi tiêu toàn platform ───────────────────────────────
    private List<TopCategoryStat> topExpenseCategories;

    // ─── Tổng tài sản hệ thống ───────────────────────────────────────────────
    private BigDecimal totalMoneyInSystem;

    // =========================================================================
    // Nested classes
    // =========================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeatureUsageStat {
        private long totalTransactions;   // Tổng giao dịch toàn hệ thống
        private long totalWallets;        // Tổng số ví
        private long totalBudgets;        // Tổng số ngân sách
        private long totalLoans;          // Tổng khoản vay/cho vay
        private long totalGoals;          // Tổng mục tiêu tài chính
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyNewUserStat {
        private int month;     // 1-12
        private int year;
        private long count;    // Số user đăng ký mới trong tháng đó
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlatformFinancialStat {
        private int month;
        private int year;
        private BigDecimal totalIncome;    // Tổng thu toàn platform trong tháng
        private BigDecimal totalExpense;   // Tổng chi toàn platform trong tháng
        private BigDecimal netCashFlow;    // Income - Expense
        private BigDecimal avgIncomePerUser;   // Thu nhập TB/user active
        private BigDecimal avgExpensePerUser;  // Chi tiêu TB/user active
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopCategoryStat {
        private String categoryName;
        private BigDecimal totalAmount;       // Tổng tiền chi vào danh mục này
        private long transactionCount;        // Số giao dịch
    }
}
