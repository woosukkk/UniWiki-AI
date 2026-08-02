package com.uniwiki.controller;

import com.uniwiki.service.EverytimeCrawlerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.uniwiki.dto.EverytimeBoardRequestDto;
import com.uniwiki.dto.EverytimeLectureRequestDto;
import com.uniwiki.dto.EverytimeLectureBatchRequestDto;
import com.uniwiki.dto.EverytimeLectureBatchResponseDto;
import com.uniwiki.service.EverytimeLectureBatchService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/crawl")
@RequiredArgsConstructor
public class CrawlerController {

    private final EverytimeCrawlerService everytimeCrawlerService;
    private final com.uniwiki.repository.RawLectureEvaluationRepository rawLectureEvaluationRepository;
    private final com.uniwiki.repository.RawCommunityPostRepository rawCommunityPostRepository;
    private final EverytimeLectureBatchService everytimeLectureBatchService;

    @PostMapping("/everytime/board")
    public ResponseEntity<String> crawlEverytimeBoard(@RequestBody EverytimeBoardRequestDto requestDto) {
        if (requestDto.getBoardUrl() == null) {
            return ResponseEntity.badRequest().body("크롤링할 게시판 URL이 필요합니다.");
        }

        try {
            int startPage = requestDto.getStartPage() != null ? requestDto.getStartPage() : 1;
            int endPage = requestDto.getEndPage() != null ? requestDto.getEndPage() : 1;
            everytimeCrawlerService.crawlBoardAndSave(
                    requestDto.getBoardUrl(),
                    requestDto.getBoardType(),
                    startPage,
                    endPage,
                    requestDto.getTitleKeywords(),
                    requestDto.getContentKeywords()
            );
            return ResponseEntity.ok("게시판 원시 데이터 크롤링 성공");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("크롤링 실패: " + e.getMessage());
        }
    }

    @PostMapping("/everytime/lecture")
    public ResponseEntity<String> crawlEverytimeLecture(@RequestBody EverytimeLectureRequestDto requestDto) {
        if (requestDto.getLectureUrl() == null) {
            return ResponseEntity.badRequest().body("크롤링할 강의평가 URL이 필요합니다.");
        }

        try {
            int startPage = requestDto.getStartPage() != null ? requestDto.getStartPage() : 1;
            int endPage = requestDto.getEndPage() != null ? requestDto.getEndPage() : 1;
            everytimeCrawlerService.crawlLectureAndSave(
                    requestDto.getLectureUrl(),
                    startPage,
                    endPage
            );
            return ResponseEntity.ok("강의평가 원시 데이터 크롤링 성공");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("크롤링 실패: " + e.getMessage());
        }
    }

    @PostMapping("/everytime/lecture/batch")
    public ResponseEntity<EverytimeLectureBatchResponseDto> crawlEverytimeLectures(
            @Valid @RequestBody EverytimeLectureBatchRequestDto requestDto
    ) {
        return ResponseEntity.ok(everytimeLectureBatchService.crawl(requestDto));
    }
    
    @GetMapping("/everytime/board")
    public ResponseEntity<java.util.List<java.util.Map<String, Object>>> getAllRawCommunityPosts() {
        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        for (com.uniwiki.entity.RawCommunityPost post : rawCommunityPostRepository.findAll()) {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", post.getId());
            map.put("sourceUrl", post.getSourceUrl());
            map.put("boardType", post.getBoardType());
            map.put("title", post.getTitle());
            map.put("content", post.getContent());
            map.put("likesCount", post.getLikesCount());
            map.put("commentsJson", post.getCommentsJson());
            map.put("isProcessed", post.isProcessed());
            map.put("crawledAt", post.getCrawledAt() != null ? post.getCrawledAt().toString() : null);
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/everytime/lecture")
    public ResponseEntity<java.util.List<java.util.Map<String, Object>>> getAllRawLectureEvaluations() {
        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        for (com.uniwiki.entity.RawLectureEvaluation eval : rawLectureEvaluationRepository.findAll()) {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", eval.getId());
            map.put("sourceUrl", eval.getSourceUrl());
            map.put("courseName", eval.getCourseName());
            map.put("professor", eval.getProfessor());
            map.put("starRating", eval.getStarRating());
            map.put("content", eval.getContent());
            map.put("likesCount", eval.getLikesCount());
            map.put("isProcessed", eval.isProcessed());
            map.put("crawledAt", eval.getCrawledAt() != null ? eval.getCrawledAt().toString() : null);
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }
}
