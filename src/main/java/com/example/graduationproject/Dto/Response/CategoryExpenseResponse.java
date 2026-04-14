package com.example.graduationproject.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryExpenseResponse {

    /** ID của danh mục */
    private UUID categoryId;

    /** Tên danh mục */
    private String categoryName;

    /** Tổng số tiền trong tháng */
    private BigDecimal totalAmount;

    /**
     * Tỉ lệ % so với tổng thu hoặc tổng chi trong tháng.
     * Ví dụ: 35.50 → chiếm 35.5%
     */
    private BigDecimal percentage;
}
