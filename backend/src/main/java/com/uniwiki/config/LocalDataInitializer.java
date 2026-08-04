package com.uniwiki.config;

import com.uniwiki.entity.Category;
import com.uniwiki.entity.User;
import com.uniwiki.repository.CategoryRepository;
import com.uniwiki.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@Profile("local")
@Order(0)
@RequiredArgsConstructor
public class LocalDataInitializer implements ApplicationRunner {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Override
    public void run(ApplicationArguments args) {
        categories().forEach(definition -> categoryRepository.findByName(definition.name())
                .orElseGet(() -> categoryRepository.save(
                        new Category(definition.name(), definition.description()))));

        userRepository.findByEmail("official-source@local.invalid")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("official-source@local.invalid")
                        .password(UUID.randomUUID().toString())
                        .nickname("세종대 공식자료")
                        .role("USER")
                        .build()));
    }

    private List<CategoryDefinition> categories() {
        return List.of(
                new CategoryDefinition("학사", "학사일정, 등록, 수강신청 등 학교생활 정보"),
                new CategoryDefinition("교과목", "전공 및 교양 과목 정보"),
                new CategoryDefinition("교수님", "교수진 및 연구실 정보"),
                new CategoryDefinition("졸업요건", "졸업학점, 필수과목, 졸업시험 정보"),
                new CategoryDefinition("인증제도", "졸업 및 소프트웨어 인증 정보"),
                new CategoryDefinition("장학·지원", "장학금과 학생 지원 제도"),
                new CategoryDefinition("진로·취업", "진로상담, 채용 및 취업 프로그램"),
                new CategoryDefinition("현장실습", "현장실습과 인턴십 정보"),
                new CategoryDefinition("프로젝트", "공모전 및 팀 프로젝트 정보"),
                new CategoryDefinition("학교생활", "학과 및 학교생활 정보"),
                new CategoryDefinition("FAQ", "자주 묻는 질문")
        );
    }

    private record CategoryDefinition(String name, String description) { }
}
