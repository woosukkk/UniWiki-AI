package com.uniwiki.service;

import com.uniwiki.dto.AiSummaryDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiSummaryServiceTest {
    @Mock private AiSummaryClient aiSummaryClient;
    @InjectMocks private AiSummaryService aiSummaryService;

    @Test
    void returnsSummaryFromAiService() {
        AiSummaryDto.Response response = new AiSummaryDto.Response(3L, "장학금", "장학금 신청 안내입니다.", 2);
        when(aiSummaryClient.summarize(3L, 500)).thenReturn(response);
        assertThat(aiSummaryService.summarize(3L, 500)).isEqualTo(response);
    }
}
