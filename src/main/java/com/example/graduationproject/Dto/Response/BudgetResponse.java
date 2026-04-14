package com.example.graduationproject.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetResponse {

    /** ID của ngân sách */
    private UUID id;

    /** ID category */
    private UUID categoryId;

    /** Tên category */
    private String categoryName;

    /** Icon của category */
    private String categoryIcon;

    /** Tháng áp dụng (1–12) */
    private Integer month;

    /** Năm áp dụng */
    private Integer year;

    /** Hạn mức đã đặt */
    private BigDecimal limitAmount;

    /**
     * Tổng số tiền đã chi trong tháng cho category này.
     * Được tính real-time từ bảng transactions
     */
    private BigDecimal spentAmount;

    /** Số tiền còn lại = limitAmount - spentAmount */
    private BigDecimal remainingAmount;

    /**
     * Phần trăm đã sử dụng (0–100+).
     * Có thể > 100 nếu đã vượt ngân sách.
     */
    private BigDecimal progressPercentage;

    /**
     * Trung bình số tiền an toàn có thể tiêu mỗi ngày cho đến cuối tháng.
     * = remainingAmount / số ngày còn lại trong tháng.
     * Trả về 0 nếu đã qua tháng hoặc đã vượt ngân sách.
     */
    private BigDecimal dailySafeToSpend;

    /** true nếu spentAmount >= limitAmount */
    private Boolean isOverBudget;

    /** Thời điểm tạo ngân sách */
    private LocalDateTime createdAt;

    /** Thời điểm cập nhật gần nhất */
    private LocalDateTime updatedAt;
}
