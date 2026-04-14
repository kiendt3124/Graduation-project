package com.example.graduationproject.Dto.Response;

import com.example.graduationproject.Entity.Enum.LoanStatus;
import com.example.graduationproject.Entity.Enum.LoanType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class LoanResponse {

    private UUID id;

    /** Tên người vay / cho vay */
    private String counterpart;

    private LoanType loanType;     // BORROW | LEND
    private LoanStatus status;     // ACTIVE | PAID | OVERDUE

    // ── Số tiền ─────────────────────────────────────────────────────────────

    /** Số tiền gốc ban đầu */
    private BigDecimal principalAmount;

    /**
     * Lãi tích lũy tính đến hôm nay (lãi đơn).
     * = principalAmount × interestRate/100 × số ngày / 365
     * = 0 nếu interestRate == null
     */
    private BigDecimal interestAmount;

    /** Tổng tiền phải trả/nhận = principalAmount + interestAmount */
    private BigDecimal totalDue;

    /** Tổng đã thanh toán (sum của tất cả LoanPayment) */
    private BigDecimal paidAmount;

    /** Còn lại cần trả = totalDue - paidAmount */
    private BigDecimal remainingAmount;

    /** Tiến độ thanh toán (%) = paidAmount / totalDue × 100 */
    private BigDecimal progressPercent;

    // ── Lãi suất ────────────────────────────────────────────────────────────

    /** %/năm. Null = không tính lãi */
    private BigDecimal interestRate;

    // ── Thời gian ───────────────────────────────────────────────────────────

    private LocalDate startDate;

    /** Null = không có hạn */
    private LocalDate dueDate;

    /** true nếu dueDate đã qua hôm nay và status != PAID */
    private Boolean isOverdue;

    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
