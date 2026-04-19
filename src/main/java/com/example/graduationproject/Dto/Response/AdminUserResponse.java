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
public class AdminUserResponse {
    private UUID id;
    private String email;
    private String role;          // "USER" | "ADMIN"
    private String accountTier;   // "BASIC" | "PREMIUM"
    private boolean isBanned;
    private LocalDateTime premiumExpiredAt;  // null nếu BASIC
    private int walletCount;
    private int transactionCount;
}
