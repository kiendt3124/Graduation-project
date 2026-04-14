package com.example.graduationproject.Dto.Webhook;

import lombok.Data;

@Data
public class SePayWebhookPayload {
    private Long id;                  // ID giao dịch trên SePay
    private String gateway;           // "Techcombank"
    private String transactionDate;   // "2024-04-13 10:00:00"
    private String accountNumber;     // Số TK nhận tiền
    private String subAccount;        // Tài khoản phụ (nếu có)
    private String transferType;      // "in" hoặc "out"
    private Long transferAmount;      // Số tiền (VNĐ)
    private Long accumulated;         // Số dư sau giao dịch
    private String code;              // Mã unique của SePay
    private String content;           // NỘI DUNG CHUYỂN KHOẢN — dùng match txnRef
    private String referenceCode;     // Mã tham chiếu ngân hàng
    private String description;       // Mô tả giao dịch
}
