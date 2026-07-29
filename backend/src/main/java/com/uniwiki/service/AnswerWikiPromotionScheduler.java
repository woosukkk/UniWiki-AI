package com.uniwiki.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AnswerWikiPromotionScheduler {

    private final AnswerWikiPromotionService promotionService;

    @Scheduled(cron = "${uniwiki.promotion.cron:0 0 3 * * *}")
    public void promoteEligibleAnswers() {
        promotionService.promoteEligibleAnswers();
    }
}
