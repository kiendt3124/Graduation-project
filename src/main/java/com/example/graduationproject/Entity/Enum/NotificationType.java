package com.example.graduationproject.Entity.Enum;

public enum NotificationType {
    BUDGET_EXCEEDED,        // Vượt ngân sách
    BUDGET_WARNING,         // Sắp vượt ngân sách (>80%)
    BUDGET_CREATED,         // Budget tự động tái tạo đầu tháng
    LOAN_OVERDUE,           // Khoản vay quá hạn
    LOAN_DUE_SOON,          // Khoản vay sắp đến hạn (3 ngày)
    GOAL_COMPLETED,         // Mục tiêu hoàn thành
    GOAL_DEADLINE_NEAR,     // Mục tiêu sắp hết hạn
    PREMIUM_EXPIRED,        // Premium hết hạn → hạ về BASIC
    PREMIUM_EXPIRING_SOON,  // Premium sắp hết hạn (3 ngày)
    ORDER_EXPIRED,          // Đơn Premium PENDING hết hạn
    SYSTEM                  // Thông báo hệ thống chung
}
