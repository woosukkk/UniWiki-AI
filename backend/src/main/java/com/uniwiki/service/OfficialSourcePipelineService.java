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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;

@Service
@Slf4j
@RequiredArgsConstructor
public class OfficialSourcePipelineService {
    private final AtomicBoolean collectingActiveSources = new AtomicBoolean();

    private static final Pattern EXPLICIT_YEAR =
            Pattern.compile("(?<!\\d)(19\\d{2}|20\\d{2})(?!\\d)");

    private final OfficialSourceRepository sourceRepository;
    private final ObjectProvider<OfficialSourcePipelineService> selfProvider;
    private final RawOfficialDocumentRepository rawRepository;
    private final OfficialWikiDocumentRepository documentRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final WikiPostRepository wikiPostRepository;
    private final WikiVectorSyncService vectorSyncService;
    private final OfficialAttachmentService attachmentService;
    private final OfficialTopicKeyResolver topicKeyResolver;
    private final OfficialRobotsPolicy robotsPolicy;

    @Value("${uniwiki.official-sources.allowed-host-suffixes:sejong.ac.kr}")
    private String allowedHostSuffixes;

    @Value("${uniwiki.official-sources.max-list-pages-per-run:300}")
    private int maxListPagesPerRun;

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

    public OfficialSourceDto.CollectionResult collect(Long sourceId) {
        OfficialSource source = sourceRepository.findById(sourceId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공식 출처입니다."));
        try {
            Set<String> articleUrls = discoverArticleUrls(source);

            int created = 0;
            int changed = 0;
            int unchanged = 0;
            int failed = 0;
            for (String articleUrl : articleUrls) {
                try {
                    ArticleCollectionOutcome outcome = selfProvider.getObject()
                            .collectArticle(sourceId, articleUrl);
                    if (outcome == ArticleCollectionOutcome.CREATED) created++;
                    else if (outcome == ArticleCollectionOutcome.CHANGED) changed++;
                    else unchanged++;
                } catch (Exception exception) {
                    failed++;
                    log.warn("Official source article failed: source={}, url={}, error={}",
                            source.getName(), articleUrl, exception.getMessage());
                }
            }
            source.markSuccess();
            sourceRepository.save(source);
            return new OfficialSourceDto.CollectionResult(articleUrls.size(), created, changed, unchanged, failed);
        } catch (Exception exception) {
            source.markFailure(exception.getMessage());
            sourceRepository.save(source);
            throw new IllegalStateException("공식 출처 수집에 실패했습니다: " + exception.getMessage(), exception);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ArticleCollectionOutcome collectArticle(Long sourceId, String articleUrl) throws Exception {
        OfficialSource source = sourceRepository.findById(sourceId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공식 출처입니다."));
        Document articlePage = fetch(articleUrl);
        String title = requiredText(articlePage, source.getTitleSelector(), "제목");
        title = catalogTitle(source, articleUrl, title);
        if (isBeforeRetentionYear(title)) {
            log.debug("Skipped official article before 2025: source={}, title={}",
                    source.getName(), title);
            return ArticleCollectionOutcome.UNCHANGED;
        }
        List<OfficialAttachmentService.CollectedAttachment> attachments =
                attachmentService.collect(articlePage);
        String content = requiredContent(articlePage, source.getContentSelector())
                + attachmentService.render(attachments);
        String hash = hash(title + "\n" + content);
        RawOfficialDocument raw = rawRepository
                .findByOfficialSource_IdAndSourceUrl(sourceId, articleUrl)
                .orElse(null);
        if (raw == null) {
            raw = rawRepository.save(new RawOfficialDocument(source, articleUrl, title, content, hash));
            attachmentService.synchronize(raw, attachments);
            createOrUpdateWiki(raw);
            return ArticleCollectionOutcome.CREATED;
        }
        if (raw.updateIfChanged(title, content, hash)
                || source.isAutoPublish()
                && raw.getProcessingStatus() != OfficialDocumentStatus.PUBLISHED) {
            attachmentService.synchronize(raw, attachments);
            createOrUpdateWiki(raw);
            return ArticleCollectionOutcome.CHANGED;
        }
        attachmentService.synchronize(raw, attachments);
        return ArticleCollectionOutcome.UNCHANGED;
    }

    public enum ArticleCollectionOutcome {
        CREATED, CHANGED, UNCHANGED
    }

    private boolean isBeforeRetentionYear(String title) {
        Matcher matcher = EXPLICIT_YEAR.matcher(title);
        boolean foundYear = false;
        while (matcher.find()) {
            foundYear = true;
            if (Integer.parseInt(matcher.group(1)) >= 2025) return false;
        }
        return foundYear;
    }

    @Transactional(readOnly = true)
    public List<OfficialSourceDto.DocumentResponse> getDocuments() {
        return rawRepository.findAllByOrderByLastCollectedAtDesc().stream()
                .map(OfficialSourceDto.DocumentResponse::from)
                .toList();
    }

    public void collectActiveSources() {
        if (!collectingActiveSources.compareAndSet(false, true)) {
            log.info("Official source collection skipped because another run is active");
            return;
        }
        try {
            for (OfficialSource source : sourceRepository.findByActiveTrueOrderByIdAsc()) {
                try {
                    OfficialSourceDto.CollectionResult result = selfProvider.getObject().collect(source.getId());
                    log.info("Official source collection: source={}, discovered={}, created={}, changed={}, unchanged={}, failed={}",
                            source.getName(), result.discovered(), result.created(), result.changed(),
                            result.unchanged(), result.failed());
                } catch (Exception exception) {
                    log.warn("Official source collection failed: source={}, error={}",
                            source.getName(), exception.getMessage());
                }
            }
        } finally {
            collectingActiveSources.set(false);
        }
    }

    @Transactional
    public int rebuildTopicWikis() {
        if (!documentRepository.existsByTopicKeyIsNull()
                && !documentRepository.existsByTopicKeyLike("TOSC:%:%")
                && !documentRepository.existsByTopicKeyLike("COURSE_GUIDE:%")
                && !documentRepository.existsBySourceOutsideTopic(
                        "sw.sejong.ac.kr/sw/notice.do", OfficialTopicKeyResolver.SW_CENTER_TOPIC)
                && !documentRepository.existsBySourceOutsideTopic(
                        "udream.sejong.ac.kr", OfficialTopicKeyResolver.UDREAM_TOPIC)) {
            return 0;
        }
        List<RawOfficialDocument> documents = rawRepository.findAllByOrderByLastCollectedAtDesc();
        documents.forEach(this::createOrUpdateWiki);
        return documents.size();
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
        Category documentCategory = resolveDocumentCategory(source, raw.getSourceUrl());
        WikiPostStatus status = source.isAutoPublish() ? WikiPostStatus.APPROVED : WikiPostStatus.DRAFT;
        String topicKey = topicKeyResolver.resolve(source, raw.getTitle(), raw.getSourceUrl());
        OfficialWikiDocument rawLink = documentRepository.findByRawDocument_Id(raw.getId()).orElse(null);
        List<OfficialWikiDocument> topicLinks = documentRepository.findByTopicKeyOrderByIdAsc(topicKey);

        WikiPost previousRawWiki = rawLink == null ? null : rawLink.getWikiPost();
        boolean splitExistingWiki = rawLink != null
                && topicLinks.isEmpty()
                && documentRepository.findByWikiPost_IdOrderByRawDocument_IdAsc(
                        rawLink.getWikiPost().getId()).size() > 1;
        WikiPost wikiPost = topicLinks.isEmpty()
                ? (rawLink == null || splitExistingWiki ? null : rawLink.getWikiPost())
                : topicLinks.get(0).getWikiPost();
        if (wikiPost == null) {
            User author = userRepository.findByEmail(authorEmail)
                    .or(() -> userRepository.findById(authorId))
                    .orElseThrow(() -> new IllegalStateException("공식 위키 작성자를 찾을 수 없습니다."));
            wikiPost = wikiPostRepository.save(new WikiPost(
                    documentCategory, author, truncate(raw.getTitle(), 200), raw.getContent(),
                    source.getName() + "에서 수집한 공식 자료입니다.", status));
        }

        if (rawLink == null) {
            rawLink = documentRepository.save(new OfficialWikiDocument(raw, wikiPost, topicKey));
        } else {
            rawLink.mergeInto(wikiPost, topicKey);
        }

        Set<WikiPost> duplicates = new LinkedHashSet<>();
        if (previousRawWiki != null && !previousRawWiki.getId().equals(wikiPost.getId())) {
            duplicates.add(previousRawWiki);
        }
        for (OfficialWikiDocument link : topicLinks) {
            if (!link.getWikiPost().getId().equals(wikiPost.getId())) {
                duplicates.add(link.getWikiPost());
                link.mergeInto(wikiPost, topicKey);
            }
        }
        documentRepository.flush();
        for (WikiPost duplicate : duplicates) {
            if (documentRepository.findByWikiPost_IdOrderByRawDocument_IdAsc(duplicate.getId()).isEmpty()) {
                vectorSyncService.enqueueDelete(duplicate.getId());
                wikiPostRepository.delete(duplicate);
            }
        }

        List<OfficialWikiDocument> mergedLinks = documentRepository
                .findByWikiPost_IdOrderByRawDocument_IdAsc(wikiPost.getId());
        String title = topicKeyResolver.displayTitle(topicKey, raw.getTitle());
        String content = renderMergedContent(title, mergedLinks);
        String summary = mergedLinks.size() == 1
                ? source.getName() + "에서 수집한 공식 자료입니다."
                : source.getName() + "의 관련 공지 " + mergedLinks.size() + "건을 하나로 통합한 공식 자료입니다.";
        wikiPost.update(documentCategory, truncate(title, 200), content, summary, status);
        mergedLinks.forEach(link -> link.getRawDocument().markProcessed(status == WikiPostStatus.APPROVED));
        if (status == WikiPostStatus.APPROVED) vectorSyncService.enqueueUpsert(wikiPost);
    }

    private String renderMergedContent(String title, List<OfficialWikiDocument> links) {
        StringBuilder content = new StringBuilder("# ").append(title).append("\n");
        for (OfficialWikiDocument link : links) {
            RawOfficialDocument document = link.getRawDocument();
            content.append("\n## ").append(document.getTitle()).append("\n\n")
                    .append(document.getContent()).append("\n\n")
                    .append("- 원문: ").append(document.getSourceUrl()).append("\n")
                    .append("- 원문 해시: `").append(document.getContentHash()).append("`\n");
        }
        content.append("\n## 공식 출처\n\n");
        links.stream().map(OfficialWikiDocument::getRawDocument).forEach(document ->
                content.append("- [").append(document.getTitle()).append("](")
                        .append(document.getSourceUrl()).append(")\n"));
        return content.toString();
    }

    private Document fetch(String url) throws Exception {
        robotsPolicy.awaitAllowed(url);
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
        robotsPolicy.requireAllowed(document.location());
        return document;
    }

    private Set<String> discoverArticleUrls(OfficialSource source) throws Exception {
        Set<String> discovered = new LinkedHashSet<>();

        for (int page = 1; page <= maxListPagesPerRun; page++) {
            String listUrl = listPageUrl(source.getListUrl(), page);
            Document listPage = fetch(listUrl);
            Set<String> pageUrls = new LinkedHashSet<>();
            int discoveredBeforePage = discovered.size();
            boolean hasRecentEntry = false;
            boolean hasUndatedEntry = false;
            for (Element link : listPage.select(source.getArticleLinkSelector())) {
                String url = canonicalArticleUrl(articleUrl(source, link));
                if (url.isBlank() || url.length() > 500) continue;
                requireAllowedUrl(url);
                Integer year = listEntryYear(link);
                if (year == null) hasUndatedEntry = true;
                else if (year >= 2025) hasRecentEntry = true;
                else continue;
                if (!discovered.add(url)) continue;
                pageUrls.add(url);
            }

            if (isAcademicCatalog(source)) {
                break;
            }
            if (pageUrls.isEmpty() && !hasRecentEntry && !hasUndatedEntry) break;
            if (page > 1 && discovered.size() == discoveredBeforePage) break;
        }

        if (source.getListUrl().contains("/kor/intro/notice3.do")) {
            discovered.addAll(courseMaterialNoticeUrls());
        }
        return discovered;
    }

    Integer listEntryYear(Element link) {
        Element entry = link.closest("tr, li, article, .b-row-box, .board-list-item");
        Element context = entry == null ? link.parent() : entry;
        String text = context == null ? link.text() : context.text();
        Matcher matcher = EXPLICIT_YEAR.matcher(text);
        while (matcher.find()) {
            int year = Integer.parseInt(matcher.group(1));
            if (year >= 2000 && year <= 2100) return year;
        }
        return null;
    }

    private Set<String> courseMaterialNoticeUrls() {
        return new LinkedHashSet<>(List.of(
                "https://www.sejong.ac.kr/kor/intro/notice3.do?articleNo=805695&mode=view",
                "https://www.sejong.ac.kr/kor/intro/notice3.do?articleNo=805814&mode=view",
                "https://www.sejong.ac.kr/kor/intro/notice3.do?articleNo=805910&mode=view",
                "https://www.sejong.ac.kr/kor/intro/notice3.do?articleNo=853683&mode=view",
                "https://www.sejong.ac.kr/kor/intro/notice3.do?articleNo=863048&mode=view",
                "https://www.sejong.ac.kr/kor/intro/notice3.do?articleNo=891086&mode=view"
        ));
    }

    private boolean isAcademicCatalog(OfficialSource source) {
        return source.getListUrl().contains("/kor/academics/academic-calendar.do")
                && source.getArticleLinkSelector().contains("/kor/academics/");
    }

    private Category resolveDocumentCategory(OfficialSource source, String sourceUrl) {
        if (!isAcademicCatalog(source)) return source.getCategory();
        String categoryName;
        if (sourceUrl.matches(".*(scholarship|student-loan).*")) {
            categoryName = "장학·지원";
        } else if (sourceUrl.matches(".*(curriculum|credit-system|class-|register-for-class|micro-degree).*")) {
            categoryName = "교과목";
        } else if (sourceUrl.matches(".*(graduation|grades|percentage-conversion|early-graduation).*")) {
            categoryName = "졸업요건";
        } else if (sourceUrl.matches(".*(certificate|cert-info).*")) {
            categoryName = "인증제도";
        } else {
            categoryName = "학사";
        }
        return categoryRepository.findByName(categoryName).orElse(source.getCategory());
    }

    String listPageUrl(String listUrl, int page) {
        if (listUrl.contains("udream.sejong.ac.kr")) {
            if (page <= 1) return listUrl;
            return listUrl + (listUrl.contains("?") ? "&" : "?") + "rp=" + page;
        }
        if (listUrl.contains("tosc.sejong.ac.kr")) {
            if (page <= 1) return listUrl;
            return listUrl + (listUrl.contains("?") ? "&" : "?") + "p=" + page;
        }
        int offset = (Math.max(1, page) - 1) * 100;
        return listUrl + (listUrl.contains("?") ? "&" : "?")
                + "mode=list&articleLimit=100&article.offset=" + offset;
    }

    String articleUrl(OfficialSource source, Element link) {
        String href = link.absUrl("href");
        if (!href.isBlank()) {
            if (isAcademicCatalog(source)
                    && href.endsWith("/kor/academics/freshman-scholarship.do")) return "";
            return href;
        }
        if (!source.getListUrl().contains("udream.sejong.ac.kr")) return "";

        Matcher matcher = Pattern.compile("goView\\(['\"]([A-Fa-f0-9]+)['\"]\\)")
                .matcher(link.attr("onclick"));
        if (!matcher.find()) return "";
        return "https://udream.sejong.ac.kr/community/Program/programView.aspx?pgdx="
                + matcher.group(1);
    }

    String catalogTitle(OfficialSource source, String sourceUrl, String title) {
        String courseMaterialTerm = courseMaterialTerm(sourceUrl);
        if (courseMaterialTerm != null) {
            return courseMaterialTerm + " 수강편람 및 강의시간표";
        }
        if (!isAcademicCatalog(source)) return title;
        Matcher freshman = Pattern.compile("freshman-scholarship_(20\\d{2})\\.do")
                .matcher(sourceUrl);
        if (freshman.find()) return freshman.group(1) + "학년도 신입생장학금";
        Matcher curriculum = Pattern.compile("curriculum(20\\d{2})\\.do")
                .matcher(sourceUrl);
        if (curriculum.find()) return curriculum.group(1) + "학년도 교과과정";
        return title;
    }

    private String courseMaterialTerm(String sourceUrl) {
        if (sourceUrl.contains("articleNo=805695")) return "2024-1";
        if (sourceUrl.contains("articleNo=805814")) return "2024-2";
        if (sourceUrl.contains("articleNo=805910")) return "2025-1";
        if (sourceUrl.contains("articleNo=853683")) return "2025-2";
        if (sourceUrl.contains("articleNo=863048")) return "2026-1";
        if (sourceUrl.contains("articleNo=891086")) return "2026-2";
        return null;
    }

    String canonicalArticleUrl(String value) {
        if (value == null || value.isBlank()) return "";
        URI uri = URI.create(value.replace("&amp;", "&"));
        String query = uri.getRawQuery();
        if (query == null || query.isBlank()) return uri.toString();
        String canonicalQuery = java.util.Arrays.stream(query.split("&"))
                .filter(parameter -> !parameter.startsWith("article.offset="))
                .filter(parameter -> !parameter.startsWith("articleLimit="))
                .filter(parameter -> !parameter.startsWith("p="))
                .sorted()
                .collect(Collectors.joining("&"));
        try {
            return new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(),
                    canonicalQuery.isBlank() ? null : canonicalQuery, null).toString();
        } catch (Exception exception) {
            return value;
        }
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

    String requiredText(Document document, String selector, String label) {
        Element element = document.selectFirst(selector);
        String value = element == null ? "" : element.text();
        if (value.isBlank() && element != null) value = element.attr("content");
        if (value.isBlank()) {
            throw new IllegalArgumentException(label + " 선택자에 해당하는 내용이 없습니다: " + selector);
        }
        return value.replaceAll("\\s+", " ").trim();
    }

    String requiredContent(Document document, String selector) {
        String text = document.select(selector).stream()
                .map(element -> element.text().isBlank()
                        ? element.attr("content")
                        : extractStructuredText(element))
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining("\n\n"))
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

    String extractStructuredText(Element element) {
        Element copy = element.clone();
        copy.select("script, style, noscript").remove();
        copy.select("li").forEach(node -> {
            node.prependText("- ");
            node.appendText("\n");
        });
        copy.select("p, div, section, article, header, footer, h1, h2, h3, h4, h5, h6, tr")
                .forEach(node -> node.appendText("\n\n"));
        copy.select("th, td").forEach(node -> node.appendText("\t"));

        return copy.wholeText()
                .replace('\u00a0', ' ')
                .replaceAll("(?m)[ \\t]+$", "")
                .replaceAll("(?m)^[ \\t]+", "")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
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
