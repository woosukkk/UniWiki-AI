package com.uniwiki.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Spring Security 없이 아주 간단하게 JWT 토큰을 만들고 검증하는 유틸리티 클래스입니다.
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    
    // 토큰 만료 시간 (예: 24시간)
    private final long expirationTime;

    public JwtUtil(
            @Value("${uniwiki.jwt.secret}") String secret,
            @Value("${uniwiki.jwt.expiration-ms:2592000000}") long expirationTime
    ) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET은 32자 이상이어야 합니다.");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        if (expirationTime <= 0) {
            throw new IllegalStateException("JWT expiration time must be positive.");
        }
        this.expirationTime = expirationTime;
    }

    /**
     * 유저 이메일과 ID를 바탕으로 JWT 토큰을 생성합니다.
     */
    public String generateToken(Long userId, String email) {
        return Jwts.builder()
                .subject(email) // 토큰의 주인을 이메일로 설정
                .claim("userId", userId) // 추가 정보로 유저 ID 저장
                .issuedAt(new Date()) // 발행 시간
                .expiration(new Date(System.currentTimeMillis() + expirationTime)) // 만료 시간
                .signWith(key) // 비밀키로 서명
                .compact();
    }

    /**
     * 프론트엔드에서 보낸 토큰이 유효한지 검증하고 이메일을 꺼냅니다.
     */
    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        
        return claims.getSubject();
    }

    /**
     * 토큰에서 유저 ID(PK)를 꺼냅니다.
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
                
        Object userId = claims.get("userId");
        if (userId instanceof Number number) {
            return number.longValue();
        }
        if (userId instanceof String value) {
            return Long.parseLong(value);
        }
        throw new IllegalArgumentException("Token does not contain a valid userId.");
    }
}
