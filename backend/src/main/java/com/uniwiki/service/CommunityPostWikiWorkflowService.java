package com.uniwiki.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final UserRepository userRepository;
    private final WikiVectorSyncService vectorSyncService;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final ObjectMapper objectMapper;

    @Value("${uniwiki.everytime-community.author-id:1}")
    private Long authorId;

    @Value("${uniwiki.everytime-community.minimum-score:50}")
    private int minimumScore;

    @Transactional
    public Result processPending() {
        List<RawCommunityPost> acceptedLegacy = rawRepository
                .findUnmigratedByStatus(CommunityPostProcessingStatus.ACCEPTED);
        acceptedLegacy.forEach(this::publishQuestion);

        List<RawCommunityPost> pending = rawRepository.findTop100ByIsProcessedFalseOrderByIdAsc();
        int accepted = acceptedLegacy.size();
        int rejected = 0;
        for (RawCommunityPost raw : pending) {
            Evaluation evaluation = evaluate(raw);
            if (!evaluation.accepted()) {
                raw.reject(evaluation.score(), evaluation.reason());
                rejected++;
                continue;
            }
            raw.accept(evaluation.score(), evaluation.contentType(), evaluation.sanitizedContent());
            publishQuestion(raw);
            accepted++;
        }
        return new Result(pending.size() + acceptedLegacy.size(), accepted, rejected);
    }

    @Transactional
    public ResetResult resetCommunityData() {
        List<EverytimeWikiDocument> documents = documentRepository.findAll().stream()
                .filter(document -> document.getContentType() != EverytimeContentType.LECTURE_REVIEW)
                .toList();
        List<WikiPost> wikiPosts = documents.stream()
                .map(EverytimeWikiDocument::getWikiPost)
                .toList();

        wikiPosts.forEach(wikiPost -> vectorSyncService.enqueueDelete(wikiPost.getId()));
        documentRepository.deleteAll(documents);
        documentRepository.flush();
        wikiPostRepository.deleteAll(wikiPosts);

        long rawPostCount = rawRepository.count();
        rawRepository.deleteAllInBatch();
        return new ResetResult(wikiPosts.size(), rawPostCount);
    }

    @Transactional(readOnly = true)
    public CommunityDataStats getCommunityDataStats() {
        long wikiPostCount = documentRepository.findAll().stream()
                .filter(document -> document.getContentType() != EverytimeContentType.LECTURE_REVIEW)
                .count();
        return new CommunityDataStats(wikiPostCount, rawRepository.count());
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
        EverytimeContentType bestType = EverytimeContentType.SCHOOL_LIFE;
        int bestScore = 0;
        for (EverytimeContentType candidate : List.of(
                EverytimeContentType.SCHOLARSHIP,
                EverytimeContentType.CAREER,
                EverytimeContentType.FACILITIES,
                EverytimeContentType.CLUB_EVENT,
                EverytimeContentType.ACADEMIC)) {
            int candidateScore = keywordMatchCount(candidate, text);
            if (candidateScore > bestScore) {
                bestType = candidate;
                bestScore = candidateScore;
            }
        }
        return bestType;
    }

    private int keywordScore(EverytimeContentType type, String text) {
        return Math.min(45, keywordMatchCount(type, text) * 15);
    }

    private int keywordMatchCount(EverytimeContentType type, String text) {
        return switch (type) {
            case ACADEMIC -> countMatches(text, "수강", "학점", "졸업", "전공", "학사", "시험");
            case SCHOLARSHIP -> countMatches(text, "장학", "지원금", "신청", "서류");
            case CAREER -> countMatches(text, "취업", "진로", "인턴", "채용", "면접", "자소서", "포트폴리오", "현장실습", "공모전");
            case FACILITIES -> countMatches(text, "도서관", "학식", "기숙사", "통학", "셔틀", "시설");
            case CLUB_EVENT -> countMatches(text, "동아리", "학회", "소모임", "모집", "행사", "축제", "학생회");
            default -> countMatches(text, "학교", "학생", "이용", "신청", "방법", "정보");
        };
    }

    private void publishQuestion(RawCommunityPost raw) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new IllegalStateException("Everytime question author was not found."));
        String rawTitle = raw.getTitle().strip();
        String title = rawTitle.endsWith("?")
                ? rawTitle
                : rawTitle + " 관련 정보가 궁금합니다";
        if (title.length() > 200) title = title.substring(0, 200);

        String content = raw.getSanitizedContent() + "\n\n"
                + "---\n"
                + "학생 커뮤니티에서 수집된 내용을 질문으로 정리했습니다. "
                + "경험이나 공식 근거를 포함한 답변을 남겨주세요.\n"
                + "정보성 점수 " + raw.getUsefulnessScore() + "점 · 추천 "
                + raw.getLikesCount() + "개 · 댓글 " + raw.getCommentsCount() + "개";

        Question question = questionRepository.save(new Question(
                author,
                title,
                content,
                "EVERYTIME",
                raw.getSourceUrl(),
                raw.getLikesCount()
        ));

        parseComments(raw.getCommentsJson()).stream()
                .map(this::sanitizeComment)
                .filter(comment -> !comment.isBlank())
                .limit(30)
                .forEach(comment -> answerRepository.save(new Answer(question, author, comment)));
    }

    private List<String> parseComments(String commentsJson) {
        if (commentsJson == null || commentsJson.isBlank()) return List.of();
        try {
            return objectMapper.readValue(commentsJson, new TypeReference<>() { });
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private String sanitizeComment(String comment) {
        String sanitized = PERSONAL.matcher(comment).replaceAll("[개인정보 제거]");
        sanitized = URL.matcher(sanitized).replaceAll("[외부 링크 제거]");
        sanitized = sanitized.replaceAll("\\s+", " ").trim();
        return sanitized.length() > 1000 ? sanitized.substring(0, 1000) : sanitized;
    }

    private boolean containsAny(String text, String... keywords) {
        return java.util.Arrays.stream(keywords).anyMatch(text::contains);
    }

    private int countMatches(String text, String... keywords) {
        return (int) java.util.Arrays.stream(keywords).filter(text::contains).count();
    }

    public record Result(int processed, int accepted, int rejected) { }
    public record ResetResult(int deletedWikiPosts, long deletedRawPosts) { }
    public record CommunityDataStats(long wikiPosts, long rawPosts) { }
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
