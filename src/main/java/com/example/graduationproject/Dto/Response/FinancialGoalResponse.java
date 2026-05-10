package com.example.graduationproject.Dto.Response;

import com.example.graduationproject.Entity.Enum.GoalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialGoalResponse {

    private UUID id;
    private String name;

    // ── Số tiền ──────────────────────────────────────────────────────────────

    /** Số tiền cần đạt */
    private BigDecimal targetAmount;

    /** Tổng đã góp */
    private BigDecimal currentAmount;

    /** Còn lại = targetAmount - currentAmount (min 0) */
    private BigDecimal remainingAmount;

    /** Tiến độ (%) = currentAmount / targetAmount × 100 */
    private BigDecimal progressPercent;

    // ── Trạng thái ───────────────────────────────────────────────────────────

    private GoalStatus status;   // ACTIVE | COMPLETED | CANCELLED

    /** true nếu deadline đã qua và status != COMPLETED */
    private Boolean isOverdue;

    // ── Thời gian ────────────────────────────────────────────────────────────

    /** Hạn hoàn thành. Null = không giới hạn */
    private LocalDate deadline;

    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
