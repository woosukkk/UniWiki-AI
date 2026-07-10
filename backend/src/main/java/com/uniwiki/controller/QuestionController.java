package com.uniwiki.controller;

import com.uniwiki.dto.QuestionDto;
import com.uniwiki.config.LoginUserId;
import com.uniwiki.service.QuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionService questionService;

    // 질문 작성
    @PostMapping
    public ResponseEntity<QuestionDto.Response> create(
            @LoginUserId Long loginUserId,
            @Valid @RequestBody QuestionDto.CreateRequest request
    ) {
        QuestionDto.Response response =
                questionService.create(loginUserId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // 질문 전체 조회
    @GetMapping
    public ResponseEntity<List<QuestionDto.Response>> findAll() {
        return ResponseEntity.ok(questionService.findAll());
    }

    // 질문 단건 조회
    @GetMapping("/{questionId}")
    public ResponseEntity<QuestionDto.Response> findById(
            @PathVariable Long questionId
    ) {
        return ResponseEntity.ok(
                questionService.findById(questionId)
        );
    }

    // 질문 수정
    @PutMapping("/{questionId}")
    public ResponseEntity<QuestionDto.Response> update(
            @LoginUserId Long loginUserId,
            @PathVariable Long questionId,
            @Valid @RequestBody QuestionDto.UpdateRequest request
    ) {
        QuestionDto.Response response =
                questionService.update(loginUserId, questionId, request);

        return ResponseEntity.ok(response);
    }

    // 질문 삭제
    @DeleteMapping("/{questionId}")
    public ResponseEntity<Void> delete(
            @LoginUserId Long loginUserId,
            @PathVariable Long questionId
    ) {
        questionService.delete(loginUserId, questionId);

        return ResponseEntity.noContent().build();
    }
}