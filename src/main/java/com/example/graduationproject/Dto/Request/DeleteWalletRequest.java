package com.example.graduationproject.Dto.Request;


import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@AllArgsConstructor
@Data
public class DeleteWalletRequest {
    @NotNull
    private UUID id;
}
