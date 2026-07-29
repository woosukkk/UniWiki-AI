package com.uniwiki.service;

import com.uniwiki.dto.AiAnswerDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiAnswerServiceTest {

    @Mock
    private AiAnswerClient aiAnswerClient;

    @InjectMocks
    private AiAnswerService aiAnswerService;

    @Test
    void returnsGroundedAnswerFromAiService() {
        AiAnswerDto.Request request = new AiAnswerDto.Request("수강신청은 어디서 하나요?", null);
        AiAnswerDto.Response response = new AiAnswerDto.Response(
                request.question(),
                "포털에서 신청할 수 있습니다.",
                true,
                2,
                List.of(new AiAnswerDto.Source(1L, "수강신청 안내", "/wiki/1"))
        );
        when(aiAnswerClient.answer(request)).thenReturn(response);

        assertThat(aiAnswerService.answer(request)).isEqualTo(response);
    }
}
