package com.uniwiki.dto;

import lombok.Getter;

public class AnswerLikeDto {

    @Getter
    public static class Response {

        private final Long answerId;
        private final long likeCount;
        private final boolean liked;

        public Response(Long answerId, long likeCount, boolean liked) {
            this.answerId = answerId;
            this.likeCount = likeCount;
            this.liked = liked;
        }
    }
}
