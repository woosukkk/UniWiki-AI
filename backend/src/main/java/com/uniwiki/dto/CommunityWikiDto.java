package com.uniwiki.dto;

import com.uniwiki.entity.AnswerWikiPromotion;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class CommunityWikiDto {

    @Getter
    public static class EntryResponse {
        private final Long questionId;
        private final String questionTitle;
        private final String questionContent;
        private final String questionAuthorNickname;
        private final LocalDateTime questionCreatedAt;
        private final Long selectedAnswerId;
        private final String selectedAnswerContent;
        private final Long wikiPostId;
        private final LocalDate promotedDate;
        private final LocalDateTime promotedAt;

        private EntryResponse(AnswerWikiPromotion promotion) {
            var answer = promotion.getAnswer();
            var question = answer.getQuestion();
            this.questionId = question.getId();
            this.questionTitle = question.getTitle();
            this.questionContent = question.getContent();
            this.questionAuthorNickname = question.getAuthor().getNickname();
            this.questionCreatedAt = question.getCreatedAt();
            this.selectedAnswerId = answer.getId();
            this.selectedAnswerContent = answer.getContent();
            this.wikiPostId = promotion.getWikiPost().getId();
            this.promotedDate = promotion.getPromotedAt().toLocalDate();
            this.promotedAt = promotion.getPromotedAt();
        }

        public static EntryResponse from(AnswerWikiPromotion promotion) {
            return new EntryResponse(promotion);
        }
    }
}
