package com.example.graduationproject.Util;

import com.example.graduationproject.Entity.Enum.AccountTier;
import com.example.graduationproject.Entity.Enum.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtils {

    @Value("${jwt.key}")
    private String secretKey;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String createToken(String email, Role role, AccountTier tier, String type) {
        Date now = new Date();
        Date validTime;
        if (type.equals("access")) {
            validTime = new Date(now.getTime() + 3600000L); //1 hour
        }else if (type.equals("refresh")) {
            validTime = new Date(now.getTime() + 2592000000L); //1 month
        }else {
            throw new IllegalStateException("Invalid token type");
        }

        return Jwts.builder()
                .claim("email",email)
                .claim("role",role.toString())
                .claim("tier", tier.toString())
                .claim("type",type)
                .setIssuedAt(now)
                .setExpiration(validTime)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Boolean validateJwtToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;

        } catch ( Exception e) {
            System.out.println("Lỗi xác thực JWT: " + e.getMessage());
            return false;
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Lấy Email (được lưu trong thuộc tính Subject chuẩn của JWT)
     */
    public String getEmailFromToken(String token) {
        return extractAllClaims(token).get("email", String.class);
    }

    /**
     * Lấy Role (String)
     */
    public String getRoleFromToken(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    /**
     * Lấy AccountTier (String)
     */
    public String getTierFromToken(String token) {
        return extractAllClaims(token).get("tier", String.class);
    }

    /**
     * Lấy loại token (access hay refresh)
     */
    public String getTypeFromToken(String token) {
        return extractAllClaims(token).get("type", String.class);
    }

}
