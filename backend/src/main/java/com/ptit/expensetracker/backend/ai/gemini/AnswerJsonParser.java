package com.ptit.expensetracker.backend.ai.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptit.expensetracker.backend.ai.dto.analyze.AiAnswerResponse;
import com.ptit.expensetracker.backend.common.exception.ApiException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AnswerJsonParser {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiAnswerResponse parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ApiException("Empty response from Gemini", "GEMINI_EMPTY_RESPONSE");
        }
        String cleaned = cleanup(raw);
        try {
            var node = objectMapper.readTree(cleaned);
            String answerMarkdown = node.path("answerMarkdown").asText("");
            List<AiAnswerResponse.ActionDto> actions = new ArrayList<>();
            var actionsNode = node.path("actions");
            if (actionsNode.isArray()) {
                for (var it = actionsNode.elements(); it.hasNext(); ) {
                    var a = it.next();
                    actions.add(AiAnswerResponse.ActionDto.builder()
                            .type(a.path("type").asText(""))
                            .label(a.path("label").asText(""))
                            .payloadJson(a.path("payloadJson").asText(""))
                            .build());
                }
            }
            return AiAnswerResponse.builder()
                    .answerMarkdown(answerMarkdown.isBlank() ? cleaned : answerMarkdown)
                    .actions(actions)
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



