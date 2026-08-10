package com.uniwiki.dto;

import com.uniwiki.entity.WikiPost;

public record WikiVectorSyncPayload(
        Long wikiPostId,
        String title,
        String content,
        Long categoryId
) {

    public static WikiVectorSyncPayload from(WikiPost wikiPost) {
        return new WikiVectorSyncPayload(
                wikiPost.getId(),
                wikiPost.getTitle(),
                wikiPost.getContent(),
                wikiPost.getCategory().getId()
        );
    }
}
