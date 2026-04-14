package com.example.graduationproject.Entity.Enum;

public enum PremiumOrderStatus {
    PENDING,    // Chờ chuyển khoản
    COMPLETED,  // Đã nhận tiền, đã kích hoạt
    EXPIRED,    // Hết giờ (quá 30 phút không thanh toán)
    FAILED      // Sai số tiền hoặc lỗi khác
}
