package com.ptit.expensetracker.backend.ai.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "ai_conversations", indexes = {
        @Index(name = "idx_ai_conversations_user_id", columnList = "user_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 128, unique = true)
    private String userId;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "pinned_facts_json", columnDefinition = "TEXT")
    private String pinnedFactsJson;

    /**
     * The last message id that has been incorporated into summary/facts.
     * Used to incrementally update memory.
     */
    @Column(name = "last_summarized_message_id")
    private Long lastSummarizedMessageId;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}


























