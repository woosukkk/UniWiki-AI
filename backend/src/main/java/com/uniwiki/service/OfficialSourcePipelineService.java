package com.uniwiki.service;

import com.uniwiki.dto.OfficialSourceDto;
import com.uniwiki.entity.*;
import com.uniwiki.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;

@Service
@Slf4j
@RequiredArgsConstructor
public class OfficialSourcePipelineService {

    private final OfficialSourceRepository sourceRepository;
    private final RawOfficialDocumentRepository rawRepository;
    private final OfficialWikiDocumentRepository documentRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final WikiPostRepository wikiPostRepository;
    private final WikiVectorSyncService vectorSyncService;
    private final OfficialAttachmentService attachmentService;

    @Value("${uniwiki.official-sources.allowed-host-suffixes:sejong.ac.kr}")
    private String allowedHostSuffixes;

    @Value("${uniwiki.official-sources.max-articles-per-run:20}")
    private int maxArticlesPerRun;

    @Value("${uniwiki.official-sources.author-id:1}")
    private Long authorId;

    @Value("${uniwiki.official-sources.author-email:official-source@local.invalid}")
    private String authorEmail;

    @Transactional
    public OfficialSourceDto.Response register(OfficialSourceDto.CreateRequest request) {
        requireAllowedUrl(request.listUrl());
        if (sourceRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("이미 등록된 공식 출처 이름입니다.");
        }
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));
        OfficialSource source = sourceRepository.save(new OfficialSource(
                category, request.name(), request.listUrl(), request.articleLinkSelector(),
                request.titleSelector(), request.contentSelector(), request.autoPublish()));
        return OfficialSourceDto.Response.from(source);
    }

    @Transactional(readOnly = true)
    public List<OfficialSourceDto.Response> getSources() {
        return sourceRepository.findAllByOrderByNameAsc().stream()
                .map(OfficialSourceDto.Response::from)
                .toList();
    }

    @Transactional(noRollbackFor = IllegalStateException.class)
    public OfficialSourceDto.CollectionResult collect(Long sourceId) {
        OfficialSource source = sourceRepository.findById(sourceId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공식 출처입니다."));
        try {
            Document listPage = fetch(source.getListUrl());
            Set<String> articleUrls = new LinkedHashSet<>();
            for (Element link : listPage.select(source.getArticleLinkSelector())) {
                String url = link.absUrl("href");
                if (url.isBlank()) continue;
                requireAllowedUrl(url);
                if (url.length() > 500) continue;
                articleUrls.add(url);
                if (articleUrls.size() >= maxArticlesPerRun) break;
            }

            int created = 0;
            int changed = 0;
            int unchanged = 0;
            int failed = 0;
            for (String articleUrl : articleUrls) {
                try {
                    Document articlePage = fetch(articleUrl);
                    String title = requiredText(articlePage, source.getTitleSelector(), "제목");
                    List<OfficialAttachmentService.CollectedAttachment> attachments =
                            attachmentService.collect(articlePage);
                    String content = requiredContent(articlePage, source.getContentSelector())
                            + attachmentService.render(attachments);
                    String hash = hash(title + "\n" + content);
                    RawOfficialDocument raw = rawRepository
                            .findByOfficialSource_IdAndSourceUrl(source.getId(), articleUrl)
                            .orElse(null);
                    if (raw == null) {
                        raw = rawRepository.save(new RawOfficialDocument(source, articleUrl, title, content, hash));
                        attachmentService.synchronize(raw, attachments);
                        createOrUpdateWiki(raw);
                        created++;
                    } else if (raw.updateIfChanged(title, content, hash)) {
                        attachmentService.synchronize(raw, attachments);
                        createOrUpdateWiki(raw);
                        changed++;
                    } else if (source.isAutoPublish()
                            && raw.getProcessingStatus() != OfficialDocumentStatus.PUBLISHED) {
                        attachmentService.synchronize(raw, attachments);
                        createOrUpdateWiki(raw);
                        changed++;
                    } else {
                        attachmentService.synchronize(raw, attachments);
                        unchanged++;
                    }
                } catch (Exception exception) {
                    failed++;
                    log.warn("Official source article failed: source={}, url={}, error={}",
                            source.getName(), articleUrl, exception.getMessage());
                }
            }
            source.markSuccess();
            return new OfficialSourceDto.CollectionResult(articleUrls.size(), created, changed, unchanged, failed);
        } catch (Exception exception) {
            source.markFailure(exception.getMessage());
            throw new IllegalStateException("공식 출처 수집에 실패했습니다: " + exception.getMessage(), exception);
        }
    }

    @Transactional(readOnly = true)
    public List<OfficialSourceDto.DocumentResponse> getDocuments() {
        return rawRepository.findAllByOrderByLastCollectedAtDesc().stream()
                .map(OfficialSourceDto.DocumentResponse::from)
                .toList();
    }

    @Transactional
    public void collectActiveSources() {
        for (OfficialSource source : sourceRepository.findByActiveTrueOrderByIdAsc()) {
            try {
                OfficialSourceDto.CollectionResult result = collect(source.getId());
                log.info("Official source collection: source={}, discovered={}, created={}, changed={}, unchanged={}, failed={}",
                        source.getName(), result.discovered(), result.created(), result.changed(),
                        result.unchanged(), result.failed());
            } catch (Exception exception) {
                log.warn("Official source collection failed: source={}, error={}",
                        source.getName(), exception.getMessage());
            }
        }
    }

    @Transactional
    public Long approveDocument(Long rawDocumentId) {
        RawOfficialDocument raw = rawRepository.findById(rawDocumentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공식 원시 자료입니다."));
        OfficialWikiDocument link = documentRepository.findByRawDocument_Id(rawDocumentId)
                .orElseThrow(() -> new IllegalArgumentException("연결된 위키 초안이 없습니다."));
        WikiPost wikiPost = link.getWikiPost();
        wikiPost.publish();
        raw.markProcessed(true);
        vectorSyncService.enqueueUpsert(wikiPost);
        return wikiPost.getId();
    }

    private void createOrUpdateWiki(RawOfficialDocument raw) {
        OfficialSource source = raw.getOfficialSource();
        WikiPostStatus status = source.isAutoPublish() ? WikiPostStatus.APPROVED : WikiPostStatus.DRAFT;
        String content = "# " + raw.getTitle() + "\n\n" + raw.getContent() + "\n\n"
                + "## 공식 출처\n\n"
                + "- 출처: " + source.getName() + "\n"
                + "- 원문: " + raw.getSourceUrl() + "\n"
                + "- 원문 해시: `" + raw.getContentHash() + "`\n";
        String summary = source.getName() + "에서 수집한 공식 자료입니다.";
        OfficialWikiDocument existing = documentRepository.findByRawDocument_Id(raw.getId()).orElse(null);
        WikiPost wikiPost;
        if (existing == null) {
            User author = userRepository.findByEmail(authorEmail)
                    .or(() -> userRepository.findById(authorId))
                    .orElseThrow(() -> new IllegalStateException("공식 위키 작성자를 찾을 수 없습니다."));
            wikiPost = wikiPostRepository.save(new WikiPost(
                    source.getCategory(), author, truncate(raw.getTitle(), 200), content, summary, status));
            documentRepository.save(new OfficialWikiDocument(raw, wikiPost));
        } else {
            wikiPost = existing.getWikiPost();
            wikiPost.update(source.getCategory(), truncate(raw.getTitle(), 200), content, summary, status);
        }
        raw.markProcessed(status == WikiPostStatus.APPROVED);
        if (status == WikiPostStatus.APPROVED) vectorSyncService.enqueueUpsert(wikiPost);
    }

    private Document fetch(String url) throws Exception {
        Document document;
        try {
            document = connection(url).get();
        } catch (SSLHandshakeException exception) {
            try {
                SSLContext tls12 = SSLContext.getInstance("TLSv1.2");
                tls12.init(null, null, null);
                document = connection(url).sslSocketFactory(tls12.getSocketFactory()).get();
            } catch (SSLHandshakeException retryException) {
                document = fetchWithCurl(url);
            }
        } catch (IOException exception) {
            document = fetchWithCurl(url);
        }
        requireAllowedUrl(document.location());
        return document;
    }

    private org.jsoup.Connection connection(String url) {
        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (compatible; UniWiki-AI official source monitor/1.0)")
                .timeout((int) Duration.ofSeconds(15).toMillis());
    }

    private Document fetchWithCurl(String url) throws Exception {
        Process process = new ProcessBuilder(
                "curl", "--fail", "--silent", "--show-error", "--location",
                "--max-time", "15", "--user-agent",
                "Mozilla/5.0 (compatible; UniWiki-AI official source monitor/1.0)", url)
                .redirectErrorStream(true)
                .start();
        byte[] response = process.getInputStream().readAllBytes();
        if (!process.waitFor(20, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException("공식 출처 curl 요청 시간이 초과됐습니다.");
        }
        if (process.exitValue() != 0) {
            throw new IllegalStateException("공식 출처 curl 요청에 실패했습니다. 종료 코드: " + process.exitValue());
        }
        return Jsoup.parse(new String(response, StandardCharsets.UTF_8), url);
    }

    private String requiredText(Document document, String selector, String label) {
        Element element = document.selectFirst(selector);
        if (element == null || element.text().isBlank()) {
            throw new IllegalArgumentException(label + " 선택자에 해당하는 내용이 없습니다: " + selector);
        }
        return element.text().replaceAll("\\s+", " ").trim();
    }

    private String requiredContent(Document document, String selector) {
        String text = document.select(selector).stream()
                .map(Element::text)
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining(" "))
                .replaceAll("\\s+", " ")
                .trim();
        if (!text.isBlank()) return text;

        String images = document.select(selector + " img").stream()
                .map(image -> image.absUrl("src"))
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining("\n- "));
        String attachments = document.select("a[href*='mode=download']").stream()
                .map(Element::text)
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining("\n- "));
        StringBuilder fallback = new StringBuilder("이미지 또는 첨부파일 중심의 공지입니다.");
        if (!images.isBlank()) fallback.append("\n\n이미지:\n- ").append(images);
        if (!attachments.isBlank()) fallback.append("\n\n첨부파일:\n- ").append(attachments);
        if (images.isBlank() && attachments.isBlank()) {
            throw new IllegalArgumentException("본문 선택자에 해당하는 내용이 없습니다: " + selector);
        }
        return fallback.toString();
    }

    private void requireAllowedUrl(String value) {
        URI uri;
        try {
            uri = URI.create(value);
        } catch (Exception exception) {
            throw new IllegalArgumentException("올바른 공식 출처 URL이 아닙니다.");
        }
        String host = uri.getHost();
        boolean allowed = "https".equalsIgnoreCase(uri.getScheme()) && host != null
                && List.of(allowedHostSuffixes.split(",")).stream()
                .map(String::trim)
                .filter(suffix -> !suffix.isBlank())
                .anyMatch(suffix -> host.equals(suffix) || host.endsWith("." + suffix));
        if (!allowed) throw new IllegalArgumentException("허용되지 않은 공식 출처 URL입니다: " + value);
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("원문 해시 생성에 실패했습니다.", exception);
        }
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
