package com.example.graduationproject.Dto.Request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

public class BudgetRequest {

    /**
     * Tạo ngân sách mới cho 1 category trong 1 tháng/năm cụ thể.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Create {

        @NotNull(message = "categoryId is required")
        private UUID categoryId;

        @NotNull(message = "amount is required")
        @Positive(message = "amount must be positive")
        private BigDecimal amount;

        @Min(value = 1, message = "month must be between 1 and 12")
        @Max(value = 12, message = "month must be between 1 and 12")
        private int month;

        @Min(value = 2000, message = "year must be 2000 or later")
        private int year;
    }

    /**
     * Cập nhật hạn mức của một ngân sách đã tồn tại.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Update {

        @NotNull(message = "id is required")
        private UUID id;

        @NotNull(message = "amount is required")
        @Positive(message = "amount must be positive")
        private BigDecimal amount;
    }
}
