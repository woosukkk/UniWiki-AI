package com.uniwiki.service;

import com.uniwiki.entity.OfficialWikiDocument;
import com.uniwiki.entity.WikiPost;
import com.uniwiki.repository.OfficialWikiDocumentRepository;
import com.uniwiki.repository.WikiPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Order(24)
@Slf4j
public class CourseMaterialDuplicateBootstrap implements ApplicationRunner {

    private static final Pattern COURSE_MATERIAL_TITLE =
            Pattern.compile("^20\\d{2}-[12] 수강편람 및 강의시간표$");

    private final WikiPostRepository wikiPostRepository;
    private final OfficialWikiDocumentRepository documentRepository;
    private final WikiVectorSyncService vectorSyncService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Map<String, List<WikiPost>> grouped = wikiPostRepository.findAll().stream()
                .filter(post -> COURSE_MATERIAL_TITLE.matcher(post.getTitle()).matches())
                .collect(Collectors.groupingBy(WikiPost::getTitle));
        int removed = 0;
        for (List<WikiPost> posts : grouped.values()) {
            if (posts.size() < 2) continue;
            posts.sort(Comparator.comparing(WikiPost::getId));
            WikiPost canonical = posts.get(0);
            for (WikiPost duplicate : posts.subList(1, posts.size())) {
                for (OfficialWikiDocument link :
                        documentRepository.findByWikiPost_IdOrderByRawDocument_IdAsc(duplicate.getId())) {
                    link.mergeInto(canonical, link.getTopicKey());
                }
                documentRepository.flush();
                vectorSyncService.enqueueDelete(duplicate.getId());
                wikiPostRepository.delete(duplicate);
                removed++;
            }
        }
        log.info("Course material duplicate cleanup completed: removed={}", removed);
    }
}
