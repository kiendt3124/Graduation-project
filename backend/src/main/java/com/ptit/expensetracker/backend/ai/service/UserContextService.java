package com.ptit.expensetracker.backend.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptit.expensetracker.backend.ai.domain.UserFinancialContext;
import com.ptit.expensetracker.backend.ai.dto.context.FinancialContextDto;
import com.ptit.expensetracker.backend.ai.dto.context.MonthlyTotalDto;
import com.ptit.expensetracker.backend.ai.dto.context.TransactionDto;
import com.ptit.expensetracker.backend.ai.dto.context.WalletDto;
import com.ptit.expensetracker.backend.ai.repository.UserFinancialContextRepository;
import com.ptit.expensetracker.backend.common.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserContextService {

    private final UserFinancialContextRepository repository;
    private final ObjectMapper objectMapper;
    private static final String EMPTY_OBJECT = "{}";
    private static final String EMPTY_ARRAY = "[]";

    @Transactional
    public void saveContext(String userId, FinancialContextDto dto) {
        UserFinancialContext context = repository.findByUserId(userId)
                .orElseGet(() -> UserFinancialContext.builder()
                        .userId(userId)
                        .createdAt(OffsetDateTime.now())
                        .build());

        try {
            context.setWalletsJson(objectMapper.writeValueAsString(
                    dto.getWallets() == null ? Collections.emptyList() : dto.getWallets()));
            context.setRecentTransactionsJson(objectMapper.writeValueAsString(
                    dto.getRecentTransactions() == null ? Collections.emptyList() : dto.getRecentTransactions()));
            context.setCategorySpendingJson(objectMapper.writeValueAsString(
                    dto.getCategorySpending() == null ? Collections.emptyMap() : dto.getCategorySpending()));
            context.setMonthlyTotalsJson(objectMapper.writeValueAsString(
                    dto.getMonthlyTotals() == null ? Collections.emptyList() : dto.getMonthlyTotals()));
            context.setAnalyticsCacheJson(objectMapper.writeValueAsString(
                    dto.getAnalyticsCache() == null ? Collections.emptyMap() : dto.getAnalyticsCache()));
        } catch (JsonProcessingException e) {
            throw new ApiException("Failed to serialize financial context", "CONTEXT_SERIALIZE_ERROR");
        }

        context.setTotalBalance(dto.getTotalBalance());
        context.setMonthlyAvgIncome(dto.getMonthlyAvgIncome());
        context.setMonthlyAvgExpense(dto.getMonthlyAvgExpense());
        context.setSavingsRate(dto.getSavingsRate());

        context.setLastSyncedAt(OffsetDateTime.now());
        context.setUpdatedAt(OffsetDateTime.now());

        repository.save(context);
    }

    @Transactional(readOnly = true)
    public FinancialContextDto getContext(String userId) {
        UserFinancialContext ctx = repository.findByUserId(userId)
                .orElse(null);
        
        if (ctx == null) {
            // Return empty context instead of throwing exception
            return FinancialContextDto.builder()
                    .wallets(Collections.emptyList())
                    .recentTransactions(Collections.emptyList())
                    .categorySpending(Collections.emptyMap())
                    .monthlyTotals(Collections.emptyList())
                    .analyticsCache(Collections.emptyMap())
                    .totalBalance(0.0)
                    .monthlyAvgIncome(0.0)
                    .monthlyAvgExpense(0.0)
                    .savingsRate(0.0)
                    .build();
        }
        
        try {
            return FinancialContextDto.builder()
                    .wallets(readList(ctx.getWalletsJson(), WalletDto.class))
                    .recentTransactions(readList(ctx.getRecentTransactionsJson(), TransactionDto.class))
                    .categorySpending(readMap(ctx.getCategorySpendingJson(), String.class, Double.class))
                    .monthlyTotals(readList(ctx.getMonthlyTotalsJson(), MonthlyTotalDto.class))
                    .analyticsCache(objectMapper.readValue(
                            ctx.getAnalyticsCacheJson() == null ? EMPTY_OBJECT : ctx.getAnalyticsCacheJson(),
                            objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class)
                    ))
                    .totalBalance(ctx.getTotalBalance())
                    .monthlyAvgIncome(ctx.getMonthlyAvgIncome())
                    .monthlyAvgExpense(ctx.getMonthlyAvgExpense())
                    .savingsRate(ctx.getSavingsRate())
                    .build();
        } catch (Exception e) {
            throw new ApiException("Failed to deserialize financial context", "CONTEXT_DESERIALIZE_ERROR");
        }
    }

    private <T> List<T> readList(String json, Class<T> clazz) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(
                    json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, clazz)
            );
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private <K, V> Map<K, V> readMap(String json, Class<K> keyClass, Class<V> valueClass) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(
                    json,
                    objectMapper.getTypeFactory().constructMapType(Map.class, keyClass, valueClass)
            );
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    /**
     * Build enriched context string for AI prompts.
     */
    @Transactional(readOnly = true)
    public String buildEnrichedContext(String userId) {
        FinancialContextDto ctx = getContext(userId);
        
        StringBuilder sb = new StringBuilder();
        sb.append("# User Financial Profile\n\n");
        
        // Basic info
        sb.append("**Total Balance:** ").append(formatMoney(ctx.getTotalBalance())).append(" VND\n");
        sb.append("**Monthly Income (avg):** ").append(formatMoney(ctx.getMonthlyAvgIncome())).append(" VND\n");
        sb.append("**Monthly Expense (avg):** ").append(formatMoney(ctx.getMonthlyAvgExpense())).append(" VND\n");
        sb.append("**Savings Rate:** ").append(String.format("%.1f%%", ctx.getSavingsRate() * 100)).append("\n\n");
        
        // Wallets
        if (!ctx.getWallets().isEmpty()) {
            sb.append("## Wallets\n");
            for (WalletDto w : ctx.getWallets()) {
                sb.append("- ").append(w.getName()).append(": ")
                  .append(formatMoney(w.getBalance())).append(" ").append(w.getCurrencyCode()).append("\n");
            }
            sb.append("\n");
        }
        
        // Category spending (top 5)
        if (!ctx.getCategorySpending().isEmpty()) {
            sb.append("## Top Spending Categories (Last 30 days)\n");
            ctx.getCategorySpending().entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5)
                .forEach(e -> sb.append("- ").append(e.getKey()).append(": ")
                    .append(formatMoney(e.getValue())).append(" VND\n"));
            sb.append("\n");
        }
        
        // Analytics cache (if exists)
        if (!ctx.getAnalyticsCache().isEmpty()) {
            sb.append("## Recent Analysis Results\n");
            try {
                sb.append(objectMapper.writeValueAsString(ctx.getAnalyticsCache())).append("\n\n");
            } catch (JsonProcessingException e) {
                // Ignore
            }
        }
        
        return sb.toString().trim();
    }

    /**
     * Update analytics cache with new analysis results.
     */
    @Transactional
    public void updateAnalyticsCache(String userId, Map<String, Object> analyticsData) {
        UserFinancialContext context = repository.findByUserId(userId)
                .orElse(null);
        
        if (context == null) {
            return; // Context not synced yet
        }
        
        try {
            // Merge with existing cache
            Map<String, Object> existingCache = context.getAnalyticsCacheJson() != null
                    ? objectMapper.readValue(
                            context.getAnalyticsCacheJson(),
                            objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class))
                    : Collections.emptyMap();
            
            existingCache.putAll(analyticsData);
            existingCache.put("lastUpdated", OffsetDateTime.now().toString());
            
            context.setAnalyticsCacheJson(objectMapper.writeValueAsString(existingCache));
            context.setUpdatedAt(OffsetDateTime.now());
            repository.save(context);
        } catch (Exception e) {
            // Log but don't fail
            // Could add logging here
        }
    }

    private String formatMoney(Double amount) {
        if (amount == null) return "0";
        return String.format("%,.0f", amount);
    }
}

