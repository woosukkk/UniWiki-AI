package com.uniwiki.repository;

import com.uniwiki.entity.LectureReviewWikiDraft;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.Query;

public interface LectureReviewWikiDraftRepository extends JpaRepository<LectureReviewWikiDraft, Long> {
    Optional<LectureReviewWikiDraft> findByCourseNameAndProfessor(String courseName, String professor);

    @Query("select d.wikiPost.id from LectureReviewWikiDraft d")
    List<Long> findAllWikiPostIds();
}
