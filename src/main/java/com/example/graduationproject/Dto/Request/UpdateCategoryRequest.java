package com.example.graduationproject.Dto.Request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCategoryRequest {

    @NotNull
    private UUID id;

    private String name;

    private String icon;
}
