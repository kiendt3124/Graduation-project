package com.ptit.expensetracker.backend.ai.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ChatResponse {

    /**
     * Assistant reply rendered to the end user.
     */
    String reply;

    /**
     * Optional suggestions for follow-up actions
     * in the client (e.g. "create_budget", "view_report").
     */
    List<String> suggestions;

    /**
     * Optional structured data (e.g. analytics results, loan options).
     * Can be used by client for rendering charts, tables, etc.
     */
    java.util.Map<String, Object> data;
}



