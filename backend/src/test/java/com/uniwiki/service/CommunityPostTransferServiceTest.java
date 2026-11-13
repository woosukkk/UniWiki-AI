package com.uniwiki.service;

import com.uniwiki.dto.CommunityPostImportItemDto;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CommunityPostTransferServiceTest {
    @Test
    void masksPersonalInformationBeforeSendingToRemoteServer() {
        RestClient.Builder client = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(client).build();
        CommunityPostTransferService service = new CommunityPostTransferService(
                mock(CommunityPostImportService.class), client, new LectureReviewSanitizer());
        ReflectionTestUtils.setField(service, "uploadUrl", "https://upload.example/import");
        ReflectionTestUtils.setField(service, "uploadToken", "test-token");
        server.expect(requestTo("https://upload.example/import"))
                .andExpect(method(POST))
                .andExpect(header("X-Crawler-Token", "test-token"))
                .andExpect(content().string(not(containsString("test@example.com"))))
                .andExpect(content().string(not(containsString("010-1234-5678"))))
                .andExpect(content().string(not(containsString("20260001"))))
                .andExpect(jsonPath("$.posts[0].title").value(containsString("[이메일 삭제]")))
                .andExpect(jsonPath("$.posts[0].content").value(containsString("[학번 삭제]")))
                .andExpect(jsonPath("$.posts[0].commentsJson").value(containsString("[연락처 삭제]")))
                .andRespond(withSuccess("{\"received\":1,\"saved\":1,\"duplicates\":0}", APPLICATION_JSON));

        var result = service.transfer(List.of(new CommunityPostImportItemDto(
                "https://everytime.kr/hotarticle/v/1", "핫 게시판", "문의 test@example.com",
                "학번 20260001", 3, 1, "[\"연락처 010-1234-5678\"]")));

        assertThat(result.saved()).isEqualTo(1);
        server.verify();
    }
}
