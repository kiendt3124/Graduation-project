package com.example.graduationproject.Repository;

import com.example.graduationproject.Entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    /** Lấy toàn bộ tin nhắn trong 1 session, theo thứ tự thời gian */
    List<ChatMessage> findByUserIdAndSessionIdOrderByCreatedAtAsc(UUID userId, UUID sessionId);

    /** Lấy danh sách session ID riêng biệt của user (mới nhất lên trước) */
    @Query("""
            SELECT DISTINCT c.sessionId FROM ChatMessage c
            WHERE c.user.id = :userId
            ORDER BY c.sessionId DESC
            """)
    List<UUID> findDistinctSessionIdsByUserId(@Param("userId") UUID userId);

    /** Lấy tin nhắn đầu tiên của mỗi session (để hiển thị preview) */
    @Query("""
            SELECT c FROM ChatMessage c
            WHERE c.user.id = :userId
              AND c.sessionId = :sessionId
              AND c.role = 'user'
            ORDER BY c.createdAt ASC
            LIMIT 1
            """)
    ChatMessage findFirstUserMessageInSession(
            @Param("userId") UUID userId,
            @Param("sessionId") UUID sessionId);

    /** Đếm số tin nhắn user đã gửi hôm nay (rate limiting) */
    long countByUserIdAndRoleAndCreatedAtAfter(UUID userId, String role, LocalDateTime after);

    /** Xóa tất cả tin nhắn trong session */
    void deleteByUserIdAndSessionId(UUID userId, UUID sessionId);
}
