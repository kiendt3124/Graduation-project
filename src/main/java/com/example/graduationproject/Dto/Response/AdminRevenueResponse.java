package com.example.graduationproject.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminRevenueResponse {
    private long totalRevenue;
    private long monthlyRevenue;
    private long yearlyRevenue;
    private List<MonthlyRevenueStat> monthlyBreakdown;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyRevenueStat {
        private int year;
        private int month;
        private long revenue;
        private long orderCount;
    }
}
