package com.ptit.expensetracker.backend.ai.dto.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletDto {
    private Integer id;
    private String name;
    private Double balance;
    private String currencyCode;
}

