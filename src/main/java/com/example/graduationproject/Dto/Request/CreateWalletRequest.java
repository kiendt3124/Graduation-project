package com.example.graduationproject.Dto.Request;

import com.example.graduationproject.Entity.Enum.WalletType;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateWalletRequest {
    @NotBlank
    private String name;
    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal balance;
    @NotNull
    private WalletType walletType;
}
