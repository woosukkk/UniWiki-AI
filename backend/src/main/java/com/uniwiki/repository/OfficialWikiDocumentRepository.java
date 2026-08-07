package com.uniwiki.repository;

import com.uniwiki.entity.OfficialWikiDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.Query;

public interface OfficialWikiDocumentRepository extends JpaRepository<OfficialWikiDocument, Long> {
    boolean existsByTopicKeyIsNull();
    boolean existsByTopicKeyLike(String pattern);

    Optional<OfficialWikiDocument> findByRawDocument_Id(Long rawDocumentId);

    List<OfficialWikiDocument> findByTopicKeyOrderByIdAsc(String topicKey);

    List<OfficialWikiDocument> findByWikiPost_IdOrderByRawDocument_IdAsc(Long wikiPostId);

    @Query("""
            select d.wikiPost.id, d.wikiPost.category.id
            from OfficialWikiDocument d
            where d.wikiPost.status = com.uniwiki.entity.WikiPostStatus.APPROVED
            """)
    List<Object[]> findApprovedWikiPostCategoryPairs();
}
