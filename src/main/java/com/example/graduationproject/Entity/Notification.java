package com.example.graduationproject.Entity;

import com.example.graduationproject.Entity.Enum.NotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    /**
     * ID tham chiếu đến đối tượng liên quan (budget, loan, goal, premium order...).
     * Nullable — thông báo hệ thống có thể không có reference.
     */
    @Column(name = "reference_id")
    private UUID referenceId;

    @Builder.Default
    @Column(name = "is_read", nullable = false, columnDefinition = "boolean default false")
    private Boolean isRead = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
