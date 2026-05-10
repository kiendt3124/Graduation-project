package com.example.graduationproject.Dto.Response;

import com.example.graduationproject.Entity.Enum.PartnerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerResponse {

    private UUID id;
    private String name;
    private String logoUrl;
    private PartnerType partnerType;
    private BigDecimal loanInterestRate;
    private BigDecimal savingInterestRate;
    private String description;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
