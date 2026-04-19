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
public class AdminStatsResponse {
    // Tổng quan người dùng
    private long totalUsers;
    private long totalBasicUsers;
    private long totalPremiumUsers;
    private long totalBannedUsers;

    // Tổng quan Premium
    private long totalPremiumOrders;
    private long totalCompletedOrders;
    private long totalRevenue;              // VNĐ, tổng tất cả đơn COMPLETED

    // Người dùng mới theo ngày (7 ngày gần nhất)
    private List<DailyNewUserStat> newUsersLast7Days;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyNewUserStat {
        private String date;   // "2026-04-18"
        private long count;
    }
}
