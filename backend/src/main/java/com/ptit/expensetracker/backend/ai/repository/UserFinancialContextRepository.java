package com.ptit.expensetracker.backend.ai.repository;

import com.ptit.expensetracker.backend.ai.domain.UserFinancialContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserFinancialContextRepository extends JpaRepository<UserFinancialContext, Long> {
    Optional<UserFinancialContext> findByUserId(String userId);
}

