package com.uniwiki.controller;

import com.uniwiki.config.LoginUserId;
import com.uniwiki.dto.OfficialSourceDto;
import com.uniwiki.entity.User;
import com.uniwiki.repository.UserRepository;
import com.uniwiki.service.OfficialSourcePipelineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/admin/official-sources")
@RequiredArgsConstructor
public class OfficialSourceController {
    private final OfficialSourcePipelineService pipelineService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<OfficialSourceDto.Response> register(
            @LoginUserId Long userId,
            @Valid @RequestBody OfficialSourceDto.CreateRequest request) {
        requireAdmin(userId);
        return ResponseEntity.ok(pipelineService.register(request));
    }

    @GetMapping
    public ResponseEntity<List<OfficialSourceDto.Response>> getSources(@LoginUserId Long userId) {
        requireAdmin(userId);
        return ResponseEntity.ok(pipelineService.getSources());
    }

    @PostMapping("/{sourceId}/collect")
    public ResponseEntity<OfficialSourceDto.CollectionResult> collect(
            @LoginUserId Long userId, @PathVariable Long sourceId) {
        requireAdmin(userId);
        return ResponseEntity.ok(pipelineService.collect(sourceId));
    }

    @PostMapping("/documents/{rawDocumentId}/approve")
    public ResponseEntity<Long> approve(
            @LoginUserId Long userId, @PathVariable Long rawDocumentId) {
        requireAdmin(userId);
        return ResponseEntity.ok(pipelineService.approveDocument(rawDocumentId));
    }

    @GetMapping("/documents")
    public ResponseEntity<List<OfficialSourceDto.DocumentResponse>> getDocuments(
            @LoginUserId Long userId) {
        requireAdmin(userId);
        return ResponseEntity.ok(pipelineService.getDocuments());
    }

    private void requireAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "인증 사용자를 찾을 수 없습니다."));
        if (!"ADMIN".equals(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자 권한이 필요합니다.");
        }
    }
}
