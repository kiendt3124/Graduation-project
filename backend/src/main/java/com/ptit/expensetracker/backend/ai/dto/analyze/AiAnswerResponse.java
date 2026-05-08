package com.ptit.expensetracker.backend.ai.dto.analyze;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class AiAnswerResponse {
    String answerMarkdown;
    List<ActionDto> actions;

    @Value
    @Builder
    public static class ActionDto {
        String type;
        String payloadJson;
        String label;
    }
}



