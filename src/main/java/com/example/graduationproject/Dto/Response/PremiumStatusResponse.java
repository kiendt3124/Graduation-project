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
public class PremiumStatusResponse {
    private String tier;              // "BASIC" hoặc "PREMIUM"
    private LocalDateTime expiredAt;  // null nếu BASIC
    private boolean expired;          // true nếu Premium đã hết hạn
}
