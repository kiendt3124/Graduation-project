package com.ptit.expensetracker.backend.ai.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptit.expensetracker.backend.ai.dto.InsightsResponse;
import com.ptit.expensetracker.backend.common.exception.ApiException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class InsightsJsonParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public InsightsResponse parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ApiException("Empty response from Gemini", "GEMINI_EMPTY_RESPONSE");
        }
        String cleaned = cleanup(raw);
        try {
            JsonNode root = objectMapper.readTree(cleaned);
            List<String> alerts = readArray(root.get("alerts"));
            List<String> tips = readArray(root.get("tips"));
            return InsightsResponse.builder()
                    .alerts(alerts)
                    .tips(tips)
                    .build();
        } catch (IOException e) {
            throw new ApiException("Failed to parse Gemini response", "GEMINI_PARSE_ERROR");
        }
    }

    private List<String> readArray(JsonNode node) {
        List<String> result = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode n : node) {
                if (n.isTextual()) {
                    String val = n.asText();
                    if (val != null && !val.isBlank()) {
                        result.add(val);
                    }
                }
            }
        }
        return result;
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
        trimmed = trimmed.replaceFirst("^json", "").trim();
        return trimmed;
    }
}


