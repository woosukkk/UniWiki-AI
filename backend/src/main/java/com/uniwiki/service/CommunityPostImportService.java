package com.uniwiki.service;

import com.uniwiki.dto.CommunityPostImportItemDto;
import com.uniwiki.dto.CommunityPostImportResponseDto;
import com.uniwiki.entity.RawCommunityPost;
import com.uniwiki.repository.RawCommunityPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommunityPostImportService {
    private final RawCommunityPostRepository repository;

    @Transactional
    public CommunityPostImportResponseDto importPosts(List<CommunityPostImportItemDto> posts) {
        int saved = 0;
        int duplicates = 0;
        for (CommunityPostImportItemDto post : posts) {
            RawCommunityPost existing = repository.findBySourceUrl(post.sourceUrl()).orElse(null);
            if (existing != null) {
                existing.refresh(
                        post.boardType(), post.title(), post.content(),
                        post.likesCount(), post.commentsCount(),
                        post.commentsJson() == null ? "[]" : post.commentsJson());
                duplicates++;
                continue;
            }
            repository.save(new RawCommunityPost(
                    post.sourceUrl(), post.boardType(), post.title(), post.content(),
                    post.likesCount(), post.commentsCount(),
                    post.commentsJson() == null ? "[]" : post.commentsJson()));
            saved++;
        }
        return new CommunityPostImportResponseDto(posts.size(), saved, duplicates);
    }
}
