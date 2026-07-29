package com.uniwiki.dto;

import lombok.Getter;

public class WikiPostLikeDto {

    @Getter
    public static class Response {

        private final Long wikiPostId;
        private final long likeCount;
        private final boolean liked;

        public Response(Long wikiPostId, long likeCount, boolean liked) {
            this.wikiPostId = wikiPostId;
            this.likeCount = likeCount;
            this.liked = liked;
        }
    }
}
