package com.example.graduationproject.Entity;

import com.example.graduationproject.Entity.Enum.TransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * The wallet this transaction belongs to (source wallet for TRANSFER).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    /**
     * For TRANSFER transactions: the destination wallet.
     * Null for INCOME and EXPENSE transactions.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_wallet_id")
    private Wallet toWallet;

    /**
     * Category of the transaction (e.g. Food, Salary, Transport).
     * Can be null for TRANSFER transactions.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    /**
     * Amount of money involved.
     * Always a positive value; the type (INCOME/EXPENSE) determines the sign.
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /**
     * INCOME, EXPENSE, or TRANSFER
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;

    /**
     * Date and time the transaction occurred (user-defined, not server time).
     */
    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    /**
     * Optional free-text note for the transaction.
     */
    @Column(length = 500)
    private String note;

    /**
     * Soft delete flag.
     */
    @Column(name = "is_deleted", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean isDeleted = Boolean.FALSE;

    /**
     * Server-side timestamp when the record was created.
     */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Server-side timestamp when the record was last updated.
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
