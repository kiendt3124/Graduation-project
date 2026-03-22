package com.example.graduationproject.Dto.Request;

import com.example.graduationproject.Entity.Enum.WalletType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@AllArgsConstructor
@Data
public class UpdateWalletRequest {
    @NotNull
    private UUID id;
    private String name;
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal balance;
    private WalletType walletType;
    private Boolean isDeleted;

}
