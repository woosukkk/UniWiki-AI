package com.uniwiki.repository;

import com.uniwiki.entity.OfficialAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;

public interface OfficialAttachmentRepository extends JpaRepository<OfficialAttachment, Long> {
    List<OfficialAttachment> findByRawDocument_Id(Long rawDocumentId);
    Optional<OfficialAttachment> findByRawDocument_IdAndSourceUrl(Long rawDocumentId, String sourceUrl);

    @Query("""
            select a.rawDocument.officialSource.category.id, a.extractionStatus, count(a)
            from OfficialAttachment a
            where a.rawDocument.processingStatus = com.uniwiki.entity.OfficialDocumentStatus.PUBLISHED
            group by a.rawDocument.officialSource.category.id, a.extractionStatus
            """)
    List<Object[]> summarizePublishedByCategoryAndStatus();
}
