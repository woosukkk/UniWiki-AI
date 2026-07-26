package com.uniwiki.repository;

import com.uniwiki.entity.Like;
import com.uniwiki.entity.LikeTargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {

    boolean existsByUser_IdAndTargetTypeAndTargetId(
            Long userId,
            LikeTargetType targetType,
            Long targetId
    );

    Optional<Like> findByUser_IdAndTargetTypeAndTargetId(
            Long userId,
            LikeTargetType targetType,
            Long targetId
    );

    long countByTargetTypeAndTargetId(LikeTargetType targetType, Long targetId);
}
