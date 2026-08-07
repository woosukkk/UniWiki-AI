package com.uniwiki.service;

import com.uniwiki.dto.CommunityWikiDto;
import com.uniwiki.repository.AnswerWikiPromotionRepository;
import com.uniwiki.repository.AnswerRepository;
import com.uniwiki.repository.QuestionWikiPromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Comparator;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityWikiService {

    private final AnswerWikiPromotionRepository promotionRepository;
    private final QuestionWikiPromotionRepository questionPromotionRepository;
    private final AnswerRepository answerRepository;

    public List<CommunityWikiDto.EntryResponse> findAll() {
        Stream<CommunityWikiDto.EntryResponse> answerPromotions =
                promotionRepository.findAllForCommunityWiki().stream()
                        .map(CommunityWikiDto.EntryResponse::from);
        Stream<CommunityWikiDto.EntryResponse> questionPromotions =
                questionPromotionRepository.findAllForCommunityWiki().stream()
                        .map(promotion -> CommunityWikiDto.EntryResponse.from(
                                promotion,
                                answerRepository
                                        .findByQuestion_IdOrderByCreatedAtAsc(
                                                promotion.getQuestion().getId()
                                        )
                                        .stream()
                                        .map(answer -> answer.getContent())
                                        .toList()
                        ));
        return Stream.concat(questionPromotions, answerPromotions)
                .sorted(Comparator.comparing(
                        CommunityWikiDto.EntryResponse::getPromotedAt
                ).reversed())
                .toList();
    }
}
