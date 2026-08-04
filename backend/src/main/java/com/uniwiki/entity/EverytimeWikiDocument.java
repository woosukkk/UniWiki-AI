package com.uniwiki.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "everytime_wiki_documents", uniqueConstraints = {
        @UniqueConstraint(name = "uq_everytime_wiki_source", columnNames = "source_key")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EverytimeWikiDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_key", nullable = false, length = 500)
    private String sourceKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 30)
    private EverytimeContentType contentType;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wiki_post_id", nullable = false, unique = true)
    private WikiPost wikiPost;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public EverytimeWikiDocument(String sourceKey, EverytimeContentType contentType, WikiPost wikiPost) {
        this.sourceKey = sourceKey;
        this.contentType = contentType;
        this.wikiPost = wikiPost;
    }

    @PrePersist
    @PreUpdate
    private void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
}
