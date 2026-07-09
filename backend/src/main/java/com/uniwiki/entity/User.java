package com.uniwiki.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * DB의 'users' 테이블과 매핑되는 엔티티 클래스입니다.
 * 사용자의 회원가입 및 로그인 정보를 담고 있습니다.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    // 유저의 고유 식별자 (자동 증가)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 로그인에 사용될 이메일 (중복 불가)
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    // 사용자 비밀번호 (학교 제출용으로 평문 저장 사용)
    @Column(nullable = false, length = 255)
    private String password;

    // 서비스에서 사용할 닉네임 (중복 불가)
    @Column(nullable = false, unique = true, length = 50)
    private String nickname;

    // 사용자 권한 ('USER' 또는 'ADMIN')
    @Column(nullable = false, length = 10)
    private String role;

    // 계정 생성 일시 (수정 불가)
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 계정 정보 마지막 수정 일시
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 엔티티가 처음 DB에 저장되기 전에 자동으로 실행되는 메서드입니다.
     * 가입 시간과 기본 권한(USER)을 설정해 줍니다.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.role == null) {
            this.role = "USER";
        }
    }

    /**
     * 엔티티 정보가 업데이트되기 전에 자동으로 실행되는 메서드입니다.
     * 수정 시간을 현재 시간으로 갱신합니다.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
