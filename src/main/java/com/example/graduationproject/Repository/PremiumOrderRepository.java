package com.example.graduationproject.Repository;

import com.example.graduationproject.Entity.Enum.PremiumOrderStatus;
import com.example.graduationproject.Entity.PremiumOrder;
import com.example.graduationproject.Entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PremiumOrderRepository extends JpaRepository<PremiumOrder, UUID> {

    Optional<PremiumOrder> findByTxnRef(String txnRef);

    boolean existsByTxnRefAndStatus(String txnRef, PremiumOrderStatus status);

    List<PremiumOrder> findByUserOrderByCreatedAtDesc(User user);

    // ─── Admin queries ────────────────────────────────────────────────────────

    /** Tất cả đơn hàng, có phân trang và lọc theo status. */
    Page<PremiumOrder> findByStatus(PremiumOrderStatus status, Pageable pageable);

    /** Tất cả đơn hàng không lọc status, có phân trang. */
    Page<PremiumOrder> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Đếm tổng đơn theo status. */
    long countByStatus(PremiumOrderStatus status);

    /** Tổng doanh thu từ các đơn COMPLETED. */
    @Query("SELECT COALESCE(SUM(o.amount), 0) FROM PremiumOrder o WHERE o.status = com.example.graduationproject.Entity.Enum.PremiumOrderStatus.COMPLETED")
    Long sumCompletedRevenue();

    /**
     * Doanh thu theo tháng/năm.
     * Trả về: [year, month, totalRevenue, orderCount]
     */
    @Query("""
            SELECT YEAR(o.paidAt), MONTH(o.paidAt), COALESCE(SUM(o.amount), 0), COUNT(o)
            FROM PremiumOrder o
            WHERE o.status = com.example.graduationproject.Entity.Enum.PremiumOrderStatus.COMPLETED
              AND YEAR(o.paidAt) = :year
            GROUP BY YEAR(o.paidAt), MONTH(o.paidAt)
            ORDER BY MONTH(o.paidAt) ASC
            """)
    List<Object[]> revenueByMonth(@Param("year") int year);

    // ─── Queries phục vụ Scheduler ──────────────────────────────────────────

    /** Tìm đơn PENDING đã quá hạn (dùng cho scheduler expire) */
    List<PremiumOrder> findByStatusAndExpiredAtBefore(
            PremiumOrderStatus status, java.time.LocalDateTime dateTime);
}

