package com.ptit.expensetracker.backend.ai.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptit.expensetracker.backend.common.exception.ApiException;
import lombok.Value;
import org.springframework.stereotype.Component;

@Component
public class MemoryJsonParser {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value
    public static class MemoryResult {
        String summary;
        String pinnedFactsJson;
    }

    public MemoryResult parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ApiException("Empty response from Gemini", "GEMINI_EMPTY_RESPONSE");
        }
        String cleaned = cleanup(raw);
        try {
            var node = objectMapper.readTree(cleaned);
            String summary = node.path("summary").asText("");
            var pinnedFactsNode = node.path("pinnedFacts");
            String pinnedFactsJson = pinnedFactsNode.isMissingNode() || pinnedFactsNode.isNull()
                    ? null
                    : objectMapper.writeValueAsString(pinnedFactsNode);
            return new MemoryResult(summary, pinnedFactsJson);
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


























