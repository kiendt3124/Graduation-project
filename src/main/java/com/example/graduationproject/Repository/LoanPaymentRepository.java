package com.example.graduationproject.Repository;

import com.example.graduationproject.Entity.LoanPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface LoanPaymentRepository extends JpaRepository<LoanPayment, UUID> {

    /** Lịch sử thanh toán của 1 loan, mới nhất lên trước */
    List<LoanPayment> findByLoanIdOrderByPaymentDateDesc(UUID loanId);

    /**
     * Tổng số tiền đã thanh toán của 1 loan.
     * COALESCE để trả về 0 nếu chưa có payment nào.
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM LoanPayment p WHERE p.loan.id = :loanId")
    BigDecimal sumPaidAmountByLoanId(@Param("loanId") UUID loanId);
}
