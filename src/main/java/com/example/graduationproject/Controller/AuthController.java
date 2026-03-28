package com.example.graduationproject.Controller;

import com.example.graduationproject.Dto.Request.GoogleLoginRequest;
import com.example.graduationproject.Dto.Request.RefreshTokenRequest;
import com.example.graduationproject.Dto.Response.GoogleLoginResponse;
import com.example.graduationproject.Service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Auth", description = "Đăng nhập, làm mới token, đăng xuất")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Operation(
        summary = "Đăng nhập bằng Google",
        description = "Gửi Google ID Token lấy từ Google OAuth2 để nhận accessToken và refreshToken.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Đăng nhập thành công",
                content = @Content(schema = @Schema(implementation = GoogleLoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Token Google không hợp lệ hoặc đã hết hạn",
                content = @Content(schema = @Schema(type = "string")))
        }
    )
    @SecurityRequirements   // API này không cần Bearer token
    @PostMapping("/google")
    public ResponseEntity<GoogleLoginResponse> googleLogin(@RequestBody GoogleLoginRequest googleLoginRequest) {
        return ResponseEntity.ok(authService.googleLogin(googleLoginRequest.getIdToken()));
    }

    @Operation(
        summary = "Làm mới access token",
        description = "Dùng refreshToken còn hiệu lực để nhận cặp token mới. " +
                      "Gọi API này khi accessToken hết hạn (thường sau 15 phút).",
        responses = {
            @ApiResponse(responseCode = "200", description = "Trả về cặp token mới",
                content = @Content(schema = @Schema(implementation = GoogleLoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Refresh token không hợp lệ hoặc đã hết hạn",
                content = @Content(schema = @Schema(type = "string")))
        }
    )
    @SecurityRequirements   // Chỉ cần refreshToken trong body, không cần Bearer header
    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest) {
        try {
            return ResponseEntity.ok(authService.refreshToken(refreshTokenRequest.getRefreshToken()));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("" + ex.getMessage());
        }
    }

    @Operation(
        summary = "Đăng xuất",
        description = "Huỷ refreshToken của user. Sau khi đăng xuất, cả accessToken và refreshToken đều không sử dụng được.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Đăng xuất thành công"),
            @ApiResponse(responseCode = "400", description = "Token không hợp lệ")
        }
    )
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestBody RefreshTokenRequest logoutRequest) {
        try {
            return ResponseEntity.ok(authService.logout(logoutRequest.getRefreshToken()));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("lỗi" + ex.getMessage());
        }
    }

    @Operation(summary = "Kiểm tra token", description = "Trả về email của user đang đăng nhập. Dùng để kiểm tra token còn hiệu lực.")
    @GetMapping("me")
    public String me() {
        return "me";
    }
}
