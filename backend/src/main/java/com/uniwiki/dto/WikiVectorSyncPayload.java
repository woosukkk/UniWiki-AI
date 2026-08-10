package com.uniwiki.dto;

import com.uniwiki.entity.WikiPost;

public record WikiVectorSyncPayload(
        Long wikiPostId,
        String title,
        String content,
        Long categoryId,
        String sourceKey
) {

    public static WikiVectorSyncPayload from(WikiPost wikiPost) {
        return new WikiVectorSyncPayload(
                wikiPost.getId(),
                wikiPost.getTitle(),
                wikiPost.getContent(),
                wikiPost.getCategory().getId(),
                sourceKey(wikiPost.getTitle())
        );
    }

    private static String sourceKey(String title) {
        String compact = title.replaceAll("\\s+", "");
        if (compact.contains("소프트웨어학과") && compact.contains("졸업")) {
            return "software-graduation-requirements";
        }
        if (compact.contains("등록금납부와등록안내")
                || (compact.contains("등록금") && compact.contains("기본"))) {
            return "tuition-policy";
        }
        if (compact.contains("장학금") && (compact.contains("기본")
                || compact.contains("교내") || compact.contains("제도"))) {
            return "scholarship-policy";
        }
        return "";
    }
}
