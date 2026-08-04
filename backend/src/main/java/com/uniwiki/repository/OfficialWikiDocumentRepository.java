package com.uniwiki.repository;

import com.uniwiki.entity.OfficialWikiDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.Query;

public interface OfficialWikiDocumentRepository extends JpaRepository<OfficialWikiDocument, Long> {
    Optional<OfficialWikiDocument> findByRawDocument_Id(Long rawDocumentId);

    @Query("""
            select d.wikiPost.id, d.wikiPost.category.id
            from OfficialWikiDocument d
            where d.wikiPost.status = com.uniwiki.entity.WikiPostStatus.APPROVED
            """)
    List<Object[]> findApprovedWikiPostCategoryPairs();
}
