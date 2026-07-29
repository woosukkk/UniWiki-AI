package com.uniwiki.service;

import com.uniwiki.dto.AnswerLikeDto;
import com.uniwiki.entity.Like;
import com.uniwiki.entity.LikeTargetType;
import com.uniwiki.entity.User;
import com.uniwiki.repository.AnswerRepository;
import com.uniwiki.repository.LikeRepository;
import com.uniwiki.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnswerLikeService {

    private static final LikeTargetType TARGET_TYPE = LikeTargetType.ANSWER;

    private final LikeRepository likeRepository;
    private final AnswerRepository answerRepository;
    private final UserRepository userRepository;

    @Transactional
    public AnswerLikeDto.Response add(Long loginUserId, Long answerId) {
        validateAnswerExists(answerId);
        User user = findUser(loginUserId);

        if (likeRepository.existsByUser_IdAndTargetTypeAndTargetId(
                loginUserId,
                TARGET_TYPE,
                answerId
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 좋아요를 누른 답변입니다."
            );
        }

        likeRepository.save(new Like(user, TARGET_TYPE, answerId));
        return response(answerId, true);
    }

    @Transactional
    public void remove(Long loginUserId, Long answerId) {
        validateAnswerExists(answerId);

        Like like = likeRepository.findByUser_IdAndTargetTypeAndTargetId(
                        loginUserId,
                        TARGET_TYPE,
                        answerId
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "취소할 좋아요를 찾을 수 없습니다."
                ));

        likeRepository.delete(like);
    }

    public AnswerLikeDto.Response getCount(Long answerId) {
        validateAnswerExists(answerId);
        return response(answerId, false);
    }

    public AnswerLikeDto.Response getStatus(Long loginUserId, Long answerId) {
        validateAnswerExists(answerId);
        findUser(loginUserId);
        boolean liked = likeRepository.existsByUser_IdAndTargetTypeAndTargetId(
                loginUserId,
                TARGET_TYPE,
                answerId
        );
        return response(answerId, liked);
    }

    private AnswerLikeDto.Response response(Long answerId, boolean liked) {
        long likeCount = likeRepository.countByTargetTypeAndTargetId(TARGET_TYPE, answerId);
        return new AnswerLikeDto.Response(answerId, likeCount, liked);
    }

    private void validateAnswerExists(Long answerId) {
        if (!answerRepository.existsById(answerId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "답변을 찾을 수 없습니다."
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
