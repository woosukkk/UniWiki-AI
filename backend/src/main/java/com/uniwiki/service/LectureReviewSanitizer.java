package com.uniwiki.service;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class LectureReviewSanitizer {

    private static final Pattern EMAIL = Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE = Pattern.compile("(?<!\\d)(?:01[016789]|02|0[3-6][1-5])[- .]?\\d{3,4}[- .]?\\d{4}(?!\\d)");
    private static final Pattern STUDENT_ID = Pattern.compile("(?<!\\d)20\\d{6}(?!\\d)");
    private static final Pattern URL = Pattern.compile("https?://\\S+", Pattern.CASE_INSENSITIVE);
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern ABUSIVE = Pattern.compile("(씨발|ㅅㅂ|병신|개새끼|죽어)", Pattern.CASE_INSENSITIVE);
    private static final Pattern MEANINGFUL = Pattern.compile("[0-9A-Za-z가-힣]");

    public Result sanitize(int starRating, String content) {
        if (starRating < 1 || starRating > 5) {
            return Result.rejected("별점을 정상적으로 확인할 수 없습니다.");
        }
        if (content == null) {
            return Result.rejected("강의평 본문이 없습니다.");
        }

        String normalized = WHITESPACE.matcher(content).replaceAll(" ").trim();
        if (normalized.length() < 10 || !MEANINGFUL.matcher(normalized).find()) {
            return Result.rejected("정보량이 부족한 짧은 강의평입니다.");
        }
        if (ABUSIVE.matcher(normalized).find()) {
            return Result.rejected("모욕적 표현이 포함되어 있습니다.");
        }

        String sanitized = EMAIL.matcher(normalized).replaceAll("[이메일 삭제]");
        sanitized = PHONE.matcher(sanitized).replaceAll("[연락처 삭제]");
        sanitized = STUDENT_ID.matcher(sanitized).replaceAll("[학번 삭제]");
        sanitized = URL.matcher(sanitized).replaceAll("[외부 링크 삭제]");
        return Result.accepted(sanitized);
    }

    public record Result(boolean accepted, String content, String reason) {
        static Result accepted(String content) {
            return new Result(true, content, null);
        }

        static Result rejected(String reason) {
            return new Result(false, null, reason);
        }
    }
}
