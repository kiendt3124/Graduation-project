package com.ptit.expensetracker.backend.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ParseTransactionRequest {

    /**
     * Natural language description of a transaction,
     * for example: "Trưa nay ăn cơm 50k ở quán A".
     */
    @NotBlank(message = "text must not be blank")
    private String text;

    /**
     * Optional locale (e.g. "vi-VN").
     */
    private String locale;
}



