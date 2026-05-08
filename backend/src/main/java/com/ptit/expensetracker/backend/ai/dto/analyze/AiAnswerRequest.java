package com.ptit.expensetracker.backend.ai.dto.analyze;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AiAnswerRequest {
    @NotBlank(message = "prompt must not be blank")
    private String prompt;

    private String locale;
    private String timezone;
    private String currencyCode;

    @NotNull(message = "dataRequest must not be null")
    @Valid
    private DataRequest dataRequest;

    @NotNull(message = "dataResponse must not be null")
    @Valid
    private DataResponse dataResponse;
}



