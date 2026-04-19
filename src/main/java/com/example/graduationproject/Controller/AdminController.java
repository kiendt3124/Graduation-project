package com.example.graduationproject.Controller;

import com.example.graduationproject.Dto.Response.*;
import com.example.graduationproject.Service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Quản trị hệ thống — chỉ dành cho tài khoản Admin")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminService adminService;

    // ===================================================
    // 1. QUẢN LÝ NGƯỜI DÙNG
    // ===================================================

    @Operation(
        summary = "Danh sách người dùng",
        description = """
            Trả về danh sách tất cả người dùng trong hệ thống (có phân trang).
            - `email` (tùy chọn): tìm kiếm gần đúng theo email
            - `page`: số trang (bắt đầu từ 0)
            - `size`: số phần tử mỗi trang (mặc định 20)
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Thành công"),
            @ApiResponse(responseCode = "403", description = "Không có quyền Admin")
        }
    )
    @GetMapping("/users")
    public ResponseEntity<?> getUsers(
            @Parameter(description = "Lọc theo email (tìm kiếm gần đúng)", example = "example@gmail.com")
            @RequestParam(required = false) String email,
            @Parameter(description = "Số trang (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số phần tử mỗi trang", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<AdminUserResponse> result = adminService.getUsers(email, page, size);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Admin getUsers error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(
        summary = "Chi tiết người dùng",
        description = "Trả về thông tin chi tiết của một user, bao gồm số lượng ví, giao dịch, khoản vay, mục tiêu, ngân sách và đơn Premium.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Thành công",
                content = @Content(schema = @Schema(implementation = AdminUserDetailResponse.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy user"),
            @ApiResponse(responseCode = "403", description = "Không có quyền Admin")
        }
    )
    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getUserDetail(
            @Parameter(description = "UUID của user", required = true)
            @PathVariable UUID userId) {
        try {
            return ResponseEntity.ok(adminService.getUserDetail(userId));
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            log.error("Admin getUserDetail error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(
        summary = "Ban tài khoản người dùng",
        description = """
            Vô hiệu hóa tài khoản người dùng. User bị ban sẽ không thể đăng nhập.
            Dữ liệu tài chính của user **không bị xóa**.
            Không thể ban tài khoản Admin.
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Ban thành công"),
            @ApiResponse(responseCode = "400", description = "Lỗi (ví dụ: cố ban Admin)"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy user"),
            @ApiResponse(responseCode = "403", description = "Không có quyền Admin")
        }
    )
    @PutMapping("/users/{userId}/ban")
    public ResponseEntity<?> banUser(
            @Parameter(description = "UUID của user cần ban", required = true)
            @PathVariable UUID userId) {
        try {
            AdminUserResponse result = adminService.setBanStatus(userId, true);
            return ResponseEntity.ok(result);
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Admin banUser error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(
        summary = "Unban tài khoản người dùng",
        description = "Khôi phục quyền đăng nhập cho tài khoản đã bị ban.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Unban thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy user"),
            @ApiResponse(responseCode = "403", description = "Không có quyền Admin")
        }
    )
    @PutMapping("/users/{userId}/unban")
    public ResponseEntity<?> unbanUser(
            @Parameter(description = "UUID của user cần unban", required = true)
            @PathVariable UUID userId) {
        try {
            AdminUserResponse result = adminService.setBanStatus(userId, false);
            return ResponseEntity.ok(result);
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            log.error("Admin unbanUser error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ===================================================
    // 2. THỐNG KÊ HỆ THỐNG (Dashboard)
    // ===================================================

    @Operation(
        summary = "Thống kê tổng quan hệ thống",
        description = """
            Trả về các số liệu vận hành hệ thống cho Admin Dashboard:
            - Tổng số user, phân chia theo BASIC/PREMIUM/Banned
            - Tổng đơn Premium, đơn thành công, tổng doanh thu
            - Số user mới đăng ký trong 7 ngày gần nhất (theo từng ngày)
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Thành công",
                content = @Content(schema = @Schema(implementation = AdminStatsResponse.class))),
            @ApiResponse(responseCode = "403", description = "Không có quyền Admin")
        }
    )
    @GetMapping("/stats")
    public ResponseEntity<?> getSystemStats() {
        try {
            return ResponseEntity.ok(adminService.getSystemStats());
        } catch (Exception e) {
            log.error("Admin getSystemStats error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ===================================================
    // 3. QUẢN LÝ ĐƠN PREMIUM
    // ===================================================

    @Operation(
        summary = "Danh sách đơn Premium",
        description = """
            Trả về tất cả đơn mua Premium trong hệ thống (có phân trang).
            Lọc theo `status`: `PENDING`, `COMPLETED`, `FAILED`, `EXPIRED`.
            Để lại trống để lấy tất cả.
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Thành công"),
            @ApiResponse(responseCode = "400", description = "Giá trị status không hợp lệ"),
            @ApiResponse(responseCode = "403", description = "Không có quyền Admin")
        }
    )
    @GetMapping("/premium/orders")
    public ResponseEntity<?> getPremiumOrders(
            @Parameter(description = "Lọc theo trạng thái: PENDING | COMPLETED | FAILED | EXPIRED", example = "COMPLETED")
            @RequestParam(required = false) String status,
            @Parameter(description = "Số trang (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số phần tử mỗi trang", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<AdminPremiumOrderResponse> result = adminService.getPremiumOrders(status, page, size);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Admin getPremiumOrders error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(
        summary = "Thống kê doanh thu Premium",
        description = """
            Trả về thống kê doanh thu từ các đơn Premium đã thanh toán (COMPLETED):
            - Tổng doanh thu toàn thời gian
            - Doanh thu tháng hiện tại
            - Doanh thu năm được chỉ định
            - Breakdown theo từng tháng trong năm
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Thành công",
                content = @Content(schema = @Schema(implementation = AdminRevenueResponse.class))),
            @ApiResponse(responseCode = "403", description = "Không có quyền Admin")
        }
    )
    @GetMapping("/premium/revenue")
    public ResponseEntity<?> getRevenue(
            @Parameter(description = "Năm cần xem doanh thu", example = "2026")
            @RequestParam(defaultValue = "2026") int year) {
        try {
            return ResponseEntity.ok(adminService.getRevenue(year));
        } catch (Exception e) {
            log.error("Admin getRevenue error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
