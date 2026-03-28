package com.example.graduationproject.Controller;

import com.example.graduationproject.Dto.Request.BudgetRequest;
import com.example.graduationproject.Dto.Response.BudgetResponse;
import com.example.graduationproject.Service.BudgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
@Tag(name = "Budget", description = "Quản lý ngân sách chi tiêu theo tháng")
public class BudgetController {

    private final BudgetService budgetService;

    // ─── POST /api/budgets ────────────────────────────────────────────────────

    @Operation(
        summary = "Tạo ngân sách",
        description = """
            Tạo ngân sách chi tiêu cho một **category EXPENSE** trong một tháng/năm cụ thể.

            **Ràng buộc:**
            - Category phải thuộc loại `EXPENSE`
            - Mỗi (user + category + tháng + năm) chỉ được có 1 ngân sách
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Tạo thành công",
                content = @Content(schema = @Schema(implementation = BudgetResponse.class))),
            @ApiResponse(responseCode = "400", description = "Lỗi: category không phải EXPENSE, trùng ngân sách, ...",
                content = @Content(schema = @Schema(type = "string")))
        }
    )
    @PostMapping
    public ResponseEntity<?> createBudget(@Valid @RequestBody BudgetRequest.Create req) {
        try {
            String email = getEmail();
            return ResponseEntity.ok(budgetService.createBudget(email, req));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    // ─── GET /api/budgets?month=&year= ────────────────────────────────────────

    @Operation(
        summary = "Xem ngân sách theo tháng",
        description = """
            Lấy tất cả ngân sách của user trong một tháng/năm cụ thể.

            - `spentAmount` được tính **real-time** từ các giao dịch EXPENSE chưa bị xóa.
            - Kết quả được sắp xếp theo **mức độ sử dụng** (`progressPercentage`) giảm dần
              → budget gần vượt mức hiển thị trước.
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Danh sách ngân sách trong tháng",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = BudgetResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Lỗi xử lý",
                content = @Content(schema = @Schema(type = "string")))
        }
    )
    @GetMapping
    public ResponseEntity<?> getBudgetsByMonth(
            @Parameter(description = "Tháng cần xem (1–12)", required = true, example = "3")
            @RequestParam int month,
            @Parameter(description = "Năm cần xem", required = true, example = "2026")
            @RequestParam int year) {
        try {
            String email = getEmail();
            List<BudgetResponse> responses = budgetService.getBudgetsByMonth(email, month, year);
            return ResponseEntity.ok(responses);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    // ─── GET /api/budgets/all ─────────────────────────────────────────────────

    @Operation(
        summary = "Xem tất cả ngân sách",
        description = """
            Lấy toàn bộ ngân sách của user (mọi tháng, mọi năm).

            Kết quả sắp xếp: năm giảm dần → tháng giảm dần → mức sử dụng giảm dần.
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Toàn bộ danh sách ngân sách",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = BudgetResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Lỗi xử lý",
                content = @Content(schema = @Schema(type = "string")))
        }
    )
    @GetMapping("/all")
    public ResponseEntity<?> getAllBudgets() {
        try {
            String email = getEmail();
            List<BudgetResponse> responses = budgetService.getAllBudgets(email);
            return ResponseEntity.ok(responses);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    // ─── PATCH /api/budgets ───────────────────────────────────────────────────

    @Operation(
        summary = "Cập nhật hạn mức ngân sách",
        description = """
            Cập nhật hạn mức (`limitAmount`) của một ngân sách đã tồn tại.

            Chỉ cho phép sửa số tiền hạn mức. Không thể thay đổi category hoặc tháng/năm.
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Cập nhật thành công",
                content = @Content(schema = @Schema(implementation = BudgetResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ngân sách không tồn tại hoặc không có quyền",
                content = @Content(schema = @Schema(type = "string")))
        }
    )
    @PatchMapping
    public ResponseEntity<?> updateBudget(@Valid @RequestBody BudgetRequest.Update req) {
        try {
            String email = getEmail();
            return ResponseEntity.ok(budgetService.updateBudget(email, req));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    // ─── DELETE /api/budgets/{id} ─────────────────────────────────────────────

    @Operation(
        summary = "Xóa ngân sách",
        description = """
            Xóa vĩnh viễn một ngân sách (hard delete).

            Không ảnh hưởng đến các giao dịch đã có. Chỉ có thể xóa ngân sách của chính mình.
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Xóa thành công",
                content = @Content(schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "400", description = "Ngân sách không tồn tại hoặc không có quyền",
                content = @Content(schema = @Schema(type = "string")))
        }
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBudget(
            @Parameter(description = "ID của ngân sách cần xóa", required = true)
            @PathVariable UUID id) {
        try {
            String email = getEmail();
            budgetService.deleteBudget(email, id);
            return ResponseEntity.ok("Budget deleted successfully");
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    // ─── HELPER ───────────────────────────────────────────────────────────────

    private String getEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
