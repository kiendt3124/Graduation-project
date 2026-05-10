package com.example.graduationproject.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chat_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Nhóm các tin nhắn thành 1 phiên hội thoại.
     * Client tự tạo hoặc nhận từ response đầu tiên.
     */
    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    /**
     * "user" = tin nhắn người dùng gửi
     * "assistant" = câu trả lời của AI
     */
    @Column(nullable = false, length = 20)
    private String role;

    /**
     * Nội dung tin nhắn (có thể rất dài).
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
