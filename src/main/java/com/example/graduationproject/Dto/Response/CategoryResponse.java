package com.example.graduationproject.Dto.Response;

import com.example.graduationproject.Entity.Enum.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryResponse {

    private UUID id;
    private String name;
    private String icon;
    private TransactionType transactionType;
}
