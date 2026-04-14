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
public class ReportSummaryResponse {

    /** Tháng báo cáo (1–12) */
    private Integer month;

    /** Năm báo cáo */
    private Integer year;

    /** Tổng thu nhập trong tháng */
    private BigDecimal totalIncome;

    /** Tổng chi tiêu trong tháng */
    private BigDecimal totalExpense;

    /** Dòng tiền ròng = totalIncome - totalExpense */
    private BigDecimal netCashFlow;

    /** Tổng số dư hiện tại của tất cả ví */
    private BigDecimal totalBalance;

    /** Số khoản vay / cho vay đang ACTIVE */
    private Long activeLoansCount;

    /** Tổng dư nợ đang đi vay (BORROW) */
    private BigDecimal totalDebt;

    /** Tổng dư nợ đang cho vay (LEND) */
    private BigDecimal totalLent;

    /** Số mục tiêu tài chính đang thực hiện (ACTIVE) */
    private Long goalsInProgress;

    /** Số mục tiêu tài chính đã hoàn thành (COMPLETED) */
    private Long goalsCompleted;
}
