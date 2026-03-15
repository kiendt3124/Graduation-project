package com.example.graduationproject.Dto.Response;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GoogleLoginResponse {
    private String accessToken;
    private String refreshToken;
    private String email;
    private String role;
    private String tier;

}
