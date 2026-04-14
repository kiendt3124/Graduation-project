package com.example.graduationproject.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitiatePremiumResponse {
    private String txnRef;            // "PREMIUM-ABC12345"
    private Long amount;              // 49000 hoặc 399000
    private String plan;              // "MONTHLY" hoặc "YEARLY"
    private String bankName;          // "Techcombank"
    private String accountNumber;     // Số tài khoản ngân hàng
    private String accountName;       // Tên chủ tài khoản
    private String transferContent;   // = txnRef — user phải ghi vào nội dung CK
    private String qrUrl;             // URL ảnh QR SePay để quét
    private LocalDateTime expiredAt;  // Đơn hết hạn sau 30 phút
}
