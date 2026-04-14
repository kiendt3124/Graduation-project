package com.example.graduationproject.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "goal_contributions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalContribution {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goal_id", nullable = false)
    private FinancialGoal goal;

    /** Ví được dùng để góp tiền vào mục tiêu */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    /** Số tiền góp trong lần này */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /** Ngày góp tiền (do user nhập) */
    @Column(name = "contribution_date", nullable = false)
    private LocalDate contributionDate;

    @Column(length = 500)
    private String note;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
