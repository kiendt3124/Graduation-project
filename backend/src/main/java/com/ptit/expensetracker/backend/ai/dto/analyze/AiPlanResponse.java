package com.ptit.expensetracker.backend.ai.dto.analyze;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AiPlanResponse {
    DataRequest dataRequest;
}



