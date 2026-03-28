package com.example.graduationproject.Dto.Request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateTransactionRequest {

    @NotNull
    private UUID id;

    private UUID categoryId;

    @Positive
    private BigDecimal amount;

    private LocalDateTime transactionDate;

    @Size(max = 500)
    private String note;
}
