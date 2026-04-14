package com.example.graduationproject.Controller;

import com.example.graduationproject.Dto.Request.FinancialGoalRequest;
import com.example.graduationproject.Dto.Response.FinancialGoalResponse;
import com.example.graduationproject.Dto.Response.GoalContributionResponse;
import com.example.graduationproject.Entity.Enum.GoalStatus;
import com.example.graduationproject.Service.FinancialGoalService;
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
@RequestMapping("/api/goals")
@RequiredArgsConstructor
@Tag(name = "Financial Goal", description = "Quản lý mục tiêu tài chính và lịch sử góp tiền")
public class FinancialGoalController {

    private final FinancialGoalService goalService;

    // ─── POST /api/goals ─────────────────────────────────────────────────────

    @Operation(
        summary = "Tạo mục tiêu tài chính mới",
        description = """
            Tạo mục tiêu tiết kiệm mới. Ví dụ: "Mua xe", "Du lịch Nhật Bản".

            **Lưu ý:**
            - `targetAmount` phải là số dương
            - `deadline` để null nếu không có hạn cụ thể
            - Mục tiêu mới tự động có trạng thái `ACTIVE`
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Tạo thành công",
                content = @Content(schema = @Schema(implementation = FinancialGoalResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ",
                content = @Content(schema = @Schema(type = "string")))
        }
    )
    @PostMapping
    public ResponseEntity<?> createGoal(@Valid @RequestBody FinancialGoalRequest.Create req) {
        try {
            return ResponseEntity.ok(goalService.createGoal(getEmail(), req));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    // ─── GET /api/goals ──────────────────────────────────────────────────────

    @Operation(
        summary = "Lấy danh sách mục tiêu",
        description = """
            Lấy tất cả mục tiêu của user.

            **Filter tuỳ chọn:**
            - `status` = `ACTIVE` | `COMPLETED` | `CANCELLED`

            **Thứ tự hiển thị:** ACTIVE → COMPLETED → CANCELLED,
            deadline gần nhất lên trước (null xuống cuối).
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Danh sách mục tiêu",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = FinancialGoalResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Lỗi xử lý",
                content = @Content(schema = @Schema(type = "string")))
        }
    )
    @GetMapping
    public ResponseEntity<?> getGoals(
            @Parameter(description = "Lọc theo trạng thái: ACTIVE, COMPLETED, CANCELLED")
            @RequestParam(required = false) GoalStatus status) {
        try {
            List<FinancialGoalResponse> responses = goalService.getGoals(getEmail(), status);
            return ResponseEntity.ok(responses);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    // ─── GET /api/goals/{id} ─────────────────────────────────────────────────

    @Operation(
        summary = "Xem chi tiết mục tiêu",
        description = "Lấy thông tin chi tiết của 1 mục tiêu, bao gồm tiến độ và số tiền còn thiếu.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Chi tiết mục tiêu",
                content = @Content(schema = @Schema(implementation = FinancialGoalResponse.class))),
            @ApiResponse(responseCode = "400", description = "Không tìm thấy hoặc không có quyền",
                content = @Content(schema = @Schema(type = "string")))
        }
    )
    @GetMapping("/{id}")
    public ResponseEntity<?> getGoalById(
            @Parameter(description = "ID của mục tiêu", required = true)
            @PathVariable UUID id) {
        try {
            return ResponseEntity.ok(goalService.getGoalById(getEmail(), id));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    // ─── PATCH /api/goals/{id} ───────────────────────────────────────────────

    @Operation(
        summary = "Cập nhật mục tiêu",
        description = """
            Cập nhật thông tin mục tiêu. Chỉ các field được truyền (khác null) mới bị thay đổi.

            **Có thể sửa:** `name`, `targetAmount`, `deadline`, `note`

            **Đổi trạng thái:** Chỉ cho phép đổi sang `CANCELLED` thủ công.
            Trạng thái `COMPLETED` do hệ thống tự cập nhật khi đủ tiền.
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Cập nhật thành công",
                content = @Content(schema = @Schema(implementation = FinancialGoalResponse.class))),
            @ApiResponse(responseCode = "400", description = "Không tìm thấy hoặc không có quyền",
                content = @Content(schema = @Schema(type = "string")))
        }
    )
    @PatchMapping("/{id}")
    public ResponseEntity<?> updateGoal(
            @Parameter(description = "ID của mục tiêu", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody FinancialGoalRequest.Update req) {
        try {
            return ResponseEntity.ok(goalService.updateGoal(getEmail(), id, req));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    // ─── DELETE /api/goals/{id} ──────────────────────────────────────────────

    @Operation(
        summary = "Xoá mục tiêu",
        description = """
            Xoá mềm (soft delete) mục tiêu. Mục tiêu sẽ không hiển thị trong danh sách nữa
            nhưng dữ liệu vẫn được lưu trong database.
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Xoá thành công",
                content = @Content(schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "400", description = "Không tìm thấy hoặc không có quyền",
                content = @Content(schema = @Schema(type = "string")))
        }
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteGoal(
            @Parameter(description = "ID của mục tiêu cần xoá", required = true)
            @PathVariable UUID id) {
        try {
            goalService.deleteGoal(getEmail(), id);
            return ResponseEntity.ok("Goal deleted successfully");
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    // ─── POST /api/goals/{id}/contributions ──────────────────────────────────

    @Operation(
        summary = "Góp tiền vào mục tiêu",
        description = """
            Ghi nhận 1 lần góp tiền vào mục tiêu từ ví cụ thể.

            **Ràng buộc:**
            - Mục tiêu phải đang ở trạng thái `ACTIVE`
            - Ví phải tồn tại, chưa xoá, và thuộc về bạn
            - Số dư ví phải đủ
            - Nếu tổng góp đạt `targetAmount` → status tự động chuyển sang `COMPLETED`
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Góp tiền thành công",
                content = @Content(schema = @Schema(implementation = GoalContributionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Lỗi (đã completed, ví không đủ tiền, ...)",
                content = @Content(schema = @Schema(type = "string")))
        }
    )
    @PostMapping("/{id}/contributions")
    public ResponseEntity<?> addContribution(
            @Parameter(description = "ID của mục tiêu", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody FinancialGoalRequest.AddContribution req) {
        try {
            GoalContributionResponse response = goalService.addContribution(getEmail(), id, req);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    // ─── GET /api/goals/{id}/contributions ───────────────────────────────────

    @Operation(
        summary = "Lịch sử góp tiền",
        description = "Lấy toàn bộ lịch sử các lần góp tiền của 1 mục tiêu, sắp xếp mới nhất lên trước.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Danh sách lần góp tiền",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = GoalContributionResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Không tìm thấy hoặc không có quyền",
                content = @Content(schema = @Schema(type = "string")))
        }
    )
    @GetMapping("/{id}/contributions")
    public ResponseEntity<?> getContributions(
            @Parameter(description = "ID của mục tiêu", required = true)
            @PathVariable UUID id) {
        try {
            List<GoalContributionResponse> responses = goalService.getContributions(getEmail(), id);
            return ResponseEntity.ok(responses);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    // ─── DELETE /api/goals/{goalId}/contributions/{contributionId} ────────────

    @Operation(
        summary = "Xoá lần góp tiền",
        description = """
            Xoá 1 lần góp tiền. Số tiền sẽ được hoàn trả về ví.

            **Lưu ý:**
            - Nếu sau khi xoá tổng tiền góp < `targetAmount` và goal đang `COMPLETED`,
              status sẽ tự động reset về `ACTIVE`.
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Xoá thành công",
                content = @Content(schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "400", description = "Không tìm thấy hoặc không có quyền",
                content = @Content(schema = @Schema(type = "string")))
        }
    )
    @DeleteMapping("/{id}/contributions/{contributionId}")
    public ResponseEntity<?> deleteContribution(
            @Parameter(description = "ID của mục tiêu", required = true)
            @PathVariable UUID id,
            @Parameter(description = "ID của lần góp tiền cần xoá", required = true)
            @PathVariable UUID contributionId) {
        try {
            goalService.deleteContribution(getEmail(), id, contributionId);
            return ResponseEntity.ok("Contribution deleted successfully");
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    // ─── HELPER ──────────────────────────────────────────────────────────────

    private String getEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
