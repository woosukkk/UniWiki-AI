package com.uniwiki.config;

import com.uniwiki.entity.User;
import com.uniwiki.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Order(2)
@Slf4j
@ConditionalOnProperty(name = "uniwiki.admin.bootstrap-enabled", havingValue = "true")
public class AdminAccountBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;

    @Value("${uniwiki.admin.bootstrap-email}")
    private String adminEmail;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        User user = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new IllegalStateException("관리자 승격 대상 계정을 찾을 수 없습니다."));
        if (!"ADMIN".equals(user.getRole())) {
            user.setRole("ADMIN");
        }
        log.info("Admin account bootstrap completed: userId={}", user.getId());
    }
}
