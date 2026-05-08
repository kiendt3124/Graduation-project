package com.ptit.expensetracker.backend.ai.service.analytics;

import com.ptit.expensetracker.backend.ai.dto.context.FinancialContextDto;
import com.ptit.expensetracker.backend.ai.dto.context.MonthlyTotalDto;
import com.ptit.expensetracker.backend.ai.dto.context.TransactionDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Backend analytics engine that performs various financial analyses
 * based on the user's financial context.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BackendAnalyticsEngine {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * Analyze spending by category over a time range.
     */
    public Map<String, Object> analyzeCategorySpending(
            FinancialContextDto context,
            String timeRange
    ) {
        List<TransactionDto> transactions = filterTransactionsByTimeRange(
                context.getRecentTransactions(), timeRange
        );

        Map<String, Double> categoryTotals = transactions.stream()
                .filter(tx -> "OUTFLOW".equals(tx.getType()))
                .collect(Collectors.groupingBy(
                        TransactionDto::getCategoryMetadata,
                        Collectors.summingDouble(TransactionDto::getAmount)
                ));

        List<Map<String, Object>> topCategories = categoryTotals.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(10)
                .map(e -> Map.<String, Object>of(
                        "category", e.getKey(),
                        "amount", e.getValue(),
                        "percentage", calculatePercentage(e.getValue(), categoryTotals.values().stream().mapToDouble(Double::doubleValue).sum())
                ))
                .collect(Collectors.toList());

        double totalSpending = categoryTotals.values().stream().mapToDouble(Double::doubleValue).sum();

        return Map.of(
                "type", "CATEGORY_SPENDING",
                "timeRange", timeRange,
                "totalSpending", totalSpending,
                "topCategories", topCategories,
                "categoryCount", categoryTotals.size()
        );
    }

    /**
     * Compare spending across multiple months.
     */
    public Map<String, Object> analyzeMonthlyComparison(
            FinancialContextDto context,
            int monthCount
    ) {
        List<MonthlyTotalDto> monthlyTotals = context.getMonthlyTotals();
        if (monthlyTotals == null || monthlyTotals.isEmpty()) {
            return Map.of(
                    "type", "MONTHLY_COMPARISON",
                    "error", "Insufficient monthly data"
            );
        }

        List<MonthlyTotalDto> recentMonths = monthlyTotals.stream()
                .sorted(Comparator.comparing(MonthlyTotalDto::getMonth).reversed())
                .limit(monthCount)
                .collect(Collectors.toList());

        Collections.reverse(recentMonths); // oldest -> newest

        List<Map<String, Object>> monthlyData = recentMonths.stream()
                .map(m -> Map.<String, Object>of(
                        "month", m.getMonth(),
                        "income", m.getIncome() != null ? m.getIncome() : 0.0,
                        "expense", m.getExpense() != null ? m.getExpense() : 0.0,
                        "savings", (m.getIncome() != null ? m.getIncome() : 0.0) - (m.getExpense() != null ? m.getExpense() : 0.0)
                ))
                .collect(Collectors.toList());

        // Calculate trends
        if (recentMonths.size() >= 2) {
            MonthlyTotalDto latest = recentMonths.get(recentMonths.size() - 1);
            MonthlyTotalDto previous = recentMonths.get(recentMonths.size() - 2);

            double incomeChange = calculatePercentageChange(
                    previous.getIncome() != null ? previous.getIncome() : 0.0,
                    latest.getIncome() != null ? latest.getIncome() : 0.0
            );
            double expenseChange = calculatePercentageChange(
                    previous.getExpense() != null ? previous.getExpense() : 0.0,
                    latest.getExpense() != null ? latest.getExpense() : 0.0
            );

            return Map.of(
                    "type", "MONTHLY_COMPARISON",
                    "monthCount", monthCount,
                    "monthlyData", monthlyData,
                    "incomeChangePercent", incomeChange,
                    "expenseChangePercent", expenseChange,
                    "trend", determineTrend(incomeChange, expenseChange)
            );
        }

        return Map.of(
                "type", "MONTHLY_COMPARISON",
                "monthCount", monthCount,
                "monthlyData", monthlyData
        );
    }

    /**
     * Analyze spending trends (increasing/decreasing).
     */
    public Map<String, Object> analyzeSpendingTrend(
            FinancialContextDto context,
            String timeRange
    ) {
        List<MonthlyTotalDto> monthlyTotals = context.getMonthlyTotals();
        if (monthlyTotals == null || monthlyTotals.size() < 2) {
            return Map.of(
                    "type", "SPENDING_TREND",
                    "error", "Insufficient data for trend analysis"
            );
        }

        List<MonthlyTotalDto> sorted = monthlyTotals.stream()
                .sorted(Comparator.comparing(MonthlyTotalDto::getMonth))
                .collect(Collectors.toList());

        List<Double> expenses = sorted.stream()
                .map(m -> m.getExpense() != null ? m.getExpense() : 0.0)
                .collect(Collectors.toList());

        String trend = "STABLE";
        double avgChange = 0.0;

        if (expenses.size() >= 2) {
            List<Double> changes = new ArrayList<>();
            for (int i = 1; i < expenses.size(); i++) {
                double change = calculatePercentageChange(expenses.get(i - 1), expenses.get(i));
                changes.add(change);
            }
            avgChange = changes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

            if (avgChange > 5.0) {
                trend = "INCREASING";
            } else if (avgChange < -5.0) {
                trend = "DECREASING";
            }
        }

        return Map.of(
                "type", "SPENDING_TREND",
                "timeRange", timeRange,
                "trend", trend,
                "averageChangePercent", avgChange,
                "monthlyExpenses", expenses
        );
    }

    /**
     * Analyze budget vs actual spending.
     */
    public Map<String, Object> analyzeBudgetVsActual(
            FinancialContextDto context,
            String timeRange
    ) {
        // Note: Budget data is not in FinancialContextDto yet
        // This is a placeholder for future enhancement
        List<TransactionDto> transactions = filterTransactionsByTimeRange(
                context.getRecentTransactions(), timeRange
        );

        double totalExpense = transactions.stream()
                .filter(tx -> "OUTFLOW".equals(tx.getType()))
                .mapToDouble(TransactionDto::getAmount)
                .sum();

        return Map.of(
                "type", "BUDGET_VS_ACTUAL",
                "timeRange", timeRange,
                "actualExpense", totalExpense,
                "note", "Budget data not available in context. This analysis will be enhanced when budget sync is implemented."
        );
    }

    /**
     * Analyze income vs expense ratio and savings potential.
     */
    public Map<String, Object> analyzeSavingsPotential(FinancialContextDto context) {
        Double monthlyIncome = context.getMonthlyAvgIncome();
        Double monthlyExpense = context.getMonthlyAvgExpense();
        Double savingsRate = context.getSavingsRate();

        if (monthlyIncome == null || monthlyIncome == 0) {
            return Map.of(
                    "type", "SAVINGS_POTENTIAL",
                    "error", "Insufficient income data"
            );
        }

        double currentSavings = monthlyExpense != null ? monthlyIncome - monthlyExpense : monthlyIncome;
        double potentialSavings = monthlyIncome * 0.2; // 20% savings goal
        double savingsGap = potentialSavings - currentSavings;

        String recommendation = "GOOD";
        if (savingsRate != null) {
            if (savingsRate < 0.1) {
                recommendation = "LOW";
            } else if (savingsRate < 0.2) {
                recommendation = "MODERATE";
            }
        }

        return Map.of(
                "type", "SAVINGS_POTENTIAL",
                "monthlyIncome", monthlyIncome,
                "monthlyExpense", monthlyExpense != null ? monthlyExpense : 0.0,
                "currentSavings", currentSavings,
                "savingsRate", savingsRate != null ? savingsRate : 0.0,
                "potentialSavings", potentialSavings,
                "savingsGap", savingsGap,
                "recommendation", recommendation
        );
    }

    /**
     * Analyze top spending days/patterns.
     */
    public Map<String, Object> analyzeSpendingPatterns(
            FinancialContextDto context,
            String timeRange
    ) {
        List<TransactionDto> transactions = filterTransactionsByTimeRange(
                context.getRecentTransactions(), timeRange
        );

        Map<String, Double> dailySpending = transactions.stream()
                .filter(tx -> "OUTFLOW".equals(tx.getType()))
                .collect(Collectors.groupingBy(
                        TransactionDto::getDate,
                        Collectors.summingDouble(TransactionDto::getAmount)
                ));

        List<Map<String, Object>> topDays = dailySpending.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5)
                .map(e -> Map.<String, Object>of(
                        "date", e.getKey(),
                        "amount", e.getValue()
                ))
                .collect(Collectors.toList());

        double avgDailySpending = dailySpending.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        return Map.of(
                "type", "SPENDING_PATTERNS",
                "timeRange", timeRange,
                "topDays", topDays,
                "averageDailySpending", avgDailySpending,
                "totalDays", dailySpending.size()
        );
    }

    // Helper methods

    private List<TransactionDto> filterTransactionsByTimeRange(
            List<TransactionDto> transactions,
            String timeRange
    ) {
        if (transactions == null) {
            return Collections.emptyList();
        }

        LocalDate now = LocalDate.now();
        LocalDate startDate;

        switch (timeRange.toUpperCase()) {
            case "THIS_WEEK":
                startDate = now.minusWeeks(1);
                break;
            case "THIS_MONTH":
                startDate = now.withDayOfMonth(1);
                break;
            case "LAST_MONTH":
                startDate = now.minusMonths(1).withDayOfMonth(1);
                LocalDate endDate = now.withDayOfMonth(1).minusDays(1);
                return transactions.stream()
                        .filter(tx -> isDateInRange(tx.getDate(), startDate, endDate))
                        .collect(Collectors.toList());
            case "LAST_3_MONTHS":
                startDate = now.minusMonths(3);
                break;
            case "LAST_6_MONTHS":
                startDate = now.minusMonths(6);
                break;
            case "THIS_YEAR":
                startDate = now.withDayOfYear(1);
                break;
            default:
                // Default to last 30 days
                startDate = now.minusDays(30);
        }

        final LocalDate finalStartDate = startDate;
        return transactions.stream()
                .filter(tx -> {
                    try {
                        LocalDate txDate = LocalDate.parse(tx.getDate());
                        return !txDate.isBefore(finalStartDate) && !txDate.isAfter(now);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }

    private boolean isDateInRange(String dateStr, LocalDate start, LocalDate end) {
        try {
            LocalDate date = LocalDate.parse(dateStr);
            return !date.isBefore(start) && !date.isAfter(end);
        } catch (Exception e) {
            return false;
        }
    }

    private double calculatePercentage(double value, double total) {
        if (total == 0) return 0.0;
        return (value / total) * 100.0;
    }

    private double calculatePercentageChange(double oldValue, double newValue) {
        if (oldValue == 0) return newValue > 0 ? 100.0 : 0.0;
        return ((newValue - oldValue) / oldValue) * 100.0;
    }

    private String determineTrend(double incomeChange, double expenseChange) {
        if (incomeChange > expenseChange) {
            return "IMPROVING";
        } else if (expenseChange > incomeChange) {
            return "WORSENING";
        } else {
            return "STABLE";
        }
    }
}

