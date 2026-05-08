package com.ptit.expensetracker.backend.ai.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InsightsRequest {

    /**
     * Aggregated income over a specific time range.
     */
    @NotNull(message = "totalIncome must not be null")
    private Double totalIncome;

    /**
     * Aggregated expense over a specific time range.
     */
    @NotNull(message = "totalExpense must not be null")
    private Double totalExpense;

    /**
     * Total outstanding debt amount.
     */
    @NotNull(message = "totalDebt must not be null")
    private Double totalDebt;

    /**
     * Optional textual description of recent spending pattern
     * generated on the client side (e.g. weekly report summary).
     */
    private String recentSpendingPattern;

    /**
     * Optional time range identifier, e.g. "LAST_30_DAYS".
     */
    private String timeRange;
}



