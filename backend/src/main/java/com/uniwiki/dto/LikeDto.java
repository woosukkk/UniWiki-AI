package com.uniwiki.dto;

import lombok.Getter;

public class LikeDto {

    @Getter
    public static class Response {

        private final Long questionId;
        private final long likeCount;
        private final boolean liked;

        public Response(Long questionId, long likeCount, boolean liked) {
            this.questionId = questionId;
            this.likeCount = likeCount;
            this.liked = liked;
        }
    }
}
