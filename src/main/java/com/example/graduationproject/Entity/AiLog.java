package com.example.graduationproject.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * User thực hiện request (nullable cho system calls).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * Loại request: CHAT, QUICK_INPUT, ANALYSIS
     */
    @Column(name = "request_type", nullable = false, length = 30)
    private String requestType;

    /**
     * Model AI được sử dụng (gemini-2.0-flash).
     */
    @Column(nullable = false, length = 50)
    private String model;

    /** Token của prompt gửi lên */
    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    /** Token của response nhận về */
    @Column(name = "response_tokens")
    private Integer responseTokens;

    /** SUCCESS hoặc ERROR */
    @Column(nullable = false, length = 20)
    private String status;

    /** Nội dung lỗi nếu status = ERROR */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /** Thời gian xử lý (ms) */
    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
