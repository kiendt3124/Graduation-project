package com.ptit.expensetracker.backend.ai.dto.analyze;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class DataRequest {
    /**
     * High-level intent, for example: COMPARE_MONTHS, FORECAST_MONTH, GENERIC.
     */
    String analysisType;

    /**
     * Time range identifier, e.g. LAST_3_MONTHS, THIS_MONTH, LAST_6_MONTHS.
     */
    String timeRange;

    /**
     * Whitelisted datasets the client can provide.
     */
    List<DatasetType> requiredDatasets;

    /**
     * Top-K for ranked datasets (e.g. TOP_CATEGORIES).
     */
    Integer topK;
}



