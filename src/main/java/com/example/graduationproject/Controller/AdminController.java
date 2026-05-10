package com.example.graduationproject.Controller;

import com.example.graduationproject.Dto.Request.CreateCategoryRequest;
import com.example.graduationproject.Dto.Request.PartnerRequest;
import com.example.graduationproject.Dto.Request.UpdateCategoryRequest;
import com.example.graduationproject.Dto.Response.*;
import com.example.graduationproject.Repository.AiLogRepository;
import com.example.graduationproject.Service.AdminService;
import com.example.graduationproject.Service.CategoryService;
import com.example.graduationproject.Service.PartnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
    private final CategoryService categoryService;
    private final PartnerService partnerService;
    private final AiLogRepository aiLogRepository;

    // ===================================================
    // 1. QUẢN LÝ NGƯỜI DÙNG
    // ===================================================

    @Operation(summary = "Danh sách người dùng", description = """
            Trả về danh sách tất cả người dùng trong hệ thống (có phân trang).
            - `email` (tùy chọn): tìm kiếm gần đúng theo email
            - `page`: số trang (bắt đầu từ 0)
            - `size`: số phần tử mỗi trang (mặc định 20)
            """, responses = {
            @ApiResponse(responseCode = "200", description = "Thành công"),
            @ApiResponse(responseCode = "403", description = "Không có quyền Admin")
    })
    @GetMapping("/users")
    public ResponseEntity<?> getUsers(
            @Parameter(description = "Lọc theo email (tìm kiếm gần đúng)", example = "example@gmail.com") @RequestParam(required = false) String email,
            @Parameter(description = "Số trang (0-indexed)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số phần tử mỗi trang", example = "20") @RequestParam(defaultValue = "20") int size) {
        try {
            Page<AdminUserResponse> result = adminService.getUsers(email, page, size);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Admin getUsers error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Chi tiết người dùng", description = "Trả về thông tin chi tiết của một user, bao gồm số lượng ví, giao dịch, khoản vay, mục tiêu, ngân sách và đơn Premium.", responses = {
            @ApiResponse(responseCode = "200", description = "Thành công", content = @Content(schema = @Schema(implementation = AdminUserDetailResponse.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy user"),
            @ApiResponse(responseCode = "403", description = "Không có quyền Admin")
    })
    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getUserDetail(
            @Parameter(description = "UUID của user", required = true) @PathVariable UUID userId) {
        try {
            return ResponseEntity.ok(adminService.getUserDetail(userId));
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            log.error("Admin getUserDetail error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Ban tài khoản người dùng", description = """
            Vô hiệu hóa tài khoản người dùng. User bị ban sẽ không thể đăng nhập.
            Dữ liệu tài chính của user **không bị xóa**.
            Không thể ban tài khoản Admin.
            """, responses = {
            @ApiResponse(responseCode = "200", description = "Ban thành công"),
            @ApiResponse(responseCode = "400", description = "Lỗi (ví dụ: cố ban Admin)"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy user"),
            @ApiResponse(responseCode = "403", description = "Không có quyền Admin")
    })
    @PutMapping("/users/{userId}/ban")
    public ResponseEntity<?> banUser(
            @Parameter(description = "UUID của user cần ban", required = true) @PathVariable UUID userId) {
        try {
            return ResponseEntity.ok(adminService.setBanStatus(userId, true));
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Admin banUser error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Unban tài khoản người dùng", description = "Khôi phục quyền đăng nhập cho tài khoản đã bị ban.", responses = {
            @ApiResponse(responseCode = "200", description = "Unban thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy user"),
            @ApiResponse(responseCode = "403", description = "Không có quyền Admin")
    })
    @PutMapping("/users/{userId}/unban")
    public ResponseEntity<?> unbanUser(
            @Parameter(description = "UUID của user cần unban", required = true) @PathVariable UUID userId) {
        try {
            return ResponseEntity.ok(adminService.setBanStatus(userId, false));
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

    @Operation(summary = "Thống kê tổng quan hệ thống", description = """
            Trả về các số liệu vận hành hệ thống cho Admin Dashboard:
            - Tổng số user, phân chia theo BASIC/PREMIUM/Banned
            - Tổng đơn Premium, đơn thành công, tổng doanh thu
            - Số user mới đăng ký trong **7 ngày** và **30 ngày** gần nhất (theo từng ngày)
            - Tần suất sử dụng tính năng: số lượng giao dịch, ví, ngân sách, khoản vay, mục tiêu
            """, responses = {
            @ApiResponse(responseCode = "200", description = "Thành công", content = @Content(schema = @Schema(implementation = AdminStatsResponse.class))),
            @ApiResponse(responseCode = "403", description = "Không có quyền Admin")
    })
    @GetMapping("/stats")
    public ResponseEntity<?> getSystemStats() {
        try {
            return ResponseEntity.ok(adminService.getSystemStats());
        } catch (Exception e) {
            log.error("Admin getSystemStats error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Thống kê xu hướng nâng cao", description = """
            Trả về dữ liệu phân tích nâng cao cho Admin:
            - **featureUsage**: tổng số giao dịch, ví, ngân sách, khoản vay, mục tiêu toàn hệ thống
            - **newUsersByMonth**: số user mới theo từng tháng trong năm chỉ định
            - **currentMonthFinancial**: tổng thu/chi toàn platform tháng hiện tại, thu/chi TB/user
            - **topExpenseCategories**: top 5 danh mục chi tiêu nhiều nhất toàn platform (ẩn danh)
            - **totalMoneyInSystem**: tổng số dư tất cả ví trong hệ thống
            """, responses = {
            @ApiResponse(responseCode = "200", description = "Thành công", content = @Content(schema = @Schema(implementation = AdminTrendsResponse.class))),
            @ApiResponse(responseCode = "403", description = "Không có quyền Admin")
    })
    @GetMapping("/stats/trends")
    public ResponseEntity<?> getTrends(
            @Parameter(description = "Năm cần xem thống kê tăng trưởng user", example = "2026") @RequestParam(defaultValue = "2026") int year) {
        try {
            return ResponseEntity.ok(adminService.getTrends(year));
        } catch (Exception e) {
            log.error("Admin getTrends error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ===================================================
    // 3. QUẢN LÝ ĐƠN PREMIUM
    // ===================================================

    @Operation(summary = "Danh sách đơn Premium", description = """
            Trả về tất cả đơn mua Premium trong hệ thống (có phân trang).
            Lọc theo `status`: `PENDING`, `COMPLETED`, `FAILED`, `EXPIRED`.
            Để lại trống để lấy tất cả.
            """, responses = {
            @ApiResponse(responseCode = "200", description = "Thành công"),
            @ApiResponse(responseCode = "400", description = "Giá trị status không hợp lệ"),
            @ApiResponse(responseCode = "403", description = "Không có quyền Admin")
    })
    @GetMapping("/premium/orders")
    public ResponseEntity<?> getPremiumOrders(
            @Parameter(description = "Lọc theo trạng thái: PENDING | COMPLETED | FAILED | EXPIRED", example = "COMPLETED") @RequestParam(required = false) String status,
            @Parameter(description = "Số trang (0-indexed)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số phần tử mỗi trang", example = "20") @RequestParam(defaultValue = "20") int size) {
        try {
            return ResponseEntity.ok(adminService.getPremiumOrders(status, page, size));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Admin getPremiumOrders error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Thống kê doanh thu Premium", description = """
            Trả về thống kê doanh thu từ các đơn Premium đã thanh toán (COMPLETED):
            - Tổng doanh thu toàn thời gian
            - Doanh thu tháng hiện tại
            - Doanh thu năm được chỉ định
            - Breakdown theo từng tháng trong năm
            """, responses = {
            @ApiResponse(responseCode = "200", description = "Thành công", content = @Content(schema = @Schema(implementation = AdminRevenueResponse.class))),
            @ApiResponse(responseCode = "403", description = "Không có quyền Admin")
    })
    @GetMapping("/premium/revenue")
    public ResponseEntity<?> getRevenue(
            @Parameter(description = "Năm cần xem doanh thu", example = "2026") @RequestParam(defaultValue = "2026") int year) {
        try {
            return ResponseEntity.ok(adminService.getRevenue(year));
        } catch (Exception e) {
            log.error("Admin getRevenue error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ===================================================
    // 4. QUẢN LÝ DANH MỤC HỆ THỐNG (Category)
    // ===================================================

    @Operation(summary = "Danh sách tất cả danh mục", description = "Trả về tất cả danh mục hệ thống (INCOME + EXPENSE). Có thể filter theo `type`: `INCOME` hoặc `EXPENSE`.", responses = {
            @ApiResponse(responseCode = "200", description = "Thành công", content = @Content(array = @ArraySchema(schema = @Schema(implementation = CategoryResponse.class)))),
            @ApiResponse(responseCode = "403", description = "Không có quyền Admin")
    })
    @GetMapping("/categories")
    public ResponseEntity<?> getCategories() {
        try {
            return ResponseEntity.ok(categoryService.getAllCategories());
        } catch (Exception e) {
            log.error("Admin getCategories error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Tạo danh mục hệ thống", description = """
            Tạo danh mục thu/chi mặc định của hệ thống.
            - `name`: tên danh mục (bắt buộc)
            - `icon`: emoji hoặc tên icon (tùy chọn)
            - `transactionType`: `INCOME` hoặc `EXPENSE` (bắt buộc)
            """, responses = {
            @ApiResponse(responseCode = "200", description = "Tạo thành công", content = @Content(schema = @Schema(implementation = CategoryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "403", description = "Không có quyền Admin")
    })
    @PostMapping("/categories")
    public ResponseEntity<?> createCategory(@Valid @RequestBody CreateCategoryRequest req) {
        try {
            return ResponseEntity.ok(categoryService.createCategory(req));
        } catch (Exception e) {
            log.error("Admin createCategory error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Cập nhật danh mục", description = "`id` bắt buộc. `name` và `icon` đều tùy chọn — chỉ field nào có giá trị mới được cập nhật.", responses = {
            @ApiResponse(responseCode = "200", description = "Cập nhật thành công", content = @Content(schema = @Schema(implementation = CategoryResponse.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy danh mục"),
            @ApiResponse(responseCode = "403", description = "Không có quyền Admin")
    })
    @PatchMapping("/categories")
    public ResponseEntity<?> updateCategory(@Valid @RequestBody UpdateCategoryRequest req) {
        try {
            return ResponseEntity.ok(categoryService.updateCategory(req));
        } catch (Exception e) {
            log.error("Admin updateCategory error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Xóa danh mục", description = """
            ⚠️ Xóa vĩnh viễn danh mục.
            **Lưu ý:** Do `orphanRemoval = true`, toàn bộ giao dịch liên kết với danh mục này cũng sẽ bị xóa.
            """, responses = {
            @ApiResponse(responseCode = "200", description = "Xóa thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy danh mục"),
            @ApiResponse(responseCode = "403", description = "Không có quyền Admin")
    })
    @DeleteMapping("/categories/{id}")
    public ResponseEntity<?> deleteCategory(
            @Parameter(description = "ID của danh mục cần xóa", required = true) @PathVariable UUID id) {
        try {
            categoryService.deleteCategory(id);
            return ResponseEntity.ok("Xóa danh mục thành công.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            log.error("Admin deleteCategory error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ===================================================
    // 5. QUẢN LÝ ĐỐI TÁC VAY VỐN & ĐẦU TƯ (Partner)
    // ===================================================

    @Operation(summary = "Danh sách tất cả đối tác", description = "Trả về toàn bộ đối tác (cả đang ẩn và đang hiển thị). "
            +
            "End-user chỉ xem được đối tác đang active tại `GET /api/partners`.", responses = {
                    @ApiResponse(responseCode = "200", description = "Thành công"),
                    @ApiResponse(responseCode = "403", description = "Không có quyền Admin")
            })
    @GetMapping("/partners")
    public ResponseEntity<?> getAllPartners() {
        try {
            return ResponseEntity.ok(partnerService.getAllPartnersForAdmin());
        } catch (Exception e) {
            log.error("Admin getAllPartners error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Tạo đối tác mới", description = """
            Tạo đối tác ngân hàng hoặc quỹ đầu tư mới.
            - `name`: tên đối tác (bắt buộc, không trùng)
            - `partnerType`: `BANK` hoặc `INVESTMENT_FUND` (bắt buộc)
            - `loanInterestRate`: lãi suất vay %/năm (tùy chọn)
            - `savingInterestRate`: lãi suất tiết kiệm %/năm (tùy chọn)
            - `logoUrl`: URL ảnh logo (tùy chọn)
            - `description`: mô tả ngắn (tùy chọn)
            """, responses = {
            @ApiResponse(responseCode = "200", description = "Tạo thành công", content = @Content(schema = @Schema(implementation = PartnerResponse.class))),
            @ApiResponse(responseCode = "400", description = "Lỗi: tên đối tác đã tồn tại hoặc dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "403", description = "Không có quyền Admin")
    })
    @PostMapping("/partners")
    public ResponseEntity<?> createPartner(@Valid @RequestBody PartnerRequest.Create req) {
        try {
            return ResponseEntity.ok(partnerService.createPartner(req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Admin createPartner error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Cập nhật thông tin đối tác", description = "`id` bắt buộc. Các field còn lại đều tùy chọn — chỉ field nào có giá trị mới mới được cập nhật. "
            +
            "Dùng để cập nhật lãi suất, logo, mô tả...", responses = {
                    @ApiResponse(responseCode = "200", description = "Cập nhật thành công", content = @Content(schema = @Schema(implementation = PartnerResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Lỗi dữ liệu"),
                    @ApiResponse(responseCode = "404", description = "Không tìm thấy đối tác"),
                    @ApiResponse(responseCode = "403", description = "Không có quyền Admin")
            })
    @PatchMapping("/partners")
    public ResponseEntity<?> updatePartner(@Valid @RequestBody PartnerRequest.Update req) {
        try {
            return ResponseEntity.ok(partnerService.updatePartner(req));
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Admin updatePartner error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Bật / Tắt hiển thị đối tác", description = """
            Toggle trạng thái `isActive` của đối tác:
            - Nếu đang **active** → ẩn khỏi end-user
            - Nếu đang **inactive** → hiển thị lại cho end-user
            """, responses = {
            @ApiResponse(responseCode = "200", description = "Toggle thành công", content = @Content(schema = @Schema(implementation = PartnerResponse.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy đối tác"),
            @ApiResponse(responseCode = "403", description = "Không có quyền Admin")
    })
    @PutMapping("/partners/{id}/toggle")
    public ResponseEntity<?> togglePartner(
            @Parameter(description = "ID của đối tác", required = true) @PathVariable UUID id) {
        try {
            return ResponseEntity.ok(partnerService.toggleActive(id));
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            log.error("Admin togglePartner error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Xóa đối tác", description = "Xóa vĩnh viễn đối tác. Nếu chỉ muốn ẩn tạm thời, dùng `PUT /partners/{id}/toggle` thay thế.", responses = {
            @ApiResponse(responseCode = "200", description = "Xóa thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy đối tác"),
            @ApiResponse(responseCode = "403", description = "Không có quyền Admin")
    })
    @DeleteMapping("/partners/{id}")
    public ResponseEntity<?> deletePartner(
            @Parameter(description = "ID của đối tác cần xóa", required = true) @PathVariable UUID id) {
        try {
            partnerService.deletePartner(id);
            return ResponseEntity.ok("Xóa đối tác thành công.");
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            log.error("Admin deletePartner error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ===================================================
    // 6. GIÁM SÁT AI
    // ===================================================

    @Operation(summary = "Danh sách AI logs", description = "Xem lịch sử các request đến Gemini AI. Lọc theo status: SUCCESS, ERROR.", responses = {
            @ApiResponse(responseCode = "200", description = "Thành công"),
            @ApiResponse(responseCode = "403", description = "Không có quyền Admin")
    })
    @GetMapping("/ai/logs")
    public ResponseEntity<?> getAiLogs(
            @Parameter(description = "Lọc theo status: SUCCESS, ERROR") @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            var pageable = PageRequest.of(page, size);
            var result = (status != null && !status.isBlank())
                    ? aiLogRepository.findByStatusOrderByCreatedAtDesc(status.toUpperCase(), pageable)
                    : aiLogRepository.findAllByOrderByCreatedAtDesc(pageable);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Admin getAiLogs error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Thống kê AI", description = "Tổng quan về việc sử dụng AI: tổng request, tỉ lệ lỗi, token đã dùng, thời gian phản hồi trung bình.", responses = {
            @ApiResponse(responseCode = "200", description = "Thành công"),
            @ApiResponse(responseCode = "403", description = "Không có quyền Admin")
    })
    @GetMapping("/ai/stats")
    public ResponseEntity<?> getAiStats() {
        try {
            long totalRequests = aiLogRepository.count();
            long successCount = aiLogRepository.countByStatus("SUCCESS");
            long errorCount = aiLogRepository.countByStatus("ERROR");
            long totalTokens = aiLogRepository.sumTotalTokens();
            Double avgDuration = aiLogRepository.avgDurationMs();
            long errorsToday = aiLogRepository.countByStatusAndCreatedAtAfter(
                    "ERROR", LocalDateTime.now().toLocalDate().atStartOfDay());

            return ResponseEntity.ok(Map.of(
                    "totalRequests", totalRequests,
                    "successCount", successCount,
                    "errorCount", errorCount,
                    "errorRate", totalRequests > 0
                            ? String.format("%.1f%%", errorCount * 100.0 / totalRequests)
                            : "0%",
                    "totalTokensUsed", totalTokens,
                    "avgResponseMs", avgDuration != null ? avgDuration.longValue() : 0,
                    "errorsToday", errorsToday));
        } catch (Exception e) {
            log.error("Admin getAiStats error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
