package com.example.graduationproject.Dto.Request;

import com.example.graduationproject.Entity.Enum.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateCategoryRequest {

    @NotBlank
    private String name;

    private String icon;

    @NotNull
    private TransactionType transactionType;   // INCOME hoặc EXPENSE
}
