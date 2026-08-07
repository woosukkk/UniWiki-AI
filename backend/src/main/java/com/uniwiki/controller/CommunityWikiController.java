package com.uniwiki.controller;

import com.uniwiki.dto.CommunityWikiDto;
import com.uniwiki.service.CommunityWikiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/wiki-posts/community")
@RequiredArgsConstructor
public class CommunityWikiController {

    private final CommunityWikiService communityWikiService;

    @GetMapping
    public ResponseEntity<List<CommunityWikiDto.EntryResponse>> findAll() {
        return ResponseEntity.ok(communityWikiService.findAll());
    }
}
