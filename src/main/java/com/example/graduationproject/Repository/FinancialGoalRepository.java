package com.example.graduationproject.Repository;

import com.example.graduationproject.Entity.FinancialGoal;
import com.example.graduationproject.Entity.Enum.GoalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FinancialGoalRepository extends JpaRepository<FinancialGoal, UUID> {

    /** Tất cả goal chưa xoá của user */
    List<FinancialGoal> findByUserIdAndIsDeletedFalse(UUID userId);

    /** Filter theo trạng thái */
    List<FinancialGoal> findByUserIdAndStatusAndIsDeletedFalse(UUID userId, GoalStatus status);

    /** Tìm 1 goal chưa xoá theo id */
    Optional<FinancialGoal> findByIdAndIsDeletedFalse(UUID id);

    /** Đếm goal theo trạng thái (dùng cho báo cáo tổng quan) */
    long countByUserIdAndStatusAndIsDeletedFalse(UUID userId, GoalStatus status);

    /** Đếm tổng goal của user (cho admin stats) */
    long countByUserId(UUID userId);
}

