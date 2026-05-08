package com.ptit.expensetracker.backend.ai.dto.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialContextDto {
    private List<WalletDto> wallets;
    private List<TransactionDto> recentTransactions;
    private Map<String, Double> categorySpending;
    private List<MonthlyTotalDto> monthlyTotals;
    private Map<String, Object> analyticsCache;

    private Double totalBalance;
    private Double monthlyAvgIncome;
    private Double monthlyAvgExpense;
    private Double savingsRate;
}

