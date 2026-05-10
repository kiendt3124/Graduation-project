package com.example.graduationproject.Repository;

import com.example.graduationproject.Entity.Enum.TransactionType;
import com.example.graduationproject.Entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    // ─── Queries phục vụ Báo cáo & Thống kê ─────────────────────────────────

    /**
     * Tổng số tiền theo loại giao dịch (INCOME / EXPENSE) trong khoảng thời gian.
     * Tính gộp tất cả ví của user.
     */
    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM Transaction t
            WHERE t.wallet.user.id = :userId
              AND t.transactionType = :type
              AND t.isDeleted = false
              AND t.transactionDate >= :from
              AND t.transactionDate <= :to
            """)
    BigDecimal sumByTypeAndPeriod(
            @Param("userId") UUID userId,
            @Param("type") TransactionType type,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /**
     * Tổng số tiền nhóm theo danh mục trong tháng/năm.
     * Trả về: [categoryId, categoryName, totalAmount]
     */
    @Query("""
            SELECT t.category.id, t.category.name, COALESCE(SUM(t.amount), 0)
            FROM Transaction t
            WHERE t.wallet.user.id = :userId
              AND t.transactionType = :type
              AND t.isDeleted = false
              AND t.category IS NOT NULL
              AND MONTH(t.transactionDate) = :month
              AND YEAR(t.transactionDate)  = :year
            GROUP BY t.category.id, t.category.name
            ORDER BY COALESCE(SUM(t.amount), 0) DESC
            """)
    List<Object[]> sumGroupByCategory(
            @Param("userId") UUID userId,
            @Param("type") TransactionType type,
            @Param("month") int month,
            @Param("year") int year);

    /**
     * Tổng thu/chi nhóm theo ngày trong khoảng thời gian.
     * Trả về: [date (LocalDate), totalIncome, totalExpense]
     */
    @Query("""
            SELECT CAST(t.transactionDate AS LocalDate),
                   COALESCE(SUM(CASE WHEN t.transactionType = com.example.graduationproject.Entity.Enum.TransactionType.INCOME THEN t.amount ELSE 0 END), 0),
                   COALESCE(SUM(CASE WHEN t.transactionType = com.example.graduationproject.Entity.Enum.TransactionType.EXPENSE THEN t.amount ELSE 0 END), 0)
            FROM Transaction t
            WHERE t.wallet.user.id = :userId
              AND t.isDeleted = false
              AND t.transactionType IN (
                  com.example.graduationproject.Entity.Enum.TransactionType.INCOME,
                  com.example.graduationproject.Entity.Enum.TransactionType.EXPENSE
              )
              AND t.transactionDate >= :from
              AND t.transactionDate <= :to
            GROUP BY CAST(t.transactionDate AS LocalDate)
            ORDER BY CAST(t.transactionDate AS LocalDate) ASC
            """)
    List<Object[]> sumGroupByDay(
            @Param("userId") UUID userId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /**
     * Tổng thu/chi nhóm theo tháng trong năm.
     * Trả về: [month (Integer), totalIncome, totalExpense]
     */
    @Query("""
            SELECT MONTH(t.transactionDate),
                   COALESCE(SUM(CASE WHEN t.transactionType = com.example.graduationproject.Entity.Enum.TransactionType.INCOME THEN t.amount ELSE 0 END), 0),
                   COALESCE(SUM(CASE WHEN t.transactionType = com.example.graduationproject.Entity.Enum.TransactionType.EXPENSE THEN t.amount ELSE 0 END), 0)
            FROM Transaction t
            WHERE t.wallet.user.id = :userId
              AND t.isDeleted = false
              AND t.transactionType IN (
                  com.example.graduationproject.Entity.Enum.TransactionType.INCOME,
                  com.example.graduationproject.Entity.Enum.TransactionType.EXPENSE
              )
              AND YEAR(t.transactionDate) = :year
            GROUP BY MONTH(t.transactionDate)
            ORDER BY MONTH(t.transactionDate) ASC
            """)
    List<Object[]> sumGroupByMonth(
            @Param("userId") UUID userId,
            @Param("year") int year);

    // ─── Queries phục vụ Admin Thống kê toàn hệ thống ─────────────────────────

    /**
     * Top danh mục chi tiêu (EXPENSE) nhiều nhất toàn platform (tất cả user gộp lại).
     * Trả về: [categoryName, totalAmount, transactionCount]
     */
    @Query("""
            SELECT t.category.name, COALESCE(SUM(t.amount), 0), COUNT(t)
            FROM Transaction t
            WHERE t.transactionType = com.example.graduationproject.Entity.Enum.TransactionType.EXPENSE
              AND t.isDeleted = false
              AND t.category IS NOT NULL
            GROUP BY t.category.name
            ORDER BY COALESCE(SUM(t.amount), 0) DESC
            """)
    List<Object[]> topExpenseCategoriesAllUsers();

    /**
     * Tổng số tiền thu vào (INCOME) và chi ra (EXPENSE) of the entire platform
     * trong tháng/năm chỉ định.
     * Trả về: [totalIncome, totalExpense]
     */
    @Query("""
            SELECT
              COALESCE(SUM(CASE WHEN t.transactionType = com.example.graduationproject.Entity.Enum.TransactionType.INCOME THEN t.amount ELSE 0 END), 0),
              COALESCE(SUM(CASE WHEN t.transactionType = com.example.graduationproject.Entity.Enum.TransactionType.EXPENSE THEN t.amount ELSE 0 END), 0)
            FROM Transaction t
            WHERE t.isDeleted = false
              AND MONTH(t.transactionDate) = :month
              AND YEAR(t.transactionDate)  = :year
            """)
    Object[] platformIncomeExpenseByMonth(@Param("month") int month, @Param("year") int year);
}

