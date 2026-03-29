package com.example.graduationproject.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "budget",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_budget_user_category_month_year",
        columnNames = {"user_id", "category_id", "month", "year"}
    )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /**
     * Hạn mức chi tiêu cho tháng này.
     */
    @Column(name = "budget_limit", nullable = false, precision = 19, scale = 2)
    private BigDecimal limit;

    /**
     * Tháng áp dụng ngân sách (1–12).
     */
    @Column(nullable = false)
    private Integer month;

    /**
     * Năm áp dụng ngân sách (ví dụ: 2026).
     */
    @Column(nullable = false)
    private Integer year;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

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
