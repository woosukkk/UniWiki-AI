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
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CommunityPostWikiWorkflowService {

    private static final Pattern ABUSIVE = Pattern.compile(
            "(씨발|시발|ㅅㅂ|병신|ㅂㅅ|개새끼|개년|좆|존나|지랄|꺼져|닥쳐)",
            Pattern.CASE_INSENSITIVE
    );

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

    @Transactional
    public Result processPending() {
        List<RawCommunityPost> unmigrated = rawRepository.findUnmigrated();
        int rejected = 0;
        int accepted = 0;
        for (RawCommunityPost raw : unmigrated) {
            if (containsAbusiveLanguage(raw.getTitle()) || containsAbusiveLanguage(raw.getContent())) {
                raw.reject(0, "ABUSIVE_CONTENT");
                rejected++;
                continue;
            }
            raw.accept(0, EverytimeContentType.SCHOOL_LIFE, raw.getContent());
            publishQuestion(raw);
            accepted++;
        }
        return new Result(unmigrated.size(), accepted, rejected);
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

    private void publishQuestion(RawCommunityPost raw) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new IllegalStateException("Everytime question author was not found."));
        String rawTitle = raw.getTitle().strip();
        String title = rawTitle.endsWith("?")
                ? rawTitle
                : rawTitle + " 관련 정보가 궁금합니다";
        if (title.length() > 200) title = title.substring(0, 200);
        final String publishedTitle = title;

        String content = raw.getContent() + "\n\n"
                + "---\n"
                + "학생 커뮤니티에서 수집된 내용을 질문으로 정리했습니다. "
                + "경험이나 공식 근거를 포함한 답변을 남겨주세요.\n"
                + "추천 " + raw.getLikesCount() + "개 · 댓글 " + raw.getCommentsCount() + "개";

        Question question = questionRepository
                .findBySourceTypeAndSourceUrl("EVERYTIME", raw.getSourceUrl())
                .map(existing -> {
                    existing.updateImportedContent(publishedTitle, content, raw.getLikesCount());
                    return existing;
                })
                .orElseGet(() -> questionRepository.save(new Question(
                        author,
                        publishedTitle,
                        content,
                        "EVERYTIME",
                        raw.getSourceUrl(),
                        raw.getLikesCount()
                )));

        parseComments(raw.getCommentsJson()).stream()
                .map(String::trim)
                .filter(comment -> !comment.isBlank())
                .filter(comment -> !containsAbusiveLanguage(comment))
                .limit(30)
                .filter(comment -> !answerRepository.existsByQuestion_IdAndContent(question.getId(), comment))
                .forEach(comment -> answerRepository.save(new Answer(question, author, comment)));
    }

    private boolean containsAbusiveLanguage(String text) {
        return text != null && ABUSIVE.matcher(text).find();
    }

    private List<String> parseComments(String commentsJson) {
        if (commentsJson == null || commentsJson.isBlank()) return List.of();
        try {
            return objectMapper.readValue(commentsJson, new TypeReference<>() { });
        } catch (Exception ignored) {
            return List.of();
        }
    }

    public record Result(int processed, int accepted, int rejected) { }
    public record ResetResult(int deletedWikiPosts, long deletedRawPosts) { }
    public record CommunityDataStats(long wikiPosts, long rawPosts) { }
}
