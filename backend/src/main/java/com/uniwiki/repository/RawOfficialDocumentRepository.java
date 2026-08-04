package com.uniwiki.repository;

import com.uniwiki.entity.RawOfficialDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface RawOfficialDocumentRepository extends JpaRepository<RawOfficialDocument, Long> {
    Optional<RawOfficialDocument> findByOfficialSource_IdAndSourceUrl(Long sourceId, String sourceUrl);
    List<RawOfficialDocument> findAllByOrderByLastCollectedAtDesc();
}
