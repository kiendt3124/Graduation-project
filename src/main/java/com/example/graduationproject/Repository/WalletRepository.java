package com.example.graduationproject.Repository;

import com.example.graduationproject.Entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;


@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    /** Lấy tất cả ví của user */
    List<Wallet> findByUserId(UUID userId);

    /**
     * Tổng số dư tất cả ví của user.
     * Dùng để hiển thị tổng tài sản trong báo cáo tổng quan.
     */
    @Query("""
            SELECT COALESCE(SUM(w.balance), 0)
            FROM Wallet w
            WHERE w.user.id = :userId
            """)
    BigDecimal sumBalanceByUserId(@Param("userId") UUID userId);
}

