package com.example.graduationproject.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PremiumOrderResponse {
    private UUID id;
    private String plan;              // "MONTHLY" hoặc "YEARLY"
    private String status;            // "PENDING" / "COMPLETED" / "EXPIRED" / "FAILED"
    private Long amount;              // 49000 hoặc 399000
    private String txnRef;            // "PREMIUM-XXXXXXXX"
    private LocalDateTime createdAt;
    private LocalDateTime expiredAt;
    private LocalDateTime paidAt;     // null nếu chưa thanh toán
}
