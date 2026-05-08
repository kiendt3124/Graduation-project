package com.ptit.expensetracker.backend.ai.dto.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDto {
    private Integer id;
    private Double amount;
    private String type; // INFLOW / OUTFLOW
    private String date; // ISO-8601 string (yyyy-MM-dd or datetime)
    private String description;
    private String categoryMetadata;
    private Integer walletId;
}

