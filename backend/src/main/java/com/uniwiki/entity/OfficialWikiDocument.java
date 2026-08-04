package com.uniwiki.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "official_wiki_documents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OfficialWikiDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "raw_document_id", nullable = false, unique = true)
    private RawOfficialDocument rawDocument;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wiki_post_id", nullable = false, unique = true)
    private WikiPost wikiPost;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public OfficialWikiDocument(RawOfficialDocument rawDocument, WikiPost wikiPost) {
        this.rawDocument = rawDocument;
        this.wikiPost = wikiPost;
    }

    @PrePersist
    @PreUpdate
    private void updateTimestamp() {
        updatedAt = LocalDateTime.now();
    }
}
