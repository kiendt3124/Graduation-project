package com.example.graduationproject.Controller;

import com.example.graduationproject.Dto.Request.LoanRequest;
import com.example.graduationproject.Dto.Response.LoanPaymentResponse;
import com.example.graduationproject.Dto.Response.LoanResponse;
import com.example.graduationproject.Entity.Enum.LoanStatus;
import com.example.graduationproject.Entity.Enum.LoanType;
import com.example.graduationproject.Service.LoanService;
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
@RequestMapping("/api/loans")
@RequiredArgsConstructor
@Tag(name = "Loan", description = "Quản lý khoản vay và lịch sử thanh toán")
public class LoanController {

    private final LoanService loanService;

    // ─── POST /api/loans ─────────────────────────────────────────────────────

    @Operation(
        summary = "Tạo khoản vay mới",
        description = """
            Tạo khoản vay mới (mình nợ hoặc mình cho vay).

            **Lưu ý:**
            - `loanType` = `BORROW` (mình nợ người khác) hoặc `LEND` (mình cho vay)
            - `interestRate` là %/năm, để null nếu không tính lãi
            - `dueDate` để null nếu không có hạn cụ thể
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Tạo thành công",
                content = @Content(schema = @Schema(implementation = LoanResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ",
                content = @Content(schema = @Schema(type = "string")))
        }
    )
    @PostMapping
    public ResponseEntity<?> createLoan(@Valid @RequestBody LoanRequest.Create req) {
        try {
            return ResponseEntity.ok(loanService.createLoan(getEmail(), req));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    // ─── GET /api/loans ──────────────────────────────────────────────────────

    @Operation(
        summary = "Lấy danh sách khoản vay",
        description = """
            Lấy tất cả khoản vay của user.

            **Filter tuỳ chọn:**
            - `type` = `BORROW` | `LEND`
            - `status` = `ACTIVE` | `PAID` | `OVERDUE`

            **Thứ tự hiển thị:** OVERDUE → ACTIVE → PAID, hạn gần nhất lên trước.

            **Lãi suất** được tính real-time theo lãi đơn tính đến ngày hôm nay.
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Danh sách khoản vay",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = LoanResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Lỗi xử lý",
                content = @Content(schema = @Schema(type = "string")))
        }
    )
    @GetMapping
    public ResponseEntity<?> getLoans(
            @Parameter(description = "Lọc theo loại: BORROW hoặc LEND")
            @RequestParam(required = false) LoanType type,
            @Parameter(description = "Lọc theo trạng thái: ACTIVE, PAID, OVERDUE")
            @RequestParam(required = false) LoanStatus status) {
        try {
            List<LoanResponse> responses = loanService.getLoans(getEmail(), type, status);
            return ResponseEntity.ok(responses);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    // ─── GET /api/loans/{id} ─────────────────────────────────────────────────

    @Operation(
        summary = "Xem chi tiết khoản vay",
        description = "Lấy thông tin chi tiết của 1 khoản vay, bao gồm lãi tích lũy và tiến độ thanh toán.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Chi tiết khoản vay",
                content = @Content(schema = @Schema(implementation = LoanResponse.class))),
            @ApiResponse(responseCode = "400", description = "Không tìm thấy hoặc không có quyền",
                content = @Content(schema = @Schema(type = "string")))
        }
    )
    @GetMapping("/{id}")
    public ResponseEntity<?> getLoanById(
            @Parameter(description = "ID của khoản vay", required = true)
            @PathVariable UUID id) {
        try {
            return ResponseEntity.ok(loanService.getLoanById(getEmail(), id));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    // ─── PATCH /api/loans/{id} ───────────────────────────────────────────────

    @Operation(
        summary = "Cập nhật khoản vay",
        description = """
            Cập nhật thông tin khoản vay. Chỉ các field được truyền (khác null) mới bị thay đổi.

            **Có thể sửa:** `counterpart`, `interestRate`, `dueDate`, `status`, `note`

            **Không thể sửa:** `principalAmount`, `loanType`, `startDate`
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Cập nhật thành công",
                content = @Content(schema = @Schema(implementation = LoanResponse.class))),
            @ApiResponse(responseCode = "400", description = "Không tìm thấy hoặc không có quyền",
                content = @Content(schema = @Schema(type = "string")))
        }
    )
    @PatchMapping("/{id}")
    public ResponseEntity<?> updateLoan(
            @Parameter(description = "ID của khoản vay", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody LoanRequest.Update req) {
        try {
            return ResponseEntity.ok(loanService.updateLoan(getEmail(), id, req));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    // ─── DELETE /api/loans/{id} ──────────────────────────────────────────────

    @Operation(
        summary = "Xoá khoản vay",
        description = """
            Xoá mềm (soft delete) khoản vay. Khoản vay sẽ không hiển thị trong danh sách nữa
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
    public ResponseEntity<?> deleteLoan(
            @Parameter(description = "ID của khoản vay cần xoá", required = true)
            @PathVariable UUID id) {
        try {
            loanService.deleteLoan(getEmail(), id);
            return ResponseEntity.ok("Loan deleted successfully");
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    // ─── POST /api/loans/{id}/payments ───────────────────────────────────────

    @Operation(
        summary = "Ghi nhận thanh toán",
        description = """
            Ghi nhận 1 lần thanh toán cho khoản vay (partial payment).

            **Ràng buộc:**
            - Khoản vay phải đang ở trạng thái `ACTIVE` hoặc `OVERDUE`
            - Số tiền không được vượt quá số dư còn lại (`remainingAmount`)
            - Nếu thanh toán đủ → status tự động chuyển sang `PAID`
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Ghi nhận thành công",
                content = @Content(schema = @Schema(implementation = LoanPaymentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Lỗi (đã paid, vượt hạn mức, ...)",
                content = @Content(schema = @Schema(type = "string")))
        }
    )
    @PostMapping("/{id}/payments")
    public ResponseEntity<?> addPayment(
            @Parameter(description = "ID của khoản vay", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody LoanRequest.AddPayment req) {
        try {
            LoanPaymentResponse response = loanService.addPayment(getEmail(), id, req);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    // ─── GET /api/loans/{id}/payments ────────────────────────────────────────

    @Operation(
        summary = "Lịch sử thanh toán",
        description = "Lấy toàn bộ lịch sử các lần thanh toán của 1 khoản vay, sắp xếp mới nhất lên trước.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Danh sách thanh toán",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = LoanPaymentResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Không tìm thấy hoặc không có quyền",
                content = @Content(schema = @Schema(type = "string")))
        }
    )
    @GetMapping("/{id}/payments")
    public ResponseEntity<?> getPayments(
            @Parameter(description = "ID của khoản vay", required = true)
            @PathVariable UUID id) {
        try {
            List<LoanPaymentResponse> responses = loanService.getPayments(getEmail(), id);
            return ResponseEntity.ok(responses);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    // ─── HELPER ──────────────────────────────────────────────────────────────

    private String getEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
