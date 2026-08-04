package com.uniwiki.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Arrays;

/**
 * Spring Boot 웹 설정 파일입니다.
 * 우리가 만든 JwtInterceptor와 LoginUserIdResolver를 스프링에 등록합니다.
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;
    private final LoginUserIdResolver loginUserIdResolver;

    @Value("${uniwiki.cors.allowed-origin-patterns:http://localhost:5173}")
    private String allowedOriginPatterns;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(loginUserIdResolver);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(Arrays.stream(allowedOriginPatterns.split(","))
                        .map(String::trim)
                        .filter(origin -> !origin.isEmpty())
                        .toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
