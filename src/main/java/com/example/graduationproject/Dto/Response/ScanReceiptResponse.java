package com.example.graduationproject.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ScanReceiptResponse {

    private BigDecimal amount;
    private String transactionType;       // "EXPENSE" hoặc "INCOME"
    private String suggestedCategoryName; // tên danh mục gợi ý từ AI
    private UUID suggestedCategoryId;     // match với category trong DB
    private String merchantName;          // tên cửa hàng
    private String note;                  // ghi chú gợi ý
    private String transactionDate;       // ngày trên hóa đơn
    private String imageUrl;              // URL ảnh đã upload lên Cloudinary
}
