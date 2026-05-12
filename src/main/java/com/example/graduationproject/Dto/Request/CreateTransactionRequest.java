package com.example.graduationproject.Dto.Request;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.example.graduationproject.Entity.Enum.TransactionType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateTransactionRequest {
    @NotNull
    private UUID walletId; // ví nguồn
    private UUID toWalletId; // chỉ bắt buộc khi TRANSFER
    private UUID categoryId; // null khi TRANSFER

    @NotNull
    @Positive
    private BigDecimal amount;
    @NotNull
    private TransactionType transactionType; // INCOME / EXPENSE / TRANSFER
    @NotNull
    private LocalDateTime transactionDate; // do user chọn, không phải server time
    @Size(max = 500)
    private String note;

    @Size(max = 500)
    private String imageUrl;  // URL ảnh hóa đơn (tùy chọn)

}
