package com.ptit.expensetracker.backend.ai.function;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * Represents an AI function that can be called by Gemini.
 */
@Value
@Builder
public class AiFunction {
    String name;
    String description;
    Map<String, Object> parameters; // JSON schema for parameters
}

