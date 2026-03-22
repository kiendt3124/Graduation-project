package com.example.graduationproject.Dto.Response;

import com.example.graduationproject.Entity.Enum.WalletType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class WalletResponse {
        private UUID id;
        private String name;
        private BigDecimal balance;
        private WalletType walletType;
        private Boolean isDeleted;

}
