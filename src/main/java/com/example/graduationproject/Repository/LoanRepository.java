package com.example.graduationproject.Repository;

import com.example.graduationproject.Entity.Enum.LoanStatus;
import com.example.graduationproject.Entity.Enum.LoanType;
import com.example.graduationproject.Entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoanRepository extends JpaRepository<Loan, UUID> {

    /** Tất cả loan chưa xoá của user */
    List<Loan> findByUserIdAndIsDeletedFalse(UUID userId);

    /** Filter theo loại (BORROW / LEND) */
    List<Loan> findByUserIdAndLoanTypeAndIsDeletedFalse(UUID userId, LoanType loanType);

    /** Filter theo trạng thái */
    List<Loan> findByUserIdAndStatusAndIsDeletedFalse(UUID userId, LoanStatus status);

    /** Filter theo cả loại và trạng thái */
    List<Loan> findByUserIdAndLoanTypeAndStatusAndIsDeletedFalse(
            UUID userId, LoanType loanType, LoanStatus status);

    /** Tìm 1 loan chưa xoá theo id */
    Optional<Loan> findByIdAndIsDeletedFalse(UUID id);

    // ─── Queries phục vụ Báo cáo & Thống kê ─────────────────────────────────

    /** Đếm số khoản vay theo trạng thái */
    long countByUserIdAndStatusAndIsDeletedFalse(UUID userId, LoanStatus status);

    /**
     * Tổng số tiền gốc (principal) của các khoản vay đang ACTIVE theo loại.
     * Dùng để tính: tổng đang đi vay (BORROW) và tổng đang cho vay (LEND).
     */
    @Query("""
            SELECT COALESCE(SUM(l.principalAmount), 0)
            FROM Loan l
            WHERE l.user.id = :userId
              AND l.loanType = :loanType
              AND l.status   = com.example.graduationproject.Entity.Enum.LoanStatus.ACTIVE
              AND l.isDeleted = false
            """)
    BigDecimal sumPrincipalByUserAndType(
            @Param("userId") UUID userId,
            @Param("loanType") LoanType loanType);

    // ─── Queries phục vụ Scheduler ──────────────────────────────────────────

    /** Tìm loan ACTIVE đã quá hạn (dùng cho scheduler cập nhật OVERDUE) */
    List<Loan> findByStatusAndIsDeletedFalseAndDueDateBefore(
            LoanStatus status, java.time.LocalDate date);

    /** Tìm loan ACTIVE sắp đến hạn trong khoảng (dùng cho nhắc nhở LOAN_DUE_SOON) */
    List<Loan> findByStatusAndIsDeletedFalseAndDueDateBetween(
            LoanStatus status, java.time.LocalDate from, java.time.LocalDate to);
}

