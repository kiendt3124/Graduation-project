package com.example.graduationproject.Controller;

import com.example.graduationproject.Dto.Response.NotificationResponse;
import com.example.graduationproject.Service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification", description = "Quản lý thông báo trong ứng dụng")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    // ─── 1. Lấy danh sách thông báo ─────────────────────────────────────────

    @Operation(
        summary = "Danh sách thông báo",
        description = "Trả về tất cả thông báo của user, mới nhất lên trước.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực")
        }
    )
    @GetMapping
    public ResponseEntity<?> getNotifications(Authentication auth) {
        try {
            List<NotificationResponse> list = notificationService.getNotifications(auth.getName());
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            log.error("getNotifications error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ─── 2. Đếm thông báo chưa đọc ─────────────────────────────────────────

    @Operation(
        summary = "Số thông báo chưa đọc",
        description = "Trả về số lượng thông báo chưa đọc — dùng hiển thị badge trên icon chuông.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Thành công")
        }
    )
    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(Authentication auth) {
        try {
            long count = notificationService.getUnreadCount(auth.getName());
            return ResponseEntity.ok(Map.of("unreadCount", count));
        } catch (Exception e) {
            log.error("getUnreadCount error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ─── 3. Đánh dấu đã đọc (1 thông báo) ─────────────────────────────────

    @Operation(
        summary = "Đánh dấu đã đọc",
        description = "Đánh dấu 1 thông báo đã đọc theo ID.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy thông báo")
        }
    )
    @PatchMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(Authentication auth, @PathVariable UUID id) {
        try {
            NotificationResponse response = notificationService.markAsRead(auth.getName(), id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ─── 4. Đánh dấu tất cả đã đọc ─────────────────────────────────────────

    @Operation(
        summary = "Đánh dấu tất cả đã đọc",
        description = "Đánh dấu toàn bộ thông báo chưa đọc thành đã đọc.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Thành công")
        }
    )
    @PatchMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(Authentication auth) {
        try {
            notificationService.markAllAsRead(auth.getName());
            return ResponseEntity.ok(Map.of("message", "Đã đánh dấu tất cả đã đọc"));
        } catch (Exception e) {
            log.error("markAllAsRead error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
