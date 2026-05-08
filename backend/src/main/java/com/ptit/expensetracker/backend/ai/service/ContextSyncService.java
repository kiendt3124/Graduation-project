package com.ptit.expensetracker.backend.ai.service;

import com.ptit.expensetracker.backend.ai.dto.context.FinancialContextDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContextSyncService {

    private final UserContextService userContextService;

    @Transactional
    public void sync(String userId, FinancialContextDto dto) {
        userContextService.saveContext(userId, dto);
    }
}

