package com.uniwiki.service;

import com.uniwiki.entity.*;
import com.uniwiki.repository.CategoryRepository;
import com.uniwiki.repository.LectureReviewWikiDraftRepository;
import com.uniwiki.repository.RawLectureEvaluationRepository;
import com.uniwiki.repository.UserRepository;
import com.uniwiki.repository.WikiPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class LectureReviewWorkflowService {

    private static final String SUMMARY = "학생 강의평을 개인정보·저품질 필터링 후 집계한 관리자 검토용 초안입니다.";

    private final RawLectureEvaluationRepository rawRepository;
    private final LectureReviewWikiDraftRepository draftRepository;
    private final WikiPostRepository wikiPostRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final LectureReviewSanitizer sanitizer;
    private final WikiVectorSyncService vectorSyncService;

    @Value("${uniwiki.lecture-review-workflow.author-id:1}")
    private Long authorId;

    @Value("${uniwiki.lecture-review-workflow.category-name:교과목}")
    private String categoryName;

    @Value("${uniwiki.lecture-review-workflow.max-reviews-per-draft:100}")
    private int maxReviewsPerDraft;

    @Transactional
    public Result processPending() {
        List<RawLectureEvaluation> pending = rawRepository.findTop100ByIsProcessedFalseOrderByIdAsc();
        if (pending.isEmpty()) {
            return new Result(0, 0, 0);
        }

        Category category = categoryRepository.findByName(categoryName)
                .orElseThrow(() -> new IllegalStateException("강의평 초안 카테고리를 찾을 수 없습니다: " + categoryName));
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new IllegalStateException("강의평 초안 작성자를 찾을 수 없습니다: " + authorId));

        int accepted = 0;
        int rejected = 0;
        for (RawLectureEvaluation raw : pending) {
            LectureReviewSanitizer.Result sanitized = sanitizer.sanitize(raw.getStarRating(), raw.getContent());
            if (!sanitized.accepted()) {
                raw.reject(sanitized.reason());
                rejected++;
                continue;
            }
            raw.accept(sanitized.content());
            refreshDraft(raw.getCourseName(), raw.getProfessor(), category, author);
            accepted++;
        }
        return new Result(pending.size(), accepted, rejected);
    }

    private void refreshDraft(String courseName, String professor, Category category, User author) {
        List<RawLectureEvaluation> acceptedReviews = rawRepository
                .findByCourseNameAndProfessorAndProcessingStatusOrderByIdAsc(
                        courseName, professor, LectureReviewProcessingStatus.ACCEPTED);
        int fromIndex = Math.max(0, acceptedReviews.size() - maxReviewsPerDraft);
        List<RawLectureEvaluation> included = acceptedReviews.subList(fromIndex, acceptedReviews.size());
        String title = courseName + " - " + professor + " 강의평 요약 초안";
        String content = buildContent(courseName, professor, acceptedReviews, included);

        LectureReviewWikiDraft link = draftRepository.findByCourseNameAndProfessor(courseName, professor).orElse(null);
        if (link == null) {
            WikiPost wikiPost = wikiPostRepository.save(new WikiPost(
                    category, author, title, content, SUMMARY, WikiPostStatus.PENDING));
            draftRepository.save(new LectureReviewWikiDraft(courseName, professor, wikiPost, included.size()));
            return;
        }

        WikiPost wikiPost = link.getWikiPost();
        if (wikiPost.getStatus() == WikiPostStatus.APPROVED) {
            vectorSyncService.enqueueDelete(wikiPost.getId());
        }
        wikiPost.update(category, title, content, SUMMARY, WikiPostStatus.PENDING);
        link.refresh(included.size());
    }

    private String buildContent(
            String courseName,
            String professor,
            List<RawLectureEvaluation> all,
            List<RawLectureEvaluation> included
    ) {
        double average = all.stream().mapToInt(RawLectureEvaluation::getStarRating).average().orElse(0.0);
        StringBuilder content = new StringBuilder()
                .append("# ").append(courseName).append(" - ").append(professor).append(" 강의평\n\n")
                .append("익명 학생 강의평을 자동 정제한 관리자 검토용 초안입니다. ")
                .append("개인 경험에 기반한 정보이므로 사실로 단정하지 말고 참고용으로만 사용해야 합니다.\n\n")
                .append("- 정제 통과 강의평: ").append(all.size()).append("개\n")
                .append("- 평균 별점: ").append(String.format(Locale.ROOT, "%.2f", average)).append(" / 5.00\n")
                .append("- 본문 포함 강의평: 최근 ").append(included.size()).append("개\n\n")
                .append("## 정제된 강의평\n\n");

        for (RawLectureEvaluation review : included) {
            String safeContent = review.getSanitizedContent().replace("|", "\\|");
            content.append("- **").append(review.getStarRating()).append("점** ")
                    .append(safeContent).append("\n");
        }
        content.append("\n## 출처 및 검토 안내\n\n")
                .append("- 출처: 에브리타임에서 수집한 익명 강의평 원시 데이터\n")
                .append("- 개인정보와 저품질 표현은 자동 필터링되었으나 게시 전 관리자가 다시 검토해야 합니다.\n")
                .append("- 승인 전에는 공개 위키 및 AI 벡터 검색에 포함되지 않습니다.\n");
        return content.toString();
    }

    public record Result(int processed, int accepted, int rejected) {
    }
}
