package com.uniwiki.controller;

import com.uniwiki.config.LoginUserId;
import com.uniwiki.dto.LikeDto;
import com.uniwiki.service.QuestionLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/questions/{questionId}/likes")
public class QuestionLikeController {

    private final QuestionLikeService questionLikeService;

    @PostMapping
    public ResponseEntity<LikeDto.Response> add(
            @LoginUserId Long loginUserId,
            @PathVariable Long questionId
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(questionLikeService.add(loginUserId, questionId));
    }

    @DeleteMapping
    public ResponseEntity<Void> remove(
            @LoginUserId Long loginUserId,
            @PathVariable Long questionId
    ) {
        questionLikeService.remove(loginUserId, questionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count")
    public ResponseEntity<LikeDto.Response> getCount(@PathVariable Long questionId) {
        return ResponseEntity.ok(questionLikeService.getCount(questionId));
    }
}
