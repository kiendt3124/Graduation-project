package com.ptit.expensetracker.backend.ai.controller;

import com.ptit.expensetracker.backend.ai.dto.context.FinancialContextDto;
import com.ptit.expensetracker.backend.ai.service.ContextSyncService;
import com.ptit.expensetracker.backend.ai.service.UserContextService;
import com.ptit.expensetracker.backend.common.exception.UnauthorizedException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class ContextController {

    private final ContextSyncService contextSyncService;
    private final UserContextService userContextService;

    /**
     * Sync financial context from client (Android) to backend.
     */
    @PostMapping("/sync-context")
    public ResponseEntity<Void> syncContext(
            Authentication authentication,
            @Valid @RequestBody FinancialContextDto body
    ) {
        String userId = authentication.getName();
        contextSyncService.sync(userId, body);
        return ResponseEntity.ok().build();
    }

    /**
     * Retrieve the latest stored financial context for the authenticated user.
     * Path userId must match authenticated user to avoid data leak.
     */
    @GetMapping("/context")
    public ResponseEntity<FinancialContextDto> getContext(
            Authentication authentication
    ) {
        if (!authentication.isAuthenticated()) {
            throw new UnauthorizedException("Unauthorized");
        }
        String userId = authentication.getName();
        return ResponseEntity.ok(userContextService.getContext(userId));
    }
}

