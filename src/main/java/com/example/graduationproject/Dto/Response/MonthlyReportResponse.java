package com.example.graduationproject.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyReportResponse {

    /** Tháng (1–12) */
    private Integer month;

    /** Năm */
    private Integer year;

    /** Tổng thu trong tháng */
    private BigDecimal totalIncome;

    /** Tổng chi trong tháng */
    private BigDecimal totalExpense;

    /** Dòng tiền ròng = totalIncome - totalExpense */
    private BigDecimal netCashFlow;
}
