package com.example.graduationproject.Controller;

import com.example.graduationproject.Dto.Request.CreateWalletRequest;
import com.example.graduationproject.Dto.Request.DeleteWalletRequest;
import com.example.graduationproject.Dto.Request.UpdateWalletRequest;
import com.example.graduationproject.Dto.Response.WalletResponse;
import com.example.graduationproject.Service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/wallets")
@RestController
@Tag(name = "Wallet", description = "Quản lý ví tiền (tiền mặt, ngân hàng, thẻ tín dụng...)")
public class WalletController {

    @Autowired
    private WalletService walletService;

    @Operation(
        summary = "Tạo ví mới",
        description = "Tạo một ví mới cho user đang đăng nhập. " +
                      "walletType nhận: `CASH`, `BANK_ACCOUNT`, `CREDIT_CARD`, `SAVINGS`, `INVESTMENT`.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Tạo ví thành công",
                content = @Content(schema = @Schema(implementation = WalletResponse.class))),
            @ApiResponse(responseCode = "400", description = "Lỗi dữ liệu đầu vào",
                content = @Content(schema = @Schema(type = "string")))
        }
    )
    @PostMapping
    public ResponseEntity<?> createWallet(@Valid @RequestBody CreateWalletRequest createWalletRequest) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            WalletResponse wallet = walletService.createWallet(
                    email,
                    createWalletRequest.getBalance(),
                    createWalletRequest.getName(),
                    createWalletRequest.getWalletType());
            return ResponseEntity.ok(wallet);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("" + ex.getMessage());
        }
    }

    @Operation(
        summary = "Lấy danh sách ví",
        description = "Lấy tất cả ví (bao gồm cả đã xóa mềm) của user đang đăng nhập.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Danh sách ví",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = WalletResponse.class))))
        }
    )
    @GetMapping
    public ResponseEntity<List<WalletResponse>> getAllWallets() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(walletService.GetListWallet(email));
    }

    @Operation(
        summary = "Cập nhật ví",
        description = "Cập nhật thông tin ví. Chỉ gửi các field cần thay đổi, `id` là bắt buộc.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Cập nhật thành công",
                content = @Content(schema = @Schema(implementation = WalletResponse.class))),
            @ApiResponse(responseCode = "400", description = "Không tìm thấy ví",
                content = @Content(schema = @Schema(type = "string")))
        }
    )
    @PatchMapping
    public ResponseEntity<?> updateWallet(@Valid @RequestBody UpdateWalletRequest updateWalletRequest) {
        try {
            return ResponseEntity.ok(walletService.updateWallet(updateWalletRequest));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("" + ex.getMessage());
        }
    }

    @Operation(
        summary = "Xóa ví (soft delete)",
        description = "Đánh dấu ví là đã xóa (`isDeleted = true`). Dữ liệu không bị xóa khỏi database.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Xóa thành công",
                content = @Content(schema = @Schema(implementation = WalletResponse.class))),
            @ApiResponse(responseCode = "400", description = "Không tìm thấy ví",
                content = @Content(schema = @Schema(type = "string")))
        }
    )
    @DeleteMapping
    public ResponseEntity<?> deleteWallet(@Valid @RequestBody DeleteWalletRequest deleteWalletRequest) {
        try {
            return ResponseEntity.ok(walletService.deleteWallet(deleteWalletRequest));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("" + ex.getMessage());
        }
    }
}
