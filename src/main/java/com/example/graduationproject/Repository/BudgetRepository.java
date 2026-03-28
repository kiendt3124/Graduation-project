package com.example.graduationproject.Repository;

import com.example.graduationproject.Entity.Budget;
import com.example.graduationproject.Entity.Enum.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    /**
     * Tìm budget theo user + category + tháng + năm.
     * Dùng để kiểm tra duplicate khi tạo mới.
     */
    Optional<Budget> findByUserIdAndCategoryIdAndMonthAndYear(
            UUID userId, UUID categoryId, int month, int year);

    /**
     * Lấy tất cả budget của user trong 1 tháng/năm cụ thể.
     */
    List<Budget> findByUserIdAndMonthAndYear(UUID userId, int month, int year);

    /**
     * Lấy toàn bộ budget của user (mọi tháng, mọi năm).
     */
    List<Budget> findByUserId(UUID userId);

    /**
     * Kiểm tra đã tồn tại budget cho (user, category, tháng, năm) chưa.
     */
    boolean existsByUserIdAndCategoryIdAndMonthAndYear(
            UUID userId, UUID categoryId, int month, int year);

    /**
     * Tính tổng số tiền EXPENSE của một category trong tháng/năm.
     * Đây là core query cho Option A — tính spent real-time từ transactions.
     *
     * Chỉ tính transaction:
     * - Thuộc ví của user (wallet.user.id = userId)
     * - Thuộc category đang xét
     * - Loại EXPENSE
     * - Chưa bị xóa (isDeleted = false)
     * - Trong đúng tháng và năm của transaction_date
     */
    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM Transaction t
            WHERE t.wallet.user.id = :userId
              AND t.category.id   = :categoryId
              AND t.transactionType = com.example.graduationproject.Entity.Enum.TransactionType.EXPENSE
              AND t.isDeleted = false
              AND MONTH(t.transactionDate) = :month
              AND YEAR(t.transactionDate)  = :year
            """)
    BigDecimal calculateSpent(
            @Param("userId") UUID userId,
            @Param("categoryId") UUID categoryId,
            @Param("month") int month,
            @Param("year") int year);
}
