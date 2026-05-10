package com.example.graduationproject.Controller;

import com.example.graduationproject.Dto.Request.AiChatRequest;
import com.example.graduationproject.Dto.Response.*;
import com.example.graduationproject.Service.AiChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI Chat", description = "Trợ lý AI tài chính — chỉ dành cho tài khoản Premium")
@SecurityRequirement(name = "bearerAuth")
public class AiChatController {

    private final AiChatService aiChatService;

    // ─── 1. Gửi tin nhắn ─────────────────────────────────────────────────────

    @Operation(
        summary = "Gửi tin nhắn cho AI",
        description = """
            Gửi tin nhắn và nhận phản hồi từ FinBot AI.
            - **Premium only**: Trả về 403 nếu tài khoản BASIC.
            - **sessionId**: null = bắt đầu conversation mới; có giá trị = tiếp tục conversation cũ.
            - **Quick input**: Gửi "Ăn trưa 50k" → AI tự động tạo giao dịch EXPENSE 50,000 VND.
            - **Giới hạn**: 50 tin nhắn/ngày.
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Thành công"),
            @ApiResponse(responseCode = "403", description = "Chỉ dành cho tài khoản Premium"),
            @ApiResponse(responseCode = "400", description = "Lỗi (hết quota, lỗi kết nối AI...)")
        }
    )
    @PostMapping("/chat")
    public ResponseEntity<?> chat(Authentication auth,
                                   @Valid @RequestBody AiChatRequest request) {
        try {
            AiChatResponse response = aiChatService.chat(auth.getName(), request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            if ("PREMIUM_REQUIRED".equals(e.getMessage())) {
                return ResponseEntity.status(403).body(Map.of(
                        "error", "Tính năng AI chỉ dành cho tài khoản Premium",
                        "upgrade", "POST /premium/initiate"
                ));
            }
            log.error("AI chat error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ─── 2. Danh sách sessions ────────────────────────────────────────────────

    @Operation(
        summary = "Danh sách phiên chat",
        description = "Trả về danh sách các phiên hội thoại với AI, kèm preview tin nhắn đầu tiên.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Thành công"),
            @ApiResponse(responseCode = "403", description = "Chỉ dành cho tài khoản Premium")
        }
    )
    @GetMapping("/sessions")
    public ResponseEntity<?> getSessions(Authentication auth) {
        try {
            List<ChatSessionResponse> sessions = aiChatService.getSessions(auth.getName());
            return ResponseEntity.ok(sessions);
        } catch (RuntimeException e) {
            if ("PREMIUM_REQUIRED".equals(e.getMessage())) {
                return ResponseEntity.status(403).body("Chỉ dành cho tài khoản Premium");
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ─── 3. Lịch sử chat của 1 session ───────────────────────────────────────

    @Operation(
        summary = "Lịch sử chat theo session",
        description = "Trả về toàn bộ tin nhắn (user + assistant) của một phiên hội thoại.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Thành công"),
            @ApiResponse(responseCode = "403", description = "Chỉ dành cho tài khoản Premium")
        }
    )
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<?> getSessionHistory(Authentication auth,
                                                @PathVariable UUID sessionId) {
        try {
            List<ChatMessageDto> messages = aiChatService.getSessionHistory(auth.getName(), sessionId);
            return ResponseEntity.ok(messages);
        } catch (RuntimeException e) {
            if ("PREMIUM_REQUIRED".equals(e.getMessage())) {
                return ResponseEntity.status(403).body("Chỉ dành cho tài khoản Premium");
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ─── 4. Xóa session ───────────────────────────────────────────────────────

    @Operation(
        summary = "Xóa phiên chat",
        description = "Xóa toàn bộ tin nhắn trong một phiên hội thoại.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Xóa thành công"),
            @ApiResponse(responseCode = "403", description = "Chỉ dành cho tài khoản Premium")
        }
    )
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<?> deleteSession(Authentication auth,
                                            @PathVariable UUID sessionId) {
        try {
            aiChatService.deleteSession(auth.getName(), sessionId);
            return ResponseEntity.ok(Map.of("message", "Đã xóa phiên chat"));
        } catch (RuntimeException e) {
            if ("PREMIUM_REQUIRED".equals(e.getMessage())) {
                return ResponseEntity.status(403).body("Chỉ dành cho tài khoản Premium");
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
