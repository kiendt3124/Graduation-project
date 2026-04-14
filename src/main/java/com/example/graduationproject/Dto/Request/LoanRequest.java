package com.example.graduationproject.Dto.Request;

import com.example.graduationproject.Entity.Enum.LoanStatus;
import com.example.graduationproject.Entity.Enum.LoanType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class LoanRequest {

    // ── Tạo khoản vay mới ─────────────────────────────────────────────────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Create {

        @NotBlank(message = "counterpart is required")
        @Size(max = 255)
        private String counterpart;

        @NotNull(message = "loanType is required")
        private LoanType loanType;

        @NotNull(message = "principalAmount is required")
        @Positive(message = "principalAmount must be positive")
        private BigDecimal principalAmount;

        /**
         * Lãi suất %/năm (lãi đơn). Null = không tính lãi.
         * Ví dụ: 12.0 → 12%/năm
         */
        @DecimalMin(value = "0.0", message = "interestRate must be >= 0")
        @DecimalMax(value = "999.99", message = "interestRate must be <= 999.99")
        private BigDecimal interestRate;

        @NotNull(message = "startDate is required")
        private LocalDate startDate;

        /** Ngày đến hạn. Null = không giới hạn thời gian. */
        private LocalDate dueDate;

        @Size(max = 500)
        private String note;
    }

    // ── Cập nhật khoản vay ────────────────────────────────────────────────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Update {

        /** Để null = không thay đổi field đó */

        @Size(max = 255)
        private String counterpart;

        @DecimalMin(value = "0.0")
        @DecimalMax(value = "999.99")
        private BigDecimal interestRate;

        private LocalDate dueDate;

        /** Cho phép cập nhật status thủ công (ví dụ: đánh dấu PAID tay) */
        private LoanStatus status;

        @Size(max = 500)
        private String note;
    }

    // ── Ghi nhận 1 lần thanh toán ─────────────────────────────────────────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddPayment {

        @NotNull(message = "walletId is required")
        private UUID walletId;

        @NotNull(message = "amount is required")
        @Positive(message = "amount must be positive")
        private BigDecimal amount;

        @NotNull(message = "paymentDate is required")
        private LocalDate paymentDate;

        @Size(max = 500)
        private String note;
    }
}
