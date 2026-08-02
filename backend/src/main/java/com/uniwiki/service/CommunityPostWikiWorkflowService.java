package com.uniwiki.service;

import com.uniwiki.entity.*;
import com.uniwiki.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CommunityPostWikiWorkflowService {

    private static final Pattern PERSONAL = Pattern.compile(
            "[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}|(?<!\\d)01[016789][- .]?\\d{3,4}[- .]?\\d{4}(?!\\d)|(?<!\\d)20\\d{6}(?!\\d)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern URL = Pattern.compile("https?://\\S+", Pattern.CASE_INSENSITIVE);
    private static final Pattern ABUSIVE = Pattern.compile("(씨발|ㅅㅂ|병신|개새끼|죽어|실명|신상)", Pattern.CASE_INSENSITIVE);
    private static final Pattern LOW_VALUE = Pattern.compile("(ㅋㅋㅋ|ㅎㅎㅎ|밈|유머|웃기|연애|외모|이상형|심심|뻘글|어그로)");

    private final RawCommunityPostRepository rawRepository;
    private final EverytimeWikiDocumentRepository documentRepository;
    private final WikiPostRepository wikiPostRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final WikiVectorSyncService vectorSyncService;

    @Value("${uniwiki.lecture-review-workflow.author-id:1}")
    private Long authorId;

    @Value("${uniwiki.everytime-community.minimum-score:50}")
    private int minimumScore;

    @Transactional
    public Result processPending() {
        List<RawCommunityPost> pending = rawRepository.findTop100ByIsProcessedFalseOrderByIdAsc();
        int accepted = 0;
        int rejected = 0;
        for (RawCommunityPost raw : pending) {
            Evaluation evaluation = evaluate(raw);
            if (!evaluation.accepted()) {
                raw.reject(evaluation.score(), evaluation.reason());
                rejected++;
                continue;
            }
            raw.accept(evaluation.score(), evaluation.contentType(), evaluation.sanitizedContent());
            publish(raw);
            accepted++;
        }
        return new Result(pending.size(), accepted, rejected);
    }

    private Evaluation evaluate(RawCommunityPost raw) {
        String combined = (raw.getTitle() + " " + raw.getContent()).replaceAll("\\s+", " ").trim();
        if (combined.length() < 30) return Evaluation.rejected(0, "정보량이 부족합니다.");
        if (ABUSIVE.matcher(combined).find()) return Evaluation.rejected(0, "개인 저격 또는 모욕 표현이 포함되어 있습니다.");

        int score = combined.length() >= 80 ? 20 : 5;
        if (raw.getLikesCount() >= 10) score += 20;
        else if (raw.getLikesCount() >= 5) score += 10;
        if (raw.getCommentsCount() >= 8) score += 15;
        else if (raw.getCommentsCount() >= 3) score += 5;

        EverytimeContentType type = classify(combined);
        score += keywordScore(type, combined);
        if (LOW_VALUE.matcher(combined).find()) score -= 50;
        if (score < minimumScore) return Evaluation.rejected(score, "정보성 점수가 기준보다 낮습니다.");

        String sanitized = PERSONAL.matcher(combined).replaceAll("[개인정보 삭제]");
        sanitized = URL.matcher(sanitized).replaceAll("[외부 링크 삭제]");
        if (sanitized.length() > 1200) sanitized = sanitized.substring(0, 1200) + "…";
        return Evaluation.accepted(score, type, sanitized);
    }

    private EverytimeContentType classify(String text) {
        if (containsAny(text, "수강", "학점", "졸업", "전공", "복수전공", "학사", "시험")) return EverytimeContentType.ACADEMIC;
        if (containsAny(text, "장학", "지원금", "국가장학")) return EverytimeContentType.SCHOLARSHIP;
        if (containsAny(text, "취업", "인턴", "채용", "면접", "공모전")) return EverytimeContentType.CAREER;
        if (containsAny(text, "도서관", "학식", "기숙사", "통학", "셔틀", "시설")) return EverytimeContentType.FACILITIES;
        if (containsAny(text, "동아리", "행사", "축제", "학생회")) return EverytimeContentType.CLUB_EVENT;
        return EverytimeContentType.SCHOOL_LIFE;
    }

    private int keywordScore(EverytimeContentType type, String text) {
        int matches = switch (type) {
            case ACADEMIC -> countMatches(text, "수강", "학점", "졸업", "전공", "학사", "시험");
            case SCHOLARSHIP -> countMatches(text, "장학", "지원금", "신청", "서류");
            case CAREER -> countMatches(text, "취업", "인턴", "채용", "면접", "공모전");
            case FACILITIES -> countMatches(text, "도서관", "학식", "기숙사", "통학", "셔틀", "시설");
            case CLUB_EVENT -> countMatches(text, "동아리", "행사", "축제", "학생회");
            default -> countMatches(text, "학교", "학생", "이용", "신청", "방법", "정보");
        };
        return Math.min(45, matches * 15);
    }

    private void publish(RawCommunityPost raw) {
        Category category = categoryRepository.findByName("학교생활")
                .or(() -> categoryRepository.findByName("교과목"))
                .orElseThrow(() -> new IllegalStateException("에브리타임 위키 카테고리를 찾을 수 없습니다."));
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new IllegalStateException("에브리타임 위키 작성자를 찾을 수 없습니다."));
        String title = raw.getTitle().length() > 180 ? raw.getTitle().substring(0, 180) : raw.getTitle();
        String content = "# " + title + "\n\n"
                + "## 정제된 핵심 내용\n\n" + raw.getSanitizedContent() + "\n\n"
                + "## 자료 성격\n\n"
                + "- 학생 커뮤니티의 경험성 정보를 자동 정제한 자료입니다.\n"
                + "- 정보성 점수: " + raw.getUsefulnessScore() + "점\n"
                + "- 추천 " + raw.getLikesCount() + "개, 댓글 " + raw.getCommentsCount() + "개\n"
                + "- 공식 규정이나 공지는 학교 홈페이지에서 다시 확인해야 합니다.\n";
        String summary = "에브리타임 핫 게시판의 정보성 게시물을 개인정보·잡담 필터링 후 정리한 자료입니다.";

        WikiPost wikiPost = wikiPostRepository.save(new WikiPost(
                category, author, title, content, summary, WikiPostStatus.APPROVED));
        documentRepository.save(new EverytimeWikiDocument(raw.getSourceUrl(), raw.getContentType(), wikiPost));
        vectorSyncService.enqueueUpsert(wikiPost);
    }

    private boolean containsAny(String text, String... keywords) {
        return java.util.Arrays.stream(keywords).anyMatch(text::contains);
    }

    private int countMatches(String text, String... keywords) {
        return (int) java.util.Arrays.stream(keywords).filter(text::contains).count();
    }

    public record Result(int processed, int accepted, int rejected) { }
    private record Evaluation(boolean accepted, int score, EverytimeContentType contentType,
                              String sanitizedContent, String reason) {
        static Evaluation accepted(int score, EverytimeContentType type, String content) {
            return new Evaluation(true, score, type, content, null);
        }
        static Evaluation rejected(int score, String reason) {
            return new Evaluation(false, score, null, null, reason);
        }
    }
}
