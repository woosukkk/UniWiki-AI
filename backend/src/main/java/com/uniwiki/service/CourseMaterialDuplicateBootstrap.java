package com.uniwiki.service;

import com.uniwiki.entity.OfficialWikiDocument;
import com.uniwiki.entity.WikiPost;
import com.uniwiki.repository.OfficialWikiDocumentRepository;
import com.uniwiki.repository.OfficialAttachmentRepository;
import com.uniwiki.repository.RawOfficialDocumentRepository;
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
    private final OfficialAttachmentRepository attachmentRepository;
    private final RawOfficialDocumentRepository rawDocumentRepository;
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
            // Keep the most recently created post. It is linked to the canonical
            // article URL produced by the current collector; older rows used
            // page/query variants that are no longer discovered.
            posts.sort(Comparator.comparing(WikiPost::getId).reversed());
            WikiPost canonical = posts.get(0);
            for (WikiPost duplicate : posts.subList(1, posts.size())) {
                List<OfficialWikiDocument> links =
                        documentRepository.findByWikiPost_IdOrderByRawDocument_IdAsc(duplicate.getId());
                List<Long> rawDocumentIds = links.stream()
                        .map(link -> link.getRawDocument().getId())
                        .toList();
                documentRepository.deleteAll(links);
                documentRepository.flush();
                vectorSyncService.enqueueDelete(duplicate.getId());
                wikiPostRepository.delete(duplicate);
                wikiPostRepository.flush();
                attachmentRepository.deleteByRawDocument_IdIn(rawDocumentIds);
                attachmentRepository.flush();
                rawDocumentRepository.deleteAllById(rawDocumentIds);
                removed++;
            }
        }
        log.info("Course material duplicate cleanup completed: removed={}", removed);
    }
}
