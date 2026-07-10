package com.uniwiki.controller;

import com.uniwiki.config.LoginUserId;
import com.uniwiki.dto.WikiPostDto;
import com.uniwiki.service.WikiPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Tag(
        name = "위키 문서 API",
        description = "위키 문서 생성, 조회, 수정, 삭제 API"
)
@RestController
@RequestMapping("/api/wiki-posts")
@RequiredArgsConstructor
public class WikiPostController {

    private final WikiPostService wikiPostService;

    // 위키 문서 생성
    @Operation(summary = "위키 문서 생성")
    @PostMapping
    public ResponseEntity<?> createWikiPost(
            @Parameter(hidden = true)
            @LoginUserId Long userId,

            @Valid
            @RequestBody WikiPostDto.CreateRequest request
    ) {
        try {
            WikiPostDto.Response response =
                    wikiPostService.createWikiPost(userId, request);

            URI location = URI.create(
                    "/api/wiki-posts/" + response.getId()
            );

            return ResponseEntity
                    .created(location)
                    .body(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    // 위키 문서 전체 조회
    @Operation(summary = "위키 문서 전체 조회")
    @GetMapping
    public ResponseEntity<List<WikiPostDto.ListResponse>>
    getWikiPosts() {
        return ResponseEntity.ok(
                wikiPostService.getWikiPosts()
        );
    }

    // 내가 작성한 위키 문서 조회
    @Operation(summary = "내가 작성한 위키 문서 조회")
    @GetMapping("/me")
    public ResponseEntity<?> getMyWikiPosts(
            @Parameter(hidden = true)
            @LoginUserId Long userId
    ) {
        try {
            return ResponseEntity.ok(
                    wikiPostService.getMyWikiPosts(userId)
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    // 카테고리별 위키 문서 조회
    @Operation(summary = "카테고리별 위키 문서 조회")
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<?> getWikiPostsByCategory(
            @PathVariable Long categoryId
    ) {
        try {
            return ResponseEntity.ok(
                    wikiPostService.getWikiPostsByCategory(categoryId)
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    // 위키 문서 단건 조회
    @Operation(summary = "위키 문서 단건 조회")
    @GetMapping("/{wikiPostId}")
    public ResponseEntity<?> getWikiPost(
            @PathVariable Long wikiPostId
    ) {
        try {
            return ResponseEntity.ok(
                    wikiPostService.getWikiPost(wikiPostId)
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    // 위키 문서 수정
    @Operation(summary = "위키 문서 수정")
    @PutMapping("/{wikiPostId}")
    public ResponseEntity<?> updateWikiPost(
            @PathVariable Long wikiPostId,

            @Parameter(hidden = true)
            @LoginUserId Long userId,

            @Valid
            @RequestBody WikiPostDto.UpdateRequest request
    ) {
        try {
            return ResponseEntity.ok(
                    wikiPostService.updateWikiPost(
                            wikiPostId,
                            userId,
                            request
                    )
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    // 위키 문서 삭제
    @Operation(summary = "위키 문서 삭제")
    @DeleteMapping("/{wikiPostId}")
    public ResponseEntity<?> deleteWikiPost(
            @PathVariable Long wikiPostId,

            @Parameter(hidden = true)
            @LoginUserId Long userId
    ) {
        try {
            wikiPostService.deleteWikiPost(
                    wikiPostId,
                    userId
            );

            return ResponseEntity
                    .noContent()
                    .build();

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }
    @Operation(summary = "위키 문서 검색")
    @GetMapping("/search")
    public ResponseEntity<List<WikiPostDto.ListResponse>> searchWikiPosts(
        @RequestParam(required = false) String keyword) {
                return ResponseEntity.ok(
                        wikiPostService.searchWikiPosts(keyword)
                );
        }
}