package com.example.graduationproject.Entity;

import com.example.graduationproject.Entity.Enum.LoanStatus;
import com.example.graduationproject.Entity.Enum.LoanType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "loans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Tên người vay / người cho vay (đối tác trong khoản vay).
     * Ví dụ: "Nguyễn Văn A", "Công ty XYZ"
     */
    @Column(nullable = false, length = 255)
    private String counterpart;

    @Enumerated(EnumType.STRING)
    @Column(name = "loan_type", nullable = false)
    private LoanType loanType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private LoanStatus status = LoanStatus.ACTIVE;

    /**
     * Số tiền gốc ban đầu (không thay đổi sau khi tạo).
     */
    @Column(name = "principal_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal principalAmount;

    /**
     * Lãi suất %/năm (lãi đơn). Null = không tính lãi.
     * Ví dụ: 12.0 → 12%/năm
     */
    @Column(name = "interest_rate", precision = 5, scale = 2)
    private BigDecimal interestRate;

    /**
     * Ngày bắt đầu khoản vay (dùng để tính lãi tích lũy).
     */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /**
     * Ngày đến hạn trả. Null = không giới hạn thời gian.
     */
    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(length = 500)
    private String note;

    @Column(name = "is_deleted", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean isDeleted = Boolean.FALSE;

    @Builder.Default
    @OneToMany(mappedBy = "loan", fetch = FetchType.LAZY,
               cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LoanPayment> payments = new ArrayList<>();

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
