package com.uniwiki.repository;

import com.uniwiki.entity.OfficialWikiDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OfficialWikiDocumentRepository extends JpaRepository<OfficialWikiDocument, Long> {
    Optional<OfficialWikiDocument> findByRawDocument_Id(Long rawDocumentId);
}
