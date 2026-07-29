package com.uniwiki.service;

import com.uniwiki.dto.AiSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiSummaryService {
    private final AiSummaryClient aiSummaryClient;

    public AiSummaryDto.Response summarize(Long wikiPostId, Integer maxChars) {
        return aiSummaryClient.summarize(wikiPostId, maxChars);
    }
}
