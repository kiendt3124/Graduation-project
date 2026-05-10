package com.example.graduationproject.Service;

import com.example.graduationproject.Dto.Response.NotificationResponse;
import com.example.graduationproject.Entity.Enum.NotificationType;
import com.example.graduationproject.Entity.Notification;
import com.example.graduationproject.Entity.User;
import com.example.graduationproject.Repository.NotificationRepository;
import com.example.graduationproject.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    // ─── CREATE (dùng nội bộ bởi ScheduledTaskService) ──────────────────────

    /**
     * Tạo thông báo cho user.
     * Được gọi bởi ScheduledTaskService khi phát hiện sự kiện cần thông báo.
     */
    @Transactional
    public Notification createNotification(UUID userId, NotificationType type,
                                           String title, String message, UUID referenceId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .referenceId(referenceId)
                .build();

        notificationRepository.save(notification);
        log.debug("Tạo notification [{}] cho user {}: {}", type, user.getEmail(), title);
        return notification;
    }

    // ─── GET LIST ───────────────────────────────────────────────────────────

    /**
     * Lấy tất cả thông báo của user (mới nhất lên trước).
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(String email) {
        User user = getUserByEmail(email);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─── UNREAD COUNT ───────────────────────────────────────────────────────

    /**
     * Đếm số thông báo chưa đọc.
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(String email) {
        User user = getUserByEmail(email);
        return notificationRepository.countByUserIdAndIsReadFalse(user.getId());
    }

    // ─── MARK AS READ ───────────────────────────────────────────────────────

    /**
     * Đánh dấu 1 thông báo đã đọc.
     */
    @Transactional
    public NotificationResponse markAsRead(String email, UUID notificationId) {
        User user = getUserByEmail(email);
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Thông báo không tồn tại"));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Không có quyền truy cập thông báo này");
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
        return toResponse(notification);
    }

    /**
     * Đánh dấu tất cả thông báo đã đọc.
     */
    @Transactional
    public void markAllAsRead(String email) {
        User user = getUserByEmail(email);
        List<Notification> unread = notificationRepository
                .findByUserIdAndIsReadFalseOrderByCreatedAtDesc(user.getId());

        unread.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(unread);
        log.info("Đánh dấu {} thông báo đã đọc cho user {}", unread.size(), email);
    }

    // ─── HELPER ─────────────────────────────────────────────────────────────

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .referenceId(n.getReferenceId())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build();
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }
}
