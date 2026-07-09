package com.uniwiki.repository;

import com.uniwiki.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * DB의 users 테이블에 접근하기 위한 리포지토리 인터페이스입니다.
 * Spring Data JPA가 자동으로 데이터베이스 쿼리를 처리하는 구현체를 생성해 줍니다.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // 이메일을 기준으로 유저 정보를 조회합니다. (로그인 시 사용)
    Optional<User> findByEmail(String email);
    
    // 해당 이메일이 이미 존재하는지 확인합니다. (회원가입 시 중복 검사)
    boolean existsByEmail(String email);
    
    // 해당 닉네임이 이미 존재하는지 확인합니다. (회원가입 시 중복 검사)
    boolean existsByNickname(String nickname);
}
