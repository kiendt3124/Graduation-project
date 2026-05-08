package com.ptit.expensetracker.backend.ai.gemini.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class GeminiResponse {
    private List<Candidate> candidates;

    @Data
    public static class Candidate {
        private Content content;
    }

    @Data
    public static class Content {
        private List<Part> parts;
    }

    @Data
    public static class Part {
        private String text;
        private FunctionCall functionCall;
    }

    @Data
    public static class FunctionCall {
        private String name;
        private Map<String, Object> args;
    }
}


