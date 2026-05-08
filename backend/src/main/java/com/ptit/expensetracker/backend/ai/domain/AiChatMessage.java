package com.ptit.expensetracker.backend.ai.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "ai_chat_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID of the authenticated user (Firebase uid).
     */
    @Column(name = "user_id", nullable = false, length = 128)
    private String userId;

    /**
     * Role of the message in the conversation.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private MessageRole role;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    /**
     * Soft-delete marker for transcript visibility.
     * If not null, this message should not be shown in UI history.
     * Memory summary may still contain information derived from it.
     */
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
}



