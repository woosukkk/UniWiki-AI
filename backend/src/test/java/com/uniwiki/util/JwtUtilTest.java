package com.uniwiki.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private static final String SECRET =
            "test-jwt-secret-with-at-least-32-characters";

    @Test
    void restoresSmallUserIdFromGeneratedToken() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 60_000L);

        String token = jwtUtil.generateToken(33L, "manager@sju.ac.kr");

        assertThat(jwtUtil.getUserIdFromToken(token)).isEqualTo(33L);
        assertThat(jwtUtil.getEmailFromToken(token)).isEqualTo("manager@sju.ac.kr");
    }

    @Test
    void restoresLargeUserIdFromGeneratedToken() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 60_000L);
        long userId = (long) Integer.MAX_VALUE + 1;

        String token = jwtUtil.generateToken(userId, "user@sju.ac.kr");

        assertThat(jwtUtil.getUserIdFromToken(token)).isEqualTo(userId);
    }
}
