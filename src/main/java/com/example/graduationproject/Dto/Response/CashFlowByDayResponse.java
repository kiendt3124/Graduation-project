package com.example.graduationproject.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashFlowByDayResponse {

    /** Ngày */
    private LocalDate date;

    /** Tổng thu trong ngày */
    private BigDecimal income;

    /** Tổng chi trong ngày */
    private BigDecimal expense;

    /** Dòng tiền ròng = income - expense */
    private BigDecimal net;
}
