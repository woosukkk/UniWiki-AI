package com.uniwiki.dto;

import lombok.Data;

/**
 * 프론트엔드와 백엔드 간에 데이터를 주고받기 위한 DTO(Data Transfer Object) 클래스입니다.
 */
public class UserDto {

    /**
     * 회원가입 요청 시 프론트엔드에서 보내주는 데이터를 담는 클래스입니다.
     */
    @Data
    public static class SignupRequest {
        private String email;    // 가입할 이메일
        private String password; // 가입할 비밀번호
        private String nickname; // 사용할 닉네임
    }

    /**
     * 로그인 요청 시 프론트엔드에서 보내주는 데이터를 담는 클래스입니다.
     */
    @Data
    public static class LoginRequest {
        private String email;    // 로그인 이메일
        private String password; // 로그인 비밀번호
    }

    /**
     * 회원가입 및 로그인 성공 시 프론트엔드로 돌려줄 응답 데이터를 담는 클래스입니다.
     */
    @Data
    public static class Response {
        private Long id;         // 유저 고유 ID
        private String email;    // 유저 이메일
        private String nickname; // 유저 닉네임
        private String role;     // 유저 권한 (USER/ADMIN)
        
        // 인증에 사용할 JWT 토큰
        private String token;
        
        // 성공 메시지 등 간단한 정보를 담기 위한 필드
        private String message;
    }
}
