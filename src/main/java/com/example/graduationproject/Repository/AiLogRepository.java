package com.example.graduationproject.Repository;

import com.example.graduationproject.Entity.AiLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface AiLogRepository extends JpaRepository<AiLog, UUID> {

    /** Admin: tất cả logs có phân trang */
    Page<AiLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Admin: lọc theo status */
    Page<AiLog> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    /** Thống kê: đếm theo status */
    long countByStatus(String status);

    /** Thống kê: đếm error trong khoảng thời gian */
    long countByStatusAndCreatedAtAfter(String status, LocalDateTime after);

    /** Thống kê: tổng token đã dùng */
    @Query("SELECT COALESCE(SUM(l.promptTokens + l.responseTokens), 0) FROM AiLog l WHERE l.status = 'SUCCESS'")
    long sumTotalTokens();

    /** Thống kê: avg duration */
    @Query("SELECT COALESCE(AVG(l.durationMs), 0) FROM AiLog l WHERE l.status = 'SUCCESS'")
    Double avgDurationMs();

    /** Thống kê: request theo ngày (30 ngày gần nhất) */
    @Query("""
            SELECT CAST(l.createdAt AS LocalDate), COUNT(l)
            FROM AiLog l
            WHERE l.createdAt >= :from
            GROUP BY CAST(l.createdAt AS LocalDate)
            ORDER BY CAST(l.createdAt AS LocalDate) ASC
            """)
    java.util.List<Object[]> countRequestsByDay(@Param("from") LocalDateTime from);
}
