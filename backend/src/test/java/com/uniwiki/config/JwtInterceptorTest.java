package com.uniwiki.config;

import com.uniwiki.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.method.HandlerMethod;

import java.io.PrintWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtInterceptorTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private JwtInterceptor jwtInterceptor;

    @Test
    void allowsPublicControllerMethodWithoutToken() throws Exception {
        HandlerMethod handler = new HandlerMethod(
                new TestController(),
                TestController.class.getDeclaredMethod("publicEndpoint")
        );
        when(request.getMethod()).thenReturn("GET");

        boolean allowed = jwtInterceptor.preHandle(request, response, handler);

        assertThat(allowed).isTrue();
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void rejectsProtectedControllerMethodWithoutToken() throws Exception {
        HandlerMethod handler = new HandlerMethod(
                new TestController(),
                TestController.class.getDeclaredMethod("protectedEndpoint", Long.class)
        );
        PrintWriter writer = mock(PrintWriter.class);
        when(request.getMethod()).thenReturn("POST");
        when(response.getWriter()).thenReturn(writer);

        boolean allowed = jwtInterceptor.preHandle(request, response, handler);

        assertThat(allowed).isFalse();
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void injectsUserIdForValidToken() throws Exception {
        HandlerMethod handler = new HandlerMethod(
                new TestController(),
                TestController.class.getDeclaredMethod("protectedEndpoint", Long.class)
        );
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtUtil.getUserIdFromToken("valid-token")).thenReturn(1L);

        boolean allowed = jwtInterceptor.preHandle(request, response, handler);

        assertThat(allowed).isTrue();
        verify(request).setAttribute("userId", 1L);
    }

    private static class TestController {

        public void publicEndpoint() {
        }

        public void protectedEndpoint(@LoginUserId Long userId) {
        }
    }
}
