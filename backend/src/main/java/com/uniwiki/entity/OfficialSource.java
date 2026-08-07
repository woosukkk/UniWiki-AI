package com.uniwiki.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "official_sources")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OfficialSource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    @Column(name = "list_url", nullable = false, length = 1000)
    private String listUrl;

    @Column(name = "article_link_selector", nullable = false, length = 500)
    private String articleLinkSelector;

    @Column(name = "title_selector", nullable = false, length = 500)
    private String titleSelector;

    @Column(name = "content_selector", nullable = false, length = 500)
    private String contentSelector;

    @Column(name = "auto_publish", nullable = false)
    private boolean autoPublish;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "last_checked_at")
    private LocalDateTime lastCheckedAt;

    @Column(name = "last_success_at")
    private LocalDateTime lastSuccessAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public OfficialSource(Category category, String name, String listUrl,
                          String articleLinkSelector, String titleSelector,
                          String contentSelector, boolean autoPublish) {
        this.category = category;
        this.name = name;
        this.listUrl = listUrl;
        this.articleLinkSelector = articleLinkSelector;
        this.titleSelector = titleSelector;
        this.contentSelector = contentSelector;
        this.autoPublish = autoPublish;
        this.active = true;
    }

    public void markSuccess() {
        LocalDateTime now = LocalDateTime.now();
        this.lastCheckedAt = now;
        this.lastSuccessAt = now;
        this.lastError = null;
    }

    public void markFailure(String error) {
        this.lastCheckedAt = LocalDateTime.now();
        this.lastError = error == null ? "Unknown error" : error.substring(0, Math.min(1000, error.length()));
    }

    public void enableAutoPublish() {
        this.autoPublish = true;
    }

    public void updateConfiguration(Category category, String listUrl,
                                    String articleLinkSelector, String titleSelector,
                                    String contentSelector, boolean autoPublish) {
        this.category = category;
        this.listUrl = listUrl;
        this.articleLinkSelector = articleLinkSelector;
        this.titleSelector = titleSelector;
        this.contentSelector = contentSelector;
        this.autoPublish = autoPublish;
        this.active = true;
    }

    @PrePersist
    private void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    private void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
