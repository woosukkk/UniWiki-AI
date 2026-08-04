package com.uniwiki.service;

import com.uniwiki.entity.AttachmentExtractionStatus;
import com.uniwiki.entity.OfficialAttachment;
import com.uniwiki.entity.RawOfficialDocument;
import com.uniwiki.repository.OfficialAttachmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfficialAttachmentService {

    private final OfficialAttachmentRepository attachmentRepository;

    @Value("${uniwiki.official-sources.allowed-host-suffixes:sejong.ac.kr}")
    private String allowedHostSuffixes;

    @Value("${uniwiki.official-sources.attachments.max-count:10}")
    private int maxCount;

    @Value("${uniwiki.official-sources.attachments.max-bytes:20971520}")
    private int maxBytes;

    @Value("${uniwiki.official-sources.attachments.max-text-length:50000}")
    private int maxTextLength;

    public List<CollectedAttachment> collect(Document articlePage) {
        List<CollectedAttachment> collected = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (Element link : articlePage.select("a[href*='mode=download'], a[href$='.pdf'], a[href$='.xlsx'], "
                + "a[href$='.xls'], a[href$='.docx'], a[href$='.txt'], a[href$='.csv']")) {
            if (collected.size() >= maxCount) break;
            String url = link.absUrl("href");
            if (url.isBlank() || !seen.add(url)) continue;
            String fileName = link.text().replaceAll("\\s+", " ").trim();
            if (fileName.isBlank()) fileName = fileNameFromUrl(url);
            collected.add(collectOne(url, fileName));
        }
        return collected;
    }

    public void synchronize(RawOfficialDocument raw, List<CollectedAttachment> collected) {
        Map<String, OfficialAttachment> existing = attachmentRepository.findByRawDocument_Id(raw.getId()).stream()
                .collect(Collectors.toMap(OfficialAttachment::getSourceUrl, attachment -> attachment));
        Set<String> currentUrls = collected.stream().map(CollectedAttachment::sourceUrl).collect(Collectors.toSet());
        existing.values().stream()
                .filter(attachment -> !currentUrls.contains(attachment.getSourceUrl()))
                .forEach(attachmentRepository::delete);

        for (CollectedAttachment item : collected) {
            OfficialAttachment attachment = existing.get(item.sourceUrl());
            if (attachment == null) {
                attachmentRepository.save(new OfficialAttachment(
                        raw, item.sourceUrl(), item.fileName(), item.contentType(), item.fileSize(),
                        item.contentHash(), item.status(), item.extractedText(), item.error()));
            } else {
                attachment.update(item.sourceUrl(), item.fileName(), item.contentType(), item.fileSize(),
                        item.contentHash(), item.status(), item.extractedText(), item.error());
            }
        }
    }

    public String render(List<CollectedAttachment> attachments) {
        if (attachments.isEmpty()) return "";
        StringBuilder text = new StringBuilder("\n\n## 첨부파일 내용\n");
        for (CollectedAttachment attachment : attachments) {
            text.append("\n### ").append(attachment.fileName()).append("\n")
                    .append("- 파일 URL: ").append(attachment.sourceUrl()).append("\n")
                    .append("- 파일 해시: `").append(attachment.contentHash()).append("`\n")
                    .append("- 추출 상태: ").append(attachment.status()).append("\n");
            if (attachment.extractedText() != null && !attachment.extractedText().isBlank()) {
                text.append("\n").append(attachment.extractedText()).append("\n");
            } else if (attachment.error() != null && !attachment.error().isBlank()) {
                text.append("- 처리 참고: ").append(attachment.error()).append("\n");
            }
        }
        return text.toString();
    }

    private CollectedAttachment collectOne(String url, String fileName) {
        try {
            requireAllowedUrl(url);
            DownloadedFile file = download(url);
            if (file.bytes().length > maxBytes) {
                return result(url, fileName, file.contentType(), file.bytes(),
                        AttachmentExtractionStatus.TOO_LARGE, null, "파일 크기 제한을 초과했습니다.");
            }
            Extraction extraction = extract(fileName, file.contentType(), file.bytes());
            return result(url, fileName, file.contentType(), file.bytes(), extraction.status(),
                    truncate(extraction.text()), extraction.error());
        } catch (Exception exception) {
            log.warn("Official attachment collection failed: url={}, error={}", url, exception.getMessage());
            return new CollectedAttachment(url, fileName, null, 0, hash(url),
                    AttachmentExtractionStatus.FAILED, null, exception.getMessage());
        }
    }

    private DownloadedFile download(String url) throws Exception {
        try {
            Connection.Response response = connection(url).execute();
            requireAllowedUrl(response.url().toString());
            return new DownloadedFile(response.bodyAsBytes(), response.contentType());
        } catch (SSLHandshakeException exception) {
            try {
                SSLContext tls12 = SSLContext.getInstance("TLSv1.2");
                tls12.init(null, null, null);
                Connection.Response response = connection(url)
                        .sslSocketFactory(tls12.getSocketFactory()).execute();
                requireAllowedUrl(response.url().toString());
                return new DownloadedFile(response.bodyAsBytes(), response.contentType());
            } catch (SSLHandshakeException retryException) {
                return downloadWithCurl(url);
            }
        }
    }

    private Connection connection(String url) {
        return Jsoup.connect(url)
                .ignoreContentType(true)
                .maxBodySize(maxBytes + 1)
                .userAgent("Mozilla/5.0 (compatible; UniWiki-AI official source monitor/1.0)")
                .timeout((int) Duration.ofSeconds(20).toMillis());
    }

    private DownloadedFile downloadWithCurl(String url) throws Exception {
        Process process = new ProcessBuilder(
                "curl", "--fail", "--silent", "--show-error", "--max-redirs", "0",
                "--max-time", "20", "--user-agent",
                "Mozilla/5.0 (compatible; UniWiki-AI official source monitor/1.0)", url)
                .redirectErrorStream(true)
                .start();
        byte[] response = process.getInputStream().readNBytes(maxBytes + 1);
        if (!process.waitFor(25, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException("첨부파일 요청 시간이 초과됐습니다.");
        }
        if (process.exitValue() != 0) {
            throw new IllegalStateException("첨부파일 요청에 실패했습니다. 종료 코드: " + process.exitValue());
        }
        return new DownloadedFile(response, null);
    }

    private Extraction extract(String fileName, String contentType, byte[] bytes) {
        String extension = extension(fileName);
        try {
            return switch (extension) {
                case "pdf" -> extractPdf(bytes);
                case "xlsx", "xls" -> extractWorkbook(bytes);
                case "docx" -> extractDocx(bytes);
                case "txt", "csv" -> extracted(new String(bytes, StandardCharsets.UTF_8));
                default -> new Extraction(AttachmentExtractionStatus.UNSUPPORTED, null,
                        "지원하지 않는 첨부파일 형식입니다: " + extension);
            };
        } catch (Exception exception) {
            return new Extraction(AttachmentExtractionStatus.FAILED, null,
                    "텍스트 추출 실패: " + exception.getMessage());
        }
    }

    private Extraction extractPdf(byte[] bytes) throws Exception {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            return extracted(new PDFTextStripper().getText(document));
        }
    }

    private Extraction extractWorkbook(byte[] bytes) throws Exception {
        StringBuilder text = new StringBuilder();
        DataFormatter formatter = new DataFormatter();
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            for (Sheet sheet : workbook) {
                text.append("[시트: ").append(sheet.getSheetName()).append("]\n");
                for (Row row : sheet) {
                    List<String> cells = new ArrayList<>();
                    for (Cell cell : row) cells.add(formatter.formatCellValue(cell));
                    text.append(String.join(" | ", cells)).append("\n");
                    if (text.length() >= maxTextLength) break;
                }
                if (text.length() >= maxTextLength) break;
            }
        }
        return extracted(text.toString());
    }

    private Extraction extractDocx(byte[] bytes) throws Exception {
        StringBuilder text = new StringBuilder();
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            document.getParagraphs().forEach(paragraph -> text.append(paragraph.getText()).append("\n"));
            for (XWPFTable table : document.getTables()) {
                table.getRows().forEach(row -> text.append(row.getTableCells().stream()
                        .map(cell -> cell.getText().replaceAll("\\s+", " "))
                        .collect(Collectors.joining(" | "))).append("\n"));
                if (text.length() >= maxTextLength) break;
            }
        }
        return extracted(text.toString());
    }

    private Extraction extracted(String value) {
        String text = value == null ? "" : value.replace('\u0000', ' ').replaceAll("[ \\t]+", " ").trim();
        if (text.isBlank()) return new Extraction(AttachmentExtractionStatus.EMPTY, null, "추출된 텍스트가 없습니다.");
        return new Extraction(AttachmentExtractionStatus.EXTRACTED, text, null);
    }

    private CollectedAttachment result(String url, String fileName, String contentType, byte[] bytes,
                                       AttachmentExtractionStatus status, String text, String error) {
        return new CollectedAttachment(url, fileName, contentType, bytes.length, hash(bytes), status, text, error);
    }

    private void requireAllowedUrl(String value) {
        URI uri = URI.create(value);
        String host = uri.getHost();
        boolean allowed = "https".equalsIgnoreCase(uri.getScheme()) && host != null
                && Arrays.stream(allowedHostSuffixes.split(","))
                .map(String::trim)
                .filter(suffix -> !suffix.isBlank())
                .anyMatch(suffix -> host.equals(suffix) || host.endsWith("." + suffix));
        if (!allowed) throw new IllegalArgumentException("허용되지 않은 첨부파일 URL입니다: " + value);
    }

    private String extension(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index < 0 ? "" : fileName.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private String fileNameFromUrl(String url) {
        String path = URI.create(url).getPath();
        int index = path.lastIndexOf('/');
        return index < 0 ? path : path.substring(index + 1);
    }

    private String truncate(String value) {
        if (value == null || value.length() <= maxTextLength) return value;
        return value.substring(0, maxTextLength) + "\n[추출 내용이 길어 일부만 저장됨]";
    }

    private String hash(String value) {
        return hash(value.getBytes(StandardCharsets.UTF_8));
    }

    private String hash(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (Exception exception) {
            throw new IllegalStateException("첨부파일 해시 생성에 실패했습니다.", exception);
        }
    }

    public record CollectedAttachment(String sourceUrl, String fileName, String contentType,
                                      long fileSize, String contentHash,
                                      AttachmentExtractionStatus status, String extractedText,
                                      String error) { }

    private record DownloadedFile(byte[] bytes, String contentType) { }
    private record Extraction(AttachmentExtractionStatus status, String text, String error) { }
}
