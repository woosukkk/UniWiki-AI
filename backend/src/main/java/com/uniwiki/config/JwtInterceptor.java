package com.uniwiki.config;

import com.uniwiki.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 프론트엔드에서 API 요청 시 헤더에 담아보낸 JWT 토큰을 가로채서 검증하는 문지기입니다.
 */
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 프리플라이트(CORS OPTIONS) 요청은 무조건 통과
        if (request.getMethod().equals("OPTIONS")) {
            return true;
        }

        // 헤더에서 Authorization 값을 꺼냅니다 (형태: "Bearer eyJhbG...")
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7); // "Bearer " 뒷부분의 진짜 토큰만 추출
            try {
                // 토큰 검증 및 유저 ID 추출
                Long userId = jwtUtil.getUserIdFromToken(token);
                // 컨트롤러에서 쓸 수 있게 request에 담아둠
                request.setAttribute("userId", userId);
                return true; // 무사 통과
            } catch (Exception e) {
                // 토큰이 만료되었거나 조작된 경우
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Invalid or expired token");
                return false; // 컨트롤러로 못 가게 막음
            }
        }

        // 토큰이 아예 없는 경우
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("Authorization token is missing");
        return false;
    }
}
