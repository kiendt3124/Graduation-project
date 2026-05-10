package com.example.graduationproject.Entity;

import com.example.graduationproject.Entity.Enum.PartnerType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "partners")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Partner {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * Tên đối tác: "Vietcombank", "BIDV", "Techcombank", "Finhay"...
     */
    @Column(nullable = false)
    private String name;

    /**
     * URL logo của đối tác (có thể là link ảnh hoặc tên icon).
     */
    @Column(name = "logo_url")
    private String logoUrl;

    /**
     * Loại đối tác: BANK (ngân hàng) hoặc INVESTMENT_FUND (quỹ đầu tư).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "partner_type", nullable = false)
    private PartnerType partnerType;

    /**
     * Lãi suất vay hiện tại (% mỗi năm). Ví dụ: 8.5 = 8.5%/năm.
     * Null nếu đối tác không có sản phẩm vay.
     */
    @Column(name = "loan_interest_rate", precision = 5, scale = 2)
    private BigDecimal loanInterestRate;

    /**
     * Lãi suất tiết kiệm hiện tại (% mỗi năm). Ví dụ: 5.2 = 5.2%/năm.
     * Null nếu đối tác không có sản phẩm tiết kiệm.
     */
    @Column(name = "saving_interest_rate", precision = 5, scale = 2)
    private BigDecimal savingInterestRate;

    /**
     * Mô tả ngắn về đối tác / sản phẩm nổi bật.
     */
    @Column(length = 500)
    private String description;

    /**
     * Trạng thái hiển thị: true = hiển thị cho user, false = ẩn.
     */
    @Column(name = "is_active", nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private Boolean isActive = Boolean.TRUE;

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
