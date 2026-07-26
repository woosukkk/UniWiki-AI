package com.uniwiki.controller;

import com.uniwiki.config.LoginUserId;
import com.uniwiki.dto.WikiPostLikeDto;
import com.uniwiki.service.WikiPostLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/wiki-posts/{wikiPostId}/likes")
public class WikiPostLikeController {

    private final WikiPostLikeService wikiPostLikeService;

    @PostMapping
    public ResponseEntity<WikiPostLikeDto.Response> add(
            @LoginUserId Long loginUserId,
            @PathVariable Long wikiPostId
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(wikiPostLikeService.add(loginUserId, wikiPostId));
    }

    @DeleteMapping
    public ResponseEntity<Void> remove(
            @LoginUserId Long loginUserId,
            @PathVariable Long wikiPostId
    ) {
        wikiPostLikeService.remove(loginUserId, wikiPostId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<WikiPostLikeDto.Response> getStatus(
            @LoginUserId Long loginUserId,
            @PathVariable Long wikiPostId
    ) {
        return ResponseEntity.ok(
                wikiPostLikeService.getStatus(loginUserId, wikiPostId)
        );
    }

    @GetMapping("/count")
    public ResponseEntity<WikiPostLikeDto.Response> getCount(
            @PathVariable Long wikiPostId
    ) {
        return ResponseEntity.ok(wikiPostLikeService.getCount(wikiPostId));
    }
}
