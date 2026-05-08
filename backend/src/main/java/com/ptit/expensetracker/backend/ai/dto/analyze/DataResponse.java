package com.ptit.expensetracker.backend.ai.dto.analyze;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class DataResponse {

    List<MonthlyTotal> monthlyTotals;
    List<CategoryTotal> topCategories;

    @Value
    @Builder
    public static class MonthlyTotal {
        String month; // yyyy-MM
        Double income;
        Double expense;
    }

    @Value
    @Builder
    public static class CategoryTotal {
        String categoryMetadata;
        Double amount;
    }
}



