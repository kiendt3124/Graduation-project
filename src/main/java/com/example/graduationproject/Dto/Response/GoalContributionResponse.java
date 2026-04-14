package com.example.graduationproject.Dto.Response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class GoalContributionResponse {

    private UUID id;
    private UUID goalId;
    private UUID walletId;
    private String walletName;
    private BigDecimal amount;
    private LocalDate contributionDate;
    private String note;
    private LocalDateTime createdAt;
}
