package com.uniwiki.repository;

import com.uniwiki.entity.OfficialAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OfficialAttachmentRepository extends JpaRepository<OfficialAttachment, Long> {
    List<OfficialAttachment> findByRawDocument_Id(Long rawDocumentId);
    Optional<OfficialAttachment> findByRawDocument_IdAndSourceUrl(Long rawDocumentId, String sourceUrl);
}
