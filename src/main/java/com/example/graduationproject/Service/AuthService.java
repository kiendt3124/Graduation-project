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

import java.util.Collections;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final GoogleIdTokenVerifier verifier;

    public AuthService(@Value("${google.client.id}") String clientId, UserRepository userRepository,
            JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(clientId))
                .build();
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
            newUser.setRole(Role.USER);
            newUser.setAccountTier(AccountTier.BASIC);
            return userRepository.save(newUser);
        });

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
}
