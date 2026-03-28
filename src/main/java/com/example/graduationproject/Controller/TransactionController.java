package com.example.graduationproject.Controller;

import com.example.graduationproject.Dto.Request.CreateTransactionRequest;
import com.example.graduationproject.Dto.Request.DeleteTransactionRequest;
import com.example.graduationproject.Dto.Request.UpdateTransactionRequest;
import com.example.graduationproject.Dto.Response.TransactionResponse;
import com.example.graduationproject.Service.TransactionService;
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
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Transaction", description = "Quản lý giao dịch thu/chi/chuyển khoản")
public class TransactionController {

    private final TransactionService transactionService;

    @Operation(
        summary = "Tạo giao dịch",
        description = """
            Tạo một giao dịch mới. Hành vi theo từng loại:
            
            - **EXPENSE**: Trừ tiền khỏi ví. `categoryId` bắt buộc.
            - **INCOME**: Cộng tiền vào ví. `categoryId` bắt buộc.
            - **TRANSFER**: Chuyển tiền giữa 2 ví. `toWalletId` bắt buộc. `categoryId` có thể bỏ trống.
            
            Số dư ví sẽ được cập nhật tự động.
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Tạo thành công",
                content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Lỗi: ví không tồn tại, số dư không đủ, thiếu field bắt buộc...",
                content = @Content(schema = @Schema(type = "string")))
        }
    )
    @PostMapping
    public ResponseEntity<?> createTransaction(@Valid @RequestBody CreateTransactionRequest req) {
        try {
            return ResponseEntity.ok(transactionService.createTransaction(req));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @Operation(
        summary = "Lấy danh sách giao dịch theo ví",
        description = "Lấy tất cả giao dịch chưa bị xóa (`isDeleted = false`) của một ví. " +
                      "Chỉ lấy được ví thuộc user đang đăng nhập.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Danh sách giao dịch",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = TransactionResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Ví không tồn tại hoặc không có quyền",
                content = @Content(schema = @Schema(type = "string")))
        }
    )
    @GetMapping
    public ResponseEntity<?> getTransactions(
            @Parameter(description = "ID của ví cần xem giao dịch", required = true) @RequestParam UUID walletId) {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            List<TransactionResponse> responses = transactionService.getTransactionsByWallet(email, walletId);
            return ResponseEntity.ok(responses);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @Operation(
        summary = "Cập nhật giao dịch",
        description = """
            Cập nhật giao dịch INCOME hoặc EXPENSE. `id` là bắt buộc, các field còn lại tuỳ chọn.
            
            ⚠️ Không hỗ trợ cập nhật giao dịch loại TRANSFER.
            
            Khi thay đổi `amount`, số dư ví sẽ được tự động điều chỉnh (hoàn lại số cũ, áp dụng số mới).
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Cập nhật thành công",
                content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Không tìm thấy hoặc số dư không đủ",
                content = @Content(schema = @Schema(type = "string")))
        }
    )
    @PatchMapping
    public ResponseEntity<?> updateTransaction(@Valid @RequestBody UpdateTransactionRequest req) {
        try {
            return ResponseEntity.ok(transactionService.updateTransaction(req));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @Operation(
        summary = "Xóa giao dịch (soft delete)",
        description = """
            Đánh dấu giao dịch là đã xóa (`isDeleted = true`) và hoàn lại số dư ví:
            
            - **EXPENSE bị xóa** → cộng lại tiền vào ví
            - **INCOME bị xóa** → trừ lại tiền khỏi ví
            - **TRANSFER bị xóa** → cộng lại ví nguồn, trừ ví đích
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Xóa thành công",
                content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Không tìm thấy hoặc đã xóa trước đó",
                content = @Content(schema = @Schema(type = "string")))
        }
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTransaction(
            @Parameter(description = "ID của giao dịch cần xóa", required = true) @PathVariable UUID id) {
        try {
            return ResponseEntity.ok(transactionService.deleteTransaction(new DeleteTransactionRequest(id)));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
}
