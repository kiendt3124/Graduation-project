package com.example.graduationproject.Dto.Response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class LoanPaymentResponse {

    private UUID id;
    private UUID loanId;
    private UUID walletId;
    private String walletName;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private String note;
    private LocalDateTime createdAt;
}
