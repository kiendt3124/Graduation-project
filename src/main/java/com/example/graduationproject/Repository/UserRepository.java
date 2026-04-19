package com.example.graduationproject.Repository;

import com.example.graduationproject.Entity.Enum.AccountTier;
import com.example.graduationproject.Entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByRefreshToken(String refreshToken);

    // ─── Admin queries ────────────────────────────────────────────────────────

    /** Tìm kiếm user theo email (LIKE, không phân biệt hoa thường), có phân trang. */
    Page<User> findByEmailContainingIgnoreCase(String email, Pageable pageable);

    /** Đếm user theo tier. */
    long countByAccountTier(AccountTier tier);

    /** Đếm user bị banned. */
    @Query("SELECT COUNT(u) FROM User u WHERE u.isBanned = true")
    long countBannedUsers();

    /**
     * Đếm số user tạo mới theo ngày trong khoảng thời gian.
     * Trả về: [date (LocalDate as String), count]
     */
    @Query("""
            SELECT CAST(u.createdAt AS LocalDate), COUNT(u)
            FROM User u
            WHERE u.createdAt >= :from
              AND u.createdAt <= :to
            GROUP BY CAST(u.createdAt AS LocalDate)
            ORDER BY CAST(u.createdAt AS LocalDate) ASC
            """)
    List<Object[]> countNewUsersByDay(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}

