package com.ptit.expensetracker.backend.ai.function;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * Represents a function call returned by Gemini.
 */
@Value
@Builder
public class FunctionCall {
    String name;
    Map<String, Object> args;
}

