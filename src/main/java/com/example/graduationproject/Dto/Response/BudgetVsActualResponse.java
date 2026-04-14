package com.example.graduationproject.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetVsActualResponse {

    /** ID của ngân sách */
    private UUID budgetId;

    /** ID danh mục */
    private UUID categoryId;

    /** Tên danh mục */
    private String categoryName;

    /** Hạn mức ngân sách đã đặt */
    private BigDecimal budgetLimit;

    /** Số tiền thực tế đã chi */
    private BigDecimal actualSpent;

    /** Số tiền còn lại = budgetLimit - actualSpent (âm nếu vượt ngân sách) */
    private BigDecimal remaining;

    /**
     * Tỉ lệ sử dụng % = (actualSpent / budgetLimit) * 100.
     * Có thể > 100 nếu vượt ngân sách.
     */
    private BigDecimal usagePercentage;

    /** true nếu actualSpent >= budgetLimit */
    private Boolean isOverBudget;
}
