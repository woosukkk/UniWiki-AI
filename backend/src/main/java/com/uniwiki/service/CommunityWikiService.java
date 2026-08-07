package com.uniwiki.service;

import com.uniwiki.dto.CommunityWikiDto;
import com.uniwiki.repository.AnswerWikiPromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityWikiService {

    private final AnswerWikiPromotionRepository promotionRepository;

    public List<CommunityWikiDto.EntryResponse> findAll() {
        return promotionRepository.findAllForCommunityWiki().stream()
                .map(CommunityWikiDto.EntryResponse::from)
                .toList();
    }
}
