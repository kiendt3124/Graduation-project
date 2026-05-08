package com.ptit.expensetracker.backend.ai.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class InsightsResponse {

    List<String> alerts;
    List<String> tips;
}



