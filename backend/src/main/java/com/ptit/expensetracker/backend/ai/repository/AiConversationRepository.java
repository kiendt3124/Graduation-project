package com.ptit.expensetracker.backend.ai.repository;

import com.ptit.expensetracker.backend.ai.domain.AiConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {
    Optional<AiConversation> findByUserId(String userId);
}


























