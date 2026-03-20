package com.example.graduationproject.Service;

import com.example.graduationproject.Dto.Response.GoogleLoginResponse;
import com.example.graduationproject.Entity.Enum.AccountTier;
import com.example.graduationproject.Entity.Enum.Role;
import com.example.graduationproject.Entity.User;
import com.example.graduationproject.Repository.UserRepository;
import com.example.graduationproject.Util.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtils jwtUtils;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        // Khởi tạo AuthService với Client ID giả (Do dùng trong GoogleIdTokenVerifier)
        authService = new AuthService("dummy-client-id", userRepository, jwtUtils);
    }

    // ==========================================
    // TEST API REFRESH TOKEN
    // ==========================================

    @Test
    void testRefreshToken_Success() {
        // 1. Khởi tạo dữ liệu giả (Mock Data)
        String oldRefreshToken = "old-refresh-token";
        String email = "test@example.com";
        User mockUser = new User();
        mockUser.setEmail(email);
        mockUser.setRole(Role.USER);
        mockUser.setAccountTier(AccountTier.BASIC);
        mockUser.setRefreshToken(oldRefreshToken);

        String newAccessToken = "new-access-token";
        String newRefreshToken = "new-refresh-token";

        // 2. Dạy cho Mockito biết phải làm gì khi các Dependency được gọi
        when(jwtUtils.validateJwtToken(oldRefreshToken)).thenReturn(true);
        when(jwtUtils.getTypeFromToken(oldRefreshToken)).thenReturn("refresh");
        when(userRepository.findByRefreshToken(oldRefreshToken)).thenReturn(Optional.of(mockUser));
        
        when(jwtUtils.createToken(email, Role.USER, AccountTier.BASIC, "access")).thenReturn(newAccessToken);
        when(jwtUtils.createToken(email, Role.USER, AccountTier.BASIC, "refresh")).thenReturn(newRefreshToken);

        // 3. Thực thi method
        GoogleLoginResponse response = authService.refreshToken(oldRefreshToken);

        // 4. Kiểm tra (Assert)
        assertNotNull(response);
        assertEquals(newAccessToken, response.getAccessToken());
        assertEquals(newRefreshToken, response.getRefreshToken());
        assertEquals(email, response.getEmail());

        // Kiểm tra xem User có được gán token mới và lưu xuống DB không
        assertEquals(newRefreshToken, mockUser.getRefreshToken());
        verify(userRepository, times(1)).save(mockUser);
    }

    @Test
    void testRefreshToken_InvalidToken_ThrowsException() {
        String invalidToken = "invalid-token";

        when(jwtUtils.validateJwtToken(invalidToken)).thenReturn(false); // Báo là token hết hạn/sai

        // Bắt lỗi RuntimeException
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.refreshToken(invalidToken);
        });

        assertEquals("Refresh Token không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại!", exception.getMessage());
        // Đảm bảo userRepository.save() không bao giờ được gọi
        verify(userRepository, never()).save(any());
    }

    // ==========================================
    // TEST API LOGOUT
    // ==========================================

    @Test
    void testLogout_WithRefreshToken_Success() {
        String refreshToken = "valid-refresh-token";
        User mockUser = new User();
        mockUser.setEmail("test@example.com");
        mockUser.setRefreshToken(refreshToken);

        when(jwtUtils.validateJwtToken(refreshToken)).thenReturn(true);
        when(jwtUtils.getTypeFromToken(refreshToken)).thenReturn("refresh");
        when(userRepository.findByRefreshToken(refreshToken)).thenReturn(Optional.of(mockUser));

        String result = authService.logout(refreshToken);

        assertEquals("đăng xuất thành công", result);
        assertNull(mockUser.getRefreshToken()); // Token của User đã phải bị gán về null
        verify(userRepository, times(1)).save(mockUser); // Đảm bảo thao tác lưu user xuống DB chạy 1 lần
    }
}
