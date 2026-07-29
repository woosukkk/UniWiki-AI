package com.uniwiki.service;

import com.uniwiki.dto.LikeDto;
import com.uniwiki.entity.Like;
import com.uniwiki.entity.LikeTargetType;
import com.uniwiki.entity.User;
import com.uniwiki.repository.LikeRepository;
import com.uniwiki.repository.QuestionRepository;
import com.uniwiki.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuestionLikeService {

    private static final LikeTargetType TARGET_TYPE = LikeTargetType.QUESTION;

    private final LikeRepository likeRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;

    @Transactional
    public LikeDto.Response add(Long loginUserId, Long questionId) {
        validateQuestionExists(questionId);
        User user = findUser(loginUserId);

        if (likeRepository.existsByUser_IdAndTargetTypeAndTargetId(
                loginUserId,
                TARGET_TYPE,
                questionId
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 좋아요를 누른 질문입니다."
            );
        }

        likeRepository.save(new Like(user, TARGET_TYPE, questionId));
        return response(questionId, true);
    }

    @Transactional
    public void remove(Long loginUserId, Long questionId) {
        validateQuestionExists(questionId);

        Like like = likeRepository.findByUser_IdAndTargetTypeAndTargetId(
                        loginUserId,
                        TARGET_TYPE,
                        questionId
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "취소할 좋아요를 찾을 수 없습니다."
                ));

        likeRepository.delete(like);
    }

    public LikeDto.Response getCount(Long questionId) {
        validateQuestionExists(questionId);
        return response(questionId, false);
    }

    public LikeDto.Response getStatus(Long loginUserId, Long questionId) {
        validateQuestionExists(questionId);
        findUser(loginUserId);
        boolean liked = likeRepository.existsByUser_IdAndTargetTypeAndTargetId(
                loginUserId,
                TARGET_TYPE,
                questionId
        );
        return response(questionId, liked);
    }

    private LikeDto.Response response(Long questionId, boolean liked) {
        long likeCount = likeRepository.countByTargetTypeAndTargetId(TARGET_TYPE, questionId);
        return new LikeDto.Response(questionId, likeCount, liked);
    }

    private void validateQuestionExists(Long questionId) {
        if (!questionRepository.existsById(questionId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "질문을 찾을 수 없습니다."
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
