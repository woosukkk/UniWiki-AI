package com.uniwiki.repository;

import com.uniwiki.entity.LectureReviewWikiDraft;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LectureReviewWikiDraftRepository extends JpaRepository<LectureReviewWikiDraft, Long> {
    Optional<LectureReviewWikiDraft> findByCourseNameAndProfessor(String courseName, String professor);
}
