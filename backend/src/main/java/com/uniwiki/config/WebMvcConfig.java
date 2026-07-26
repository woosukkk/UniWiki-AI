package com.uniwiki.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Spring Boot 웹 설정 파일입니다.
 * 우리가 만든 JwtInterceptor와 LoginUserIdResolver를 스프링에 등록합니다.
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;
    private final LoginUserIdResolver loginUserIdResolver;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns(
                        "/api/users/me",
                        "/api/answers/**"
                );
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(loginUserIdResolver);
    }
}
