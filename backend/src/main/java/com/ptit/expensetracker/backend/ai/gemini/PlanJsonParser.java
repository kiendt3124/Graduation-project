package com.ptit.expensetracker.backend.ai.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptit.expensetracker.backend.ai.dto.analyze.DataRequest;
import com.ptit.expensetracker.backend.ai.dto.analyze.DatasetType;
import com.ptit.expensetracker.backend.common.exception.ApiException;
import org.springframework.stereotype.Component;

@Component
public class PlanJsonParser {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DataRequest parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ApiException("Empty response from Gemini", "GEMINI_EMPTY_RESPONSE");
        }
        String cleaned = cleanup(raw);
        try {
            // Map dataset strings to enum
            var node = objectMapper.readTree(cleaned);
            var analysisType = node.path("analysisType").asText("GENERIC");
            var timeRange = node.path("timeRange").asText("THIS_MONTH");
            Integer topK = node.has("topK") && node.get("topK").isInt() ? node.get("topK").asInt() : null;

            var datasetsNode = node.path("requiredDatasets");
            java.util.List<DatasetType> datasets = new java.util.ArrayList<>();
            if (datasetsNode.isArray()) {
                for (var it = datasetsNode.elements(); it.hasNext(); ) {
                    var s = it.next().asText();
                    try {
                        datasets.add(DatasetType.valueOf(s));
                    } catch (Exception ignored) {
                        // ignore unknown
                    }
                }
            }

            return DataRequest.builder()
                    .analysisType(analysisType)
                    .timeRange(timeRange)
                    .requiredDatasets(datasets.isEmpty()
                            ? java.util.List.of(DatasetType.MONTHLY_TOTALS, DatasetType.TOP_CATEGORIES)
                            : datasets)
                    .topK(topK == null ? 5 : topK)
                    .build();
        } catch (Exception e) {
            throw new ApiException("Failed to parse Gemini response", "GEMINI_PARSE_ERROR");
        }
    }

    private String cleanup(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            int first = trimmed.indexOf("```");
            int last = trimmed.lastIndexOf("```");
            if (first != -1 && last != -1 && last > first) {
                trimmed = trimmed.substring(first + 3, last);
            }
        }
        return trimmed.replaceFirst("^json", "").trim();
    }
}



