package com.uniwiki.service;

import com.uniwiki.dto.CommunityPostImportItemDto;
import com.uniwiki.dto.CommunityPostImportRequestDto;
import com.uniwiki.dto.CommunityPostImportResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommunityPostTransferService {
    private final CommunityPostImportService importService;
    private final RestClient.Builder restClientBuilder;

    @Value("${uniwiki.everytime.community-upload-url:}")
    private String uploadUrl;

    @Value("${uniwiki.everytime.upload-token:}")
    private String uploadToken;

    public CommunityPostImportResponseDto transfer(List<CommunityPostImportItemDto> posts) {
        if (posts.isEmpty()) return new CommunityPostImportResponseDto(0, 0, 0);
        if (uploadUrl.isBlank()) return importService.importPosts(posts);
        if (uploadToken.isBlank()) throw new IllegalStateException("EVERYTIME_UPLOAD_TOKEN is required");
        CommunityPostImportResponseDto response = restClientBuilder.build().post()
                .uri(uploadUrl)
                .header("X-Crawler-Token", uploadToken)
                .body(new CommunityPostImportRequestDto(posts))
                .retrieve()
                .body(CommunityPostImportResponseDto.class);
        if (response == null) throw new IllegalStateException("Community post upload returned an empty response");
        return response;
    }
}
