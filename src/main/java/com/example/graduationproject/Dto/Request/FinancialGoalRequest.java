package com.example.graduationproject.Dto.Request;

import com.example.graduationproject.Entity.Enum.GoalStatus;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class FinancialGoalRequest {

    // ── Tạo mục tiêu mới ──────────────────────────────────────────────────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Create {

        @NotBlank(message = "name is required")
        @Size(max = 255)
        private String name;

        @NotNull(message = "targetAmount is required")
        @Positive(message = "targetAmount must be positive")
        private BigDecimal targetAmount;

        /** Hạn hoàn thành. Null = không giới hạn */
        private LocalDate deadline;

        @Size(max = 500)
        private String note;
    }

    // ── Cập nhật mục tiêu (partial patch) ────────────────────────────────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Update {

        /** Null = không thay đổi field đó */

        @Size(max = 255)
        private String name;

        @Positive(message = "targetAmount must be positive")
        private BigDecimal targetAmount;

        private LocalDate deadline;

        /** Cho phép cập nhật thủ công sang CANCELLED */
        private GoalStatus status;

        @Size(max = 500)
        private String note;
    }

    // ── Góp tiền vào mục tiêu ─────────────────────────────────────────────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddContribution {

        @NotNull(message = "walletId is required")
        private UUID walletId;

        @NotNull(message = "amount is required")
        @Positive(message = "amount must be positive")
        private BigDecimal amount;

        @NotNull(message = "contributionDate is required")
        private LocalDate contributionDate;

        @Size(max = 500)
        private String note;
    }
}
