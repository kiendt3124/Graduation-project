package com.example.graduationproject.Repository;

import com.example.graduationproject.Entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /** Lấy tất cả thông báo của user, mới nhất lên trước */
    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /** Lấy thông báo chưa đọc */
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(UUID userId);

    /** Đếm số thông báo chưa đọc */
    long countByUserIdAndIsReadFalse(UUID userId);
}
