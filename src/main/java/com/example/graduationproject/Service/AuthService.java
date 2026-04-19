package com.example.graduationproject.Service;

import com.example.graduationproject.Dto.Response.GoogleLoginResponse;
import com.example.graduationproject.Entity.Enum.AccountTier;
import com.example.graduationproject.Entity.Enum.Role;
import com.example.graduationproject.Entity.User;
import com.example.graduationproject.Repository.UserRepository;
import com.example.graduationproject.Util.JwtUtils;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final GoogleIdTokenVerifier verifier;

    @Value("${admin.username:admin}")
    private String adminUsername;

    @Value("${admin.password:admin}")
    private String adminPassword;

    public AuthService(@Value("${google.client.id}") String clientId, UserRepository userRepository,
            JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    // ===================================================
    // Admin Login (username + password từ config)
    // ===================================================
    public Map<String, String> adminLogin(String username, String password) {
        if (!adminUsername.equals(username) || !adminPassword.equals(password)) {
            throw new RuntimeException("Username hoặc password không đúng.");
        }

        String accessToken  = jwtUtils.createToken(adminUsername, Role.ADMIN, AccountTier.BASIC, "access");
        String refreshToken = jwtUtils.createToken(adminUsername, Role.ADMIN, AccountTier.BASIC, "refresh");

        return Map.of(
                "accessToken",  accessToken,
                "refreshToken", refreshToken,
                "username",     adminUsername,
                "role",         "ADMIN"
        );
    }

    public GoogleLoginResponse googleLogin(String idTokenByClient) {
        GoogleIdToken idToken;

        try {
            idToken = verifier.verify(idTokenByClient);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi" + e.getMessage());
        }

        if (idToken == null) {
            throw new RuntimeException("Google Token không hợp lệ hoặc đã hết hạn!");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();
        String email = payload.getEmail();

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setGoogleId(payload.getSubject());
            newUser.setRole(Role.USER);
            newUser.setAccountTier(AccountTier.BASIC);
            newUser.setCreatedAt(LocalDateTime.now());
            return userRepository.save(newUser);
        });

        // Từ chối đăng nhập nếu tài khoản bị banned
        if (user.isBanned()) {
            throw new RuntimeException("Tài khoản của bạn đã bị vô hiệu hóa. Vui lòng liên hệ hỗ trợ.");
        }

        String accessToken = jwtUtils.createToken(email, user.getRole(), user.getAccountTier(), "access");
        String refreshToken = jwtUtils.createToken(email, user.getRole(), user.getAccountTier(), "refresh");

        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        return new GoogleLoginResponse(accessToken, refreshToken, email, user.getRole().toString(),
                user.getAccountTier().toString());

    }

    public GoogleLoginResponse refreshToken(String refreshToken) {
        if (refreshToken == null || !jwtUtils.validateJwtToken(refreshToken)) {
            throw new RuntimeException("Refresh Token không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại!");
        }

        String type = jwtUtils.getTypeFromToken(refreshToken);
        if ("access".equals(type)) {
            throw new RuntimeException("Yêu cầu refresh token");
        }else if ("refresh".equals(type)) {
            User user = userRepository.findByRefreshToken(refreshToken).orElse(null);
            if (user == null) {
                throw new RuntimeException("Token không hợp lệ");
            }else {
                String newAccessToken = jwtUtils.createToken(user.getEmail(), user.getRole(), user.getAccountTier(), "access");
                String newRefreshToken = jwtUtils.createToken(user.getEmail(), user.getRole(), user.getAccountTier(), "refresh");

                user.setRefreshToken(newRefreshToken);
                userRepository.save(user);
                return new GoogleLoginResponse(newAccessToken,newRefreshToken,user.getEmail(),user.getRole().toString(),user.getAccountTier().toString());
            }
        }
        throw new RuntimeException("Loại Token không xác định");
    }


    public String logout(String token) {
        if (token == null || !jwtUtils.validateJwtToken(token)) {
            throw new RuntimeException("Refresh Token không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại!");
        }
        String type = jwtUtils.getTypeFromToken(token);

        if ("access".equals(type)) {
            try {
                String currentEmail = jwtUtils.getEmailFromToken(token);
                User currentUser = userRepository.findByEmail(currentEmail).orElse(null);
                if (currentUser == null) {
                    throw new RuntimeException("lỗi token");
                }else {
                    currentUser.setRefreshToken(null);
                    userRepository.save(currentUser);
                    return "đăng xuất thành công";
                }
            }catch (Exception e){
                throw new RuntimeException(e.getMessage());
            }
        }else if ("refresh".equals(type)) {
            try {
                User currentUser = userRepository.findByRefreshToken(token).orElse(null);
                if(currentUser==null){
                    throw new RuntimeException("lỗi token");

                }else {
                    currentUser.setRefreshToken(null);
                    userRepository.save(currentUser);
                    return "đăng xuất thành công";
                }
            }catch (Exception e){
                throw new RuntimeException(e.getMessage());
            }
        }

        return "";

    }

}
