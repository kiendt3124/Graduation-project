package com.example.graduationproject.Controller;

import com.example.graduationproject.Dto.Request.InitiatePremiumRequest;
import com.example.graduationproject.Dto.Response.InitiatePremiumResponse;
import com.example.graduationproject.Dto.Response.PremiumOrderResponse;
import com.example.graduationproject.Dto.Response.PremiumStatusResponse;
import com.example.graduationproject.Dto.Webhook.SePayWebhookPayload;
import com.example.graduationproject.Service.PremiumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/premium")
@RequiredArgsConstructor
@Tag(name = "Premium", description = "Mua và quản lý gói Premium")
public class PremiumController {

    private final PremiumService premiumService;

    @Value("${sepay.api-key:}")
    private String sePayApiKey;

    // ===================================================
    // POST /premium/initiate — Tạo đơn, trả QR + thông tin CK
    // ===================================================
    @Operation(
        summary = "Tạo đơn mua Premium",
        description = "Tạo đơn chờ thanh toán. Trả về QR Code SePay và thông tin chuyển khoản. " +
                      "User quét QR bằng app ngân hàng, nhập đúng nội dung chuyển khoản để hệ thống tự động kích hoạt.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Tạo đơn thành công",
                content = @Content(schema = @Schema(implementation = InitiatePremiumResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
        }
    )
    @PostMapping("/initiate")
    public ResponseEntity<?> initiatePurchase(
            @Valid @RequestBody InitiatePremiumRequest request,
            Authentication authentication) {
        try {
            String email = (String) authentication.getPrincipal();
            InitiatePremiumResponse response = premiumService.initiatePurchase(email, request.getPlan());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Lỗi tạo đơn Premium: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ===================================================
    // POST /premium/webhook — SePay gọi vào khi có CK (không cần JWT)
    // ===================================================
    @Operation(
        summary = "Webhook SePay",
        description = "Endpoint nhận thông báo từ SePay khi có biến động số dư. " +
                      "Không cần JWT — được bảo vệ bằng API Key trong header Authorization.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Nhận webhook thành công")
        }
    )
    @SecurityRequirements  // Không cần Bearer token
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> handleWebhook(
            @RequestBody SePayWebhookPayload payload,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Xác thực API Key nếu đã cấu hình
        if (sePayApiKey != null && !sePayApiKey.isBlank()) {
            String expected = "Apikey " + sePayApiKey;
            if (!expected.equals(authHeader)) {
                log.warn("Webhook bị từ chối: sai API Key");
                return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
            }
        }

        try {
            premiumService.handleWebhook(payload);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.error("Lỗi xử lý webhook SePay: {}", e.getMessage(), e);
            // Vẫn trả 200 để SePay không retry liên tục với lỗi hệ thống nội bộ
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ===================================================
    // GET /premium/status — Trạng thái Premium hiện tại
    // ===================================================
    @Operation(
        summary = "Trạng thái Premium",
        description = "Trả về tier hiện tại (BASIC/PREMIUM) và ngày hết hạn.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Thành công",
                content = @Content(schema = @Schema(implementation = PremiumStatusResponse.class)))
        }
    )
    @GetMapping("/status")
    public ResponseEntity<?> getStatus(Authentication authentication) {
        try {
            String email = (String) authentication.getPrincipal();
            return ResponseEntity.ok(premiumService.getStatus(email));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ===================================================
    // GET /premium/my-orders — Lịch sử mua Premium
    // ===================================================
    @Operation(
        summary = "Lịch sử đơn mua Premium",
        description = "Trả về toàn bộ lịch sử đơn mua Premium của user đang đăng nhập, sắp xếp mới nhất trước.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Thành công")
        }
    )
    @GetMapping("/my-orders")
    public ResponseEntity<?> getMyOrders(Authentication authentication) {
        try {
            String email = (String) authentication.getPrincipal();
            List<PremiumOrderResponse> orders = premiumService.getMyOrders(email);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
