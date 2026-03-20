package com.example.graduationproject.Dto.Request;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class GoogleLoginRequest {
    private String idToken;
}
