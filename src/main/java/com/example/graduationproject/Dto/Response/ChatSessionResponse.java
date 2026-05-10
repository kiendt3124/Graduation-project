package com.example.graduationproject.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatSessionResponse {

    private UUID sessionId;

    /** Tin nhắn đầu tiên của user trong session (dùng làm preview) */
    private String firstMessage;

    /** Thời điểm bắt đầu session */
    private LocalDateTime startedAt;

    /** Tổng số tin nhắn trong session */
    private int messageCount;
}
