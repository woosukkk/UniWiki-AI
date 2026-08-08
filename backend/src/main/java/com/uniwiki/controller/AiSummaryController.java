package com.uniwiki.controller;

import com.uniwiki.dto.AiSummaryDto;
import com.uniwiki.service.AiSummaryClient;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/ai/wiki-posts")
@RequiredArgsConstructor
public class AiSummaryController {
    private final AiSummaryClient aiSummaryClient;

    @PostMapping("/{wikiPostId}/summary")
    public ResponseEntity<AiSummaryDto.Response> summarize(
            @PathVariable Long wikiPostId,
            @RequestParam(required = false) @Min(100) @Max(2000) Integer maxChars) {
        return ResponseEntity.ok(aiSummaryClient.summarize(wikiPostId, maxChars));
    }
}
