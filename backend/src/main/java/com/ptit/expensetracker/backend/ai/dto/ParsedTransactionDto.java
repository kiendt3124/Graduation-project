package com.ptit.expensetracker.backend.ai.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class ParsedTransactionDto {

    Double amount;
    String currencyCode;
    String categoryName;
    String description;
    LocalDate date;
    String walletName;
}



