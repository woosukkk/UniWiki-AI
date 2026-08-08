package com.uniwiki.config;

import com.uniwiki.entity.Category;
import com.uniwiki.entity.User;
import com.uniwiki.entity.WikiPost;
import com.uniwiki.entity.WikiPostStatus;
import com.uniwiki.repository.CategoryRepository;
import com.uniwiki.repository.UserRepository;
import com.uniwiki.repository.WikiPostRepository;
import com.uniwiki.service.WikiVectorSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "uniwiki.academic-regulation.bootstrap-enabled",
        havingValue = "true"
)
public class AcademicRegulationWikiBootstrap implements ApplicationRunner {

    private static final String TITLE = "세종대학교 학사내규 (2025.08.22 시행)";
    private static final String SUMMARY =
            "등록, 수강신청, 재수강, 성적, 휴복학, 출결과 계절수업 등 학사 운영 기준을 정리한 공식 학사내규입니다.";
    private static final String RESOURCE = "sejong-academic-regulations-2025.md";

    private final WikiPostRepository wikiPostRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final WikiVectorSyncService vectorSyncService;

    @Value("${uniwiki.official-sources.author-id:1}")
    private Long authorId;

    @Value("${uniwiki.official-sources.author-email:official-source@local.invalid}")
    private String authorEmail;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        Category category = categoryRepository.findByName("학사")
                .orElseThrow(() -> new IllegalStateException("학사 카테고리를 찾을 수 없습니다."));
        User author = userRepository.findByEmail(authorEmail)
                .or(() -> userRepository.findById(authorId))
                .orElseThrow(() -> new IllegalStateException("공식 위키 작성자를 찾을 수 없습니다."));
        String content = new ClassPathResource(RESOURCE)
                .getContentAsString(StandardCharsets.UTF_8);

        WikiPost post = wikiPostRepository.findByTitle(TITLE)
                .orElseGet(() -> wikiPostRepository.save(new WikiPost(
                        category, author, TITLE, content, SUMMARY, WikiPostStatus.APPROVED
                )));
        post.update(category, TITLE, content, SUMMARY, WikiPostStatus.APPROVED);
        vectorSyncService.enqueueUpsert(post);
        log.info("Academic regulation wiki upserted: wikiPostId={}", post.getId());
    }
}
