package com.ptit.expensetracker.backend.ai.dto.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyTotalDto {
    private String month; // yyyy-MM
    private Double income;
    private Double expense;
}

