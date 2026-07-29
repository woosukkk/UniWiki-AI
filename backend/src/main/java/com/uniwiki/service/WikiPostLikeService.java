package com.uniwiki.service;

import com.uniwiki.dto.WikiPostLikeDto;
import com.uniwiki.entity.Like;
import com.uniwiki.entity.LikeTargetType;
import com.uniwiki.entity.User;
import com.uniwiki.repository.LikeRepository;
import com.uniwiki.repository.UserRepository;
import com.uniwiki.repository.WikiPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WikiPostLikeService {

    private static final LikeTargetType TARGET_TYPE = LikeTargetType.WIKI_POST;

    private final LikeRepository likeRepository;
    private final WikiPostRepository wikiPostRepository;
    private final UserRepository userRepository;

    @Transactional
    public WikiPostLikeDto.Response add(Long loginUserId, Long wikiPostId) {
        validateWikiPostExists(wikiPostId);
        User user = findUser(loginUserId);

        if (isLiked(loginUserId, wikiPostId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 좋아요를 누른 위키 문서입니다."
            );
        }

        likeRepository.save(new Like(user, TARGET_TYPE, wikiPostId));
        return response(wikiPostId, true);
    }

    @Transactional
    public void remove(Long loginUserId, Long wikiPostId) {
        validateWikiPostExists(wikiPostId);
        Like like = likeRepository.findByUser_IdAndTargetTypeAndTargetId(
                        loginUserId,
                        TARGET_TYPE,
                        wikiPostId
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "취소할 좋아요를 찾을 수 없습니다."
                ));
        likeRepository.delete(like);
    }

    public WikiPostLikeDto.Response getCount(Long wikiPostId) {
        validateWikiPostExists(wikiPostId);
        return response(wikiPostId, false);
    }

    public WikiPostLikeDto.Response getStatus(Long loginUserId, Long wikiPostId) {
        validateWikiPostExists(wikiPostId);
        findUser(loginUserId);
        return response(wikiPostId, isLiked(loginUserId, wikiPostId));
    }

    private boolean isLiked(Long userId, Long wikiPostId) {
        return likeRepository.existsByUser_IdAndTargetTypeAndTargetId(
                userId,
                TARGET_TYPE,
                wikiPostId
        );
    }

    private WikiPostLikeDto.Response response(Long wikiPostId, boolean liked) {
        long count = likeRepository.countByTargetTypeAndTargetId(
                TARGET_TYPE,
                wikiPostId
        );
        return new WikiPostLikeDto.Response(wikiPostId, count, liked);
    }

    private void validateWikiPostExists(Long wikiPostId) {
        if (!wikiPostRepository.existsById(wikiPostId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "위키 문서를 찾을 수 없습니다."
            );
        }
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "사용자를 찾을 수 없습니다."
                ));
    }
}
