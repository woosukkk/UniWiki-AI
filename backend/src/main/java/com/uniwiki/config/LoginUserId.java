package com.uniwiki.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 파라미터에서 현재 로그인한 유저의 ID를 쉽게 꺼내쓰기 위한 커스텀 어노테이션입니다.
 * 사용 예: public ResponseEntity<?> getUserInfo(@LoginUserId Long userId)
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginUserId {
}
