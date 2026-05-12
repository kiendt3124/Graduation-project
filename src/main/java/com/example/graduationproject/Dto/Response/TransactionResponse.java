package com.example.graduationproject.Dto.Response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.example.graduationproject.Entity.Wallet;
import com.example.graduationproject.Entity.Enum.TransactionType;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TransactionResponse {
    private UUID id;

    // Thông tin ví nguồn
    private UUID walletId;
    private String walletName;

    // Ví đích (chỉ có khi TRANSFER)
    private UUID toWalletId;
    private String toWalletName;

    // Danh mục
    private UUID categoryId;
    private String categoryName;

    private BigDecimal amount;
    private TransactionType transactionType;
    private LocalDateTime transactionDate;
    private String note;
    private String imageUrl;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
