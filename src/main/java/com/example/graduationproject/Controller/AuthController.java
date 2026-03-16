package com.example.graduationproject.Controller;

import com.example.graduationproject.Dto.Request.GoogleLoginRequest;
import com.example.graduationproject.Dto.Request.RefreshTokenRequest;
import com.example.graduationproject.Dto.Response.GoogleLoginResponse;
import com.example.graduationproject.Repository.UserRepository;
import com.example.graduationproject.Service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {
    @Autowired
    private AuthService authService;

    @PostMapping("/google")
    public ResponseEntity<GoogleLoginResponse> googleLogin(@RequestBody GoogleLoginRequest googleLoginRequest){
        return ResponseEntity.ok(authService.googleLogin(googleLoginRequest.getIdToken()));
    }

    @PostMapping("refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest){
        try {
            return ResponseEntity.ok(authService.refreshToken(refreshTokenRequest.getRefreshToken()));
        }catch (Exception ex){
            return ResponseEntity.badRequest().body("" + ex.getMessage());
        }

    }



}
