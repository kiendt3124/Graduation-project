package com.example.graduationproject.Dto.Response;

import com.example.graduationproject.Entity.Enum.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private UUID id;
    private NotificationType type;
    private String title;
    private String message;
    private UUID referenceId;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
