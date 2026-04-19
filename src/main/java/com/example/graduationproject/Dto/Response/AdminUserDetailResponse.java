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
public class AdminUserDetailResponse {
    private UUID id;
    private String email;
    private String googleId;
    private String role;
    private String accountTier;
    private boolean isBanned;
    private LocalDateTime premiumExpiredAt;
    private int walletCount;
    private int transactionCount;
    private int loanCount;
    private int goalCount;
    private int budgetCount;
    private int premiumOrderCount;
}
