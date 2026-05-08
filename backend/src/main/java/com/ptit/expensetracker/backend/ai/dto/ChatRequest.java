package com.ptit.expensetracker.backend.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRequest {

    /**
     * Free-form natural language message from the user.
     */
    @NotBlank(message = "message must not be blank")
    private String message;

    /**
     * Optional locale (e.g. "vi-VN", "en-US").
     */
    private String locale;

    /**
     * Optional extra context provided by the client
     * (e.g. selected month, current screen).
     */
    private String context;
}



