package com.uniwiki.controller;

import com.uniwiki.config.LoginUserId;
import com.uniwiki.dto.UserDto;
import com.uniwiki.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 프론트엔드에서의 회원가입 및 로그인 관련 HTTP 요청을 받아 처리하는 컨트롤러입니다.
 */
@Tag(name = "유저 API", description = "회원가입 및 로그인 등 유저 관련 기능")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "회원가입", description = "신규 사용자의 회원가입을 처리합니다. 비밀번호는 암호화되어 저장됩니다.")
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody UserDto.SignupRequest request) {
        try {
            UserDto.Response response = userService.signup(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "로그인", description = "이메일과 비밀번호를 받아 JWT 토큰과 회원 정보를 반환합니다.")
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserDto.LoginRequest request) {
        try {
            UserDto.Response response = userService.login(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "내 정보 조회", description = "요청 헤더의 JWT 토큰을 검증하고 내 정보를 반환합니다.")
    @GetMapping("/me")
    public ResponseEntity<?> getUserInfo(@Parameter(hidden = true) @LoginUserId Long userId) {
        try {
            UserDto.Response response = userService.getUserInfo(userId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
