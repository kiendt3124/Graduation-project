package com.ptit.expensetracker.backend.ai.dto;

import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;

@Value
@Builder
public class ChatHistoryDto {
    String role; // USER / ASSISTANT
    String content;
    OffsetDateTime createdAt;
}


