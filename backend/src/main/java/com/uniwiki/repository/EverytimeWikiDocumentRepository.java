package com.uniwiki.repository;

import com.uniwiki.entity.EverytimeContentType;
import com.uniwiki.entity.EverytimeWikiDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EverytimeWikiDocumentRepository extends JpaRepository<EverytimeWikiDocument, Long> {
    Optional<EverytimeWikiDocument> findBySourceKey(String sourceKey);

    @Query("select d.wikiPost.id from EverytimeWikiDocument d where d.contentType = :contentType")
    List<Long> findWikiPostIdsByContentType(EverytimeContentType contentType);

    @Query("select d.wikiPost.id from EverytimeWikiDocument d")
    List<Long> findAllWikiPostIds();

    @Query("""
            select d.contentType, count(d)
            from EverytimeWikiDocument d
            where d.wikiPost.status = com.uniwiki.entity.WikiPostStatus.APPROVED
            group by d.contentType
            order by d.contentType
            """)
    List<Object[]> summarizeApprovedByContentType();

    @Query("""
            select d.wikiPost.id, d.wikiPost.category.id
            from EverytimeWikiDocument d
            where d.wikiPost.status = com.uniwiki.entity.WikiPostStatus.APPROVED
            """)
    List<Object[]> findApprovedWikiPostCategoryPairs();
}
