package com.uniwiki.controller;

import com.uniwiki.config.LoginUserId;
import com.uniwiki.dto.AnswerLikeDto;
import com.uniwiki.service.AnswerLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/answers/{answerId}/likes")
public class AnswerLikeController {

    private final AnswerLikeService answerLikeService;

    @PostMapping
    public ResponseEntity<AnswerLikeDto.Response> add(
            @LoginUserId Long loginUserId,
            @PathVariable Long answerId
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(answerLikeService.add(loginUserId, answerId));
    }

    @DeleteMapping
    public ResponseEntity<Void> remove(
            @LoginUserId Long loginUserId,
            @PathVariable Long answerId
    ) {
        answerLikeService.remove(loginUserId, answerId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count")
    public ResponseEntity<AnswerLikeDto.Response> getCount(@PathVariable Long answerId) {
        return ResponseEntity.ok(answerLikeService.getCount(answerId));
    }
}
