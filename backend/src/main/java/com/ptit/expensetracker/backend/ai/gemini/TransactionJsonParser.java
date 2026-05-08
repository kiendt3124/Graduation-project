package com.ptit.expensetracker.backend.ai.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptit.expensetracker.backend.ai.dto.ParsedTransactionDto;
import com.ptit.expensetracker.backend.common.exception.ApiException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Component
public class TransactionJsonParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ParsedTransactionDto parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ApiException("Empty response from Gemini", "GEMINI_EMPTY_RESPONSE");
        }

        String cleaned = cleanup(raw);
        try {
            JsonNode root = objectMapper.readTree(cleaned);

            Double amount = root.path("amount").isNumber() ? root.get("amount").asDouble() : null;
            String currencyCode = asTextOrNull(root.get("currencyCode"));
            String categoryName = asTextOrNull(root.get("categoryName"));
            String description = asTextOrNull(root.get("description"));
            String walletName = asTextOrNull(root.get("walletName"));

            LocalDate date = null;
            if (root.hasNonNull("date")) {
                try {
                    date = LocalDate.parse(root.get("date").asText());
                } catch (DateTimeParseException ignored) {
                    // keep null if format invalid
                }
            }

            return ParsedTransactionDto.builder()
                    .amount(amount)
                    .currencyCode(emptyToNull(currencyCode))
                    .categoryName(emptyToNull(categoryName))
                    .description(emptyToNull(description))
                    .date(date)
                    .walletName(emptyToNull(walletName))
                    .build();
        } catch (IOException e) {
            throw new ApiException("Failed to parse Gemini response", "GEMINI_PARSE_ERROR");
        }
    }

    private String cleanup(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            // remove code fences like ```json ... ```
            int first = trimmed.indexOf("```");
            int last = trimmed.lastIndexOf("```");
            if (first != -1 && last != -1 && last > first) {
                trimmed = trimmed.substring(first + 3, last);
            }
        }
        // remove leading 'json' if present after fence
        trimmed = trimmed.replaceFirst("^json", "").trim();
        return trimmed;
    }

    private String asTextOrNull(JsonNode node) {
        if (node == null || node.isNull()) return null;
        String txt = node.asText();
        return txt == null ? null : txt;
    }

    private String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}


