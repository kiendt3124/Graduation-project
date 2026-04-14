package com.example.graduationproject.Entity;

import com.example.graduationproject.Entity.Enum.PremiumOrderStatus;
import com.example.graduationproject.Entity.Enum.PremiumPlan;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "premium_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PremiumOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PremiumPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PremiumOrderStatus status;

    @Column(nullable = false)
    private Long amount;            // 49000 hoặc 399000

    @Column(name = "txn_ref", nullable = false, unique = true)
    private String txnRef;          // "PREMIUM-XXXXXXXX"

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt; // createdAt + 30 phút

    @Column(name = "paid_at")
    private LocalDateTime paidAt;   // Thời điểm webhook đến
}
