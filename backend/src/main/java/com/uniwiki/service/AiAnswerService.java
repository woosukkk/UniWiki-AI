package com.uniwiki.service;

import com.uniwiki.dto.AiAnswerDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiAnswerService {

    private final AiAnswerClient aiAnswerClient;

    public AiAnswerDto.Response answer(AiAnswerDto.Request request) {
        return aiAnswerClient.answer(request);
    }
}
