package com.uniwiki.controller;

import com.uniwiki.config.LoginUserId;
import com.uniwiki.dto.WikiPostDto;
import com.uniwiki.service.AnswerWikiPromotionService;
import com.uniwiki.service.QuestionWikiPromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/wiki-promotions")
@RequiredArgsConstructor
public class AdminWikiPromotionController {

    private final AnswerWikiPromotionService promotionService;
    private final QuestionWikiPromotionService questionPromotionService;

    @PostMapping("/questions/{questionId}")
    public ResponseEntity<WikiPostDto.Response> promoteQuestion(
            @LoginUserId Long adminUserId,
            @PathVariable Long questionId
    ) {
        return ResponseEntity.ok(
                questionPromotionService.promoteByAdmin(adminUserId, questionId)
        );
    }

    @PostMapping("/answers/{answerId}")
    public ResponseEntity<WikiPostDto.Response> promoteAnswer(
            @LoginUserId Long adminUserId,
            @PathVariable Long answerId
    ) {
        return ResponseEntity.ok(
                promotionService.promoteByAdmin(adminUserId, answerId)
        );
    }
}
