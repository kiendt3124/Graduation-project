package com.ptit.expensetracker.backend.ai.repository;

import com.ptit.expensetracker.backend.ai.domain.AiChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface AiChatMessageRepository extends JpaRepository<AiChatMessage, Long> {

    List<AiChatMessage> findTop50ByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(String userId);

    List<AiChatMessage> findTop12ByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(String userId);

    List<AiChatMessage> findByUserIdAndIdGreaterThanOrderByIdAsc(String userId, Long id);

    @Modifying
    @Transactional
    @Query("update AiChatMessage m set m.deletedAt = :deletedAt where m.userId = :userId and m.deletedAt is null")
    int softDeleteAll(@Param("userId") String userId, @Param("deletedAt") OffsetDateTime deletedAt);

    @Modifying
    @Transactional
    void deleteByUserId(String userId);
}



