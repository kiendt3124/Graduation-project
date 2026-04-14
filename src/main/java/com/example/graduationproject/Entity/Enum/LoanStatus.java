package com.example.graduationproject.Entity.Enum;

public enum LoanStatus {
    ACTIVE,   // Còn dư nợ
    PAID,     // Đã trả / nhận đủ
    OVERDUE   // Quá hạn (dueDate < today && chưa PAID)
}
