package com.ptit.expensetracker.backend.ai.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Persisted snapshot of a user's financial context.
 * This keeps lightweight aggregates and serialized JSON blobs
 * so AI can build richer prompts without pulling full datasets each time.
 */
@Entity
@Table(name = "user_financial_contexts", indexes = {
        @Index(name = "idx_user_financial_contexts_user_id", columnList = "user_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserFinancialContext {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 128, unique = true)
    private String userId;

    /**
     * JSON array of wallets: [{id,name,balance,currencyCode}]
     */
    @Column(name = "wallets_json", columnDefinition = "TEXT")
    private String walletsJson;

    /**
     * JSON array of recent transactions (e.g. last 30 days).
     */
    @Column(name = "recent_transactions_json", columnDefinition = "TEXT")
    private String recentTransactionsJson;

    /**
     * JSON object of category spending summary: {metadata: amount}
     */
    @Column(name = "category_spending_json", columnDefinition = "TEXT")
    private String categorySpendingJson;

    /**
     * JSON array of monthly totals: [{month,income,expense}]
     */
    @Column(name = "monthly_totals_json", columnDefinition = "TEXT")
    private String monthlyTotalsJson;

    /**
     * Optional cache for the latest analytics result to reuse in chat.
     */
    @Column(name = "analytics_cache_json", columnDefinition = "TEXT")
    private String analyticsCacheJson;

    @Column(name = "total_balance")
    private Double totalBalance;

    @Column(name = "monthly_avg_income")
    private Double monthlyAvgIncome;

    @Column(name = "monthly_avg_expense")
    private Double monthlyAvgExpense;

    @Column(name = "savings_rate")
    private Double savingsRate;

    @Column(name = "last_synced_at")
    private OffsetDateTime lastSyncedAt;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}

