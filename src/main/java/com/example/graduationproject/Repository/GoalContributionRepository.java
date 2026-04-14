package com.example.graduationproject.Repository;

import com.example.graduationproject.Entity.GoalContribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface GoalContributionRepository extends JpaRepository<GoalContribution, UUID> {

    /** Lịch sử góp tiền của 1 goal, mới nhất lên trước */
    List<GoalContribution> findByGoalIdOrderByContributionDateDesc(UUID goalId);

    /** Tổng tiền đã góp vào goal (dùng để đồng bộ current_amount) */
    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM GoalContribution c WHERE c.goal.id = :goalId")
    BigDecimal sumContributedAmountByGoalId(@Param("goalId") UUID goalId);
}
