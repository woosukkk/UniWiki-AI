package com.uniwiki.controller;

import com.uniwiki.config.LoginUserId;
import com.uniwiki.dto.AnswerDto;
import com.uniwiki.service.AnswerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AnswerController {

    private final AnswerService answerService;

    @PostMapping("/api/answers/questions/{questionId}")
    public ResponseEntity<AnswerDto.Response> create(
            @LoginUserId Long loginUserId,
            @PathVariable Long questionId,
            @Valid @RequestBody AnswerDto.CreateRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(answerService.create(loginUserId, questionId, request));
    }

    @GetMapping("/api/questions/{questionId}/answers")
    public ResponseEntity<List<AnswerDto.Response>> findByQuestionId(
            @PathVariable Long questionId
    ) {
        return ResponseEntity.ok(answerService.findByQuestionId(questionId));
    }

    @PutMapping("/api/answers/{answerId}")
    public ResponseEntity<AnswerDto.Response> update(
            @LoginUserId Long loginUserId,
            @PathVariable Long answerId,
            @Valid @RequestBody AnswerDto.UpdateRequest request
    ) {
        return ResponseEntity.ok(answerService.update(loginUserId, answerId, request));
    }

    @DeleteMapping("/api/answers/{answerId}")
    public ResponseEntity<Void> delete(
            @LoginUserId Long loginUserId,
            @PathVariable Long answerId
    ) {
        answerService.delete(loginUserId, answerId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/answers/{answerId}/accept")
    public ResponseEntity<AnswerDto.Response> accept(
            @LoginUserId Long loginUserId,
            @PathVariable Long answerId
    ) {
        return ResponseEntity.ok(answerService.accept(loginUserId, answerId));
    }
}
