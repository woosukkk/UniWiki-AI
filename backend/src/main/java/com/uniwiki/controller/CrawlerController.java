package com.uniwiki.controller;

import com.uniwiki.dto.EverytimeCrawlerRequestDto;
import com.uniwiki.service.EverytimeCrawlerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/crawl")
@RequiredArgsConstructor
public class CrawlerController {

    private final EverytimeCrawlerService everytimeCrawlerService;

    @PostMapping("/everytime")
    public ResponseEntity<String> crawlEverytime(@RequestBody EverytimeCrawlerRequestDto requestDto) {
        
        if (requestDto.getEtsid() == null || requestDto.getEtsid().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("에브리타임 쿠키(etsid)는 필수입니다.");
        }
        if (requestDto.getBoardUrl() == null) {
            return ResponseEntity.badRequest().body("크롤링할 게시판 URL이 필요합니다.");
        }
        if (!"Question".equalsIgnoreCase(requestDto.getTargetTable()) && !"WikiPost".equalsIgnoreCase(requestDto.getTargetTable())) {
            return ResponseEntity.badRequest().body("targetTable은 'Question' 또는 'WikiPost' 여야 합니다.");
        }

        try {
            everytimeCrawlerService.crawlAndSave(
                    requestDto.getEtsid(),
                    requestDto.getBoardUrl(),
                    requestDto.getTargetTable(),
                    requestDto.getCategoryId()
            );
            return ResponseEntity.ok("크롤링 성공");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("크롤링 실패: " + e.getMessage());
        }
    }
}
