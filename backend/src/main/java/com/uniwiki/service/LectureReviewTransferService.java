package com.uniwiki.service;

import com.uniwiki.dto.LectureReviewImportItemDto;
import com.uniwiki.dto.LectureReviewImportRequestDto;
import com.uniwiki.dto.LectureReviewImportResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LectureReviewTransferService {

    private final LectureReviewImportService importService;
    private final RestClient.Builder restClientBuilder;

    @Value("${uniwiki.everytime.upload-url:}")
    private String uploadUrl;

    @Value("${uniwiki.everytime.upload-token:}")
    private String uploadToken;

    public LectureReviewImportResponseDto transfer(List<LectureReviewImportItemDto> reviews) {
        if (reviews.isEmpty()) {
            return new LectureReviewImportResponseDto(0, 0, 0);
        }
        if (uploadUrl.isBlank()) {
            return importService.importReviews(reviews);
        }
        if (uploadToken.isBlank()) {
            throw new IllegalStateException("EVERYTIME_UPLOAD_TOKEN is required when EVERYTIME_UPLOAD_URL is set");
        }
        LectureReviewImportResponseDto response = restClientBuilder.build()
                .post()
                .uri(uploadUrl)
                .header("X-Crawler-Token", uploadToken)
                .body(new LectureReviewImportRequestDto(reviews))
                .retrieve()
                .body(LectureReviewImportResponseDto.class);
        if (response == null) {
            throw new IllegalStateException("Lecture review upload returned an empty response");
        }
        return response;
    }
}
