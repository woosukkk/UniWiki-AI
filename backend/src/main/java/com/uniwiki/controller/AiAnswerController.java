package com.uniwiki.controller;

import com.uniwiki.dto.AiAnswerDto;
import com.uniwiki.service.AiAnswerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/answers")
@RequiredArgsConstructor
public class AiAnswerController {

    private final AiAnswerService aiAnswerService;

    @PostMapping
    public ResponseEntity<AiAnswerDto.Response> answer(@Valid @RequestBody AiAnswerDto.Request request) {
        return ResponseEntity.ok(aiAnswerService.answer(request));
    }
}
