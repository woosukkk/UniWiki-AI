package com.uniwiki.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * @LoginUserId 어노테이션이 붙은 파라미터에 대해, JwtInterceptor에서 저장해둔 userId를 꺼내서 주입해주는 리졸버입니다.
 */
@Component
public class LoginUserIdResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // @LoginUserId 어노테이션이 붙어있고, 타입이 Long인지 확인
        return parameter.hasParameterAnnotation(LoginUserId.class) &&
               Long.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        // JwtInterceptor에서 request.setAttribute("userId", userId)로 담은 값을 가져옵니다.
        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
        return request.getAttribute("userId");
    }
}
