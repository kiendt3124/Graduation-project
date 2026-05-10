package com.example.graduationproject.Dto.Request;

import com.example.graduationproject.Entity.Enum.PartnerType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

public class PartnerRequest {

    // ─── TẠO MỚI ─────────────────────────────────────────────────────────────
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Create {

        @NotBlank(message = "Tên đối tác không được để trống")
        private String name;

        private String logoUrl;

        @NotNull(message = "partnerType không được để trống (BANK | INVESTMENT_FUND)")
        private PartnerType partnerType;

        @DecimalMin(value = "0.0", message = "Lãi suất vay không được âm")
        @DecimalMax(value = "100.0", message = "Lãi suất vay không được vượt quá 100%")
        private BigDecimal loanInterestRate;

        @DecimalMin(value = "0.0", message = "Lãi suất tiết kiệm không được âm")
        @DecimalMax(value = "100.0", message = "Lãi suất tiết kiệm không được vượt quá 100%")
        private BigDecimal savingInterestRate;

        private String description;
    }

    // ─── CẬP NHẬT ────────────────────────────────────────────────────────────
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Update {

        @NotNull(message = "id không được để trống")
        private UUID id;

        private String name;
        private String logoUrl;
        private PartnerType partnerType;

        @DecimalMin(value = "0.0", message = "Lãi suất vay không được âm")
        @DecimalMax(value = "100.0", message = "Lãi suất vay không được vượt quá 100%")
        private BigDecimal loanInterestRate;

        @DecimalMin(value = "0.0", message = "Lãi suất tiết kiệm không được âm")
        @DecimalMax(value = "100.0", message = "Lãi suất tiết kiệm không được vượt quá 100%")
        private BigDecimal savingInterestRate;

        private String description;
    }
}
