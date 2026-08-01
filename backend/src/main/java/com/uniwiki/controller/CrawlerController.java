package com.uniwiki.controller;

import com.uniwiki.service.EverytimeCrawlerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.uniwiki.dto.EverytimeBoardRequestDto;
import com.uniwiki.dto.EverytimeLectureRequestDto;

@RestController
@RequestMapping("/api/admin/crawl")
@RequiredArgsConstructor
public class CrawlerController {

    private final EverytimeCrawlerService everytimeCrawlerService;
    private final com.uniwiki.repository.CourseEvaluationRepository courseEvaluationRepository;

    @PostMapping("/everytime/board")
    public ResponseEntity<String> crawlEverytimeBoard(@RequestBody EverytimeBoardRequestDto requestDto) {
        if (requestDto.getBoardUrl() == null) {
            return ResponseEntity.badRequest().body("크롤링할 게시판 URL이 필요합니다.");
        }
        if (!"Question".equalsIgnoreCase(requestDto.getTargetTable()) && !"WikiPost".equalsIgnoreCase(requestDto.getTargetTable())) {
            return ResponseEntity.badRequest().body("targetTable은 'Question' 또는 'WikiPost' 여야 합니다.");
        }

        try {
            int startPage = requestDto.getStartPage() != null ? requestDto.getStartPage() : 1;
            int endPage = requestDto.getEndPage() != null ? requestDto.getEndPage() : 1;
            everytimeCrawlerService.crawlBoardAndSave(
                    requestDto.getBoardUrl(),
                    requestDto.getTargetTable(),
                    requestDto.getCategoryId(),
                    startPage,
                    endPage
            );
            return ResponseEntity.ok("게시판 크롤링 성공");
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
            return ResponseEntity.ok("강의평가 크롤링 성공");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("크롤링 실패: " + e.getMessage());
        }
    }
    @GetMapping("/everytime/lecture")
    public ResponseEntity<java.util.List<java.util.Map<String, Object>>> getAllCourseEvaluations() {
        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        for (com.uniwiki.entity.CourseEvaluation eval : courseEvaluationRepository.findAll()) {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", eval.getId());
            map.put("courseName", eval.getCourseName());
            map.put("professor", eval.getProfessor());
            map.put("starRating", eval.getStarRating());
            map.put("content", eval.getContent());
            map.put("createdAt", eval.getCreatedAt());
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }
}
