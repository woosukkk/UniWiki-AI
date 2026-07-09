package com.uniwiki.service;

import com.uniwiki.dto.UserDto;
import com.uniwiki.entity.User;
import com.uniwiki.repository.UserRepository;
import com.uniwiki.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 회원가입 및 로그인과 관련된 실질적인 비즈니스 로직을 처리하는 서비스 클래스입니다.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil; // JWT 생성 유틸리티 주입

    /**
     * 비밀번호를 간단하게 암호화(SHA-256)하는 메서드입니다.
     * (실무에서는 BCryptPasswordEncoder 등을 사용합니다)
     */
    private String hashPassword(String password) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("비밀번호 암호화 중 에러 발생");
        }
    }

    /**
     * 회원가입을 처리하는 메서드입니다.
     * @param request 회원가입 요청 데이터 (이메일, 비밀번호, 닉네임)
     * @return 가입 완료된 유저 정보 및 성공 메시지
     */
    public UserDto.Response signup(UserDto.SignupRequest request) {
        // 1. 이메일 중복 체크: 이미 가입된 이메일이 있는지 확인합니다.
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        
        // 2. 닉네임 중복 체크: 이미 사용 중인 닉네임인지 확인합니다.
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        // 3. User 엔티티 생성: DB에 저장할 객체를 만듭니다. (비밀번호 암호화 적용)
        User user = User.builder()
                .email(request.getEmail())
                .password(hashPassword(request.getPassword()))
                .nickname(request.getNickname())
                .role("USER")
                .build();

        // 4. DB에 저장: JPA를 통해 실제 데이터베이스에 회원 정보를 저장합니다.
        User savedUser = userRepository.save(user);

        // 5. JWT 토큰 발급
        String token = jwtUtil.generateToken(savedUser.getId(), savedUser.getEmail());

        // 6. 응답 데이터 생성 및 반환: 저장된 회원 정보와 토큰을 바탕으로 응답 DTO를 구성하여 리턴합니다.
        UserDto.Response response = new UserDto.Response();
        response.setId(savedUser.getId());
        response.setEmail(savedUser.getEmail());
        response.setNickname(savedUser.getNickname());
        response.setRole(savedUser.getRole());
        response.setToken(token); // 토큰 추가
        response.setMessage("회원가입이 완료되었습니다.");
        
        return response;
    }

    /**
     * 로그인을 처리하는 메서드입니다.
     * @param request 로그인 요청 데이터 (이메일, 비밀번호)
     * @return 로그인 성공 시 유저 정보 및 성공 메시지
     */
    public UserDto.Response login(UserDto.LoginRequest request) {
        // 1. 이메일로 유저 조회: 입력받은 이메일로 가입된 유저가 있는지 DB에서 찾습니다.
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
        
        // 2. 유저가 존재하지 않는 경우 예외를 발생시킵니다.
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("가입되지 않은 이메일입니다.");
        }
        
        User user = userOpt.get();
        
        // 3. 비밀번호 확인: DB에 저장된 비밀번호(해시)와 입력된 비밀번호(해시)가 일치하는지 비교합니다.
        if (!user.getPassword().equals(hashPassword(request.getPassword()))) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // 4. 로그인 성공 시 JWT 토큰 발급
        String token = jwtUtil.generateToken(user.getId(), user.getEmail());

        // 5. 응답 데이터 반환: 일치한다면 토큰과 함께 로그인 성공 메시지와 유저 정보를 리턴합니다.
        UserDto.Response response = new UserDto.Response();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setNickname(user.getNickname());
        response.setRole(user.getRole());
        response.setToken(token); // 토큰 추가
        response.setMessage("로그인 성공");
        
        return response;
    }

    /**
     * 내 정보 조회를 처리하는 메서드입니다.
     */
    public UserDto.Response getUserInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        UserDto.Response response = new UserDto.Response();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setNickname(user.getNickname());
        response.setRole(user.getRole());
        // 정보 조회 시 토큰은 빈 값으로 두거나 기존 토큰을 반환하지 않음
        response.setMessage("내 정보 조회 성공");
        
        return response;
    }
}
