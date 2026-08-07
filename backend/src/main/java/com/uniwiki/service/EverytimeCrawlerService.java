package com.uniwiki.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniwiki.entity.*;
import com.uniwiki.repository.CategoryRepository;
import com.uniwiki.repository.CourseEvaluationRepository;
import com.uniwiki.repository.QuestionRepository;
import com.uniwiki.repository.UserRepository;
import com.uniwiki.repository.WikiPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

@Slf4j
@Service
@RequiredArgsConstructor
public class EverytimeCrawlerService {

    private final CommunityPostTransferService communityPostTransferService;
    private final com.uniwiki.repository.RawLectureEvaluationRepository rawLectureEvaluationRepository;
    private final ObjectMapper objectMapper;
    
    private static final Logger logger = LoggerFactory.getLogger(EverytimeCrawlerService.class);

    private static final String LOGIN_URL = "https://account.everytime.kr/login";

    public void crawlBoardAndSave(String boardUrl, String boardType, int startPage, int endPage, 
                                  java.util.List<String> titleKeywords, java.util.List<String> contentKeywords) {
        logger.info("에브리타임 일반 게시판 크롤링 시작: {}", boardUrl);
        
        ChromeOptions options = new ChromeOptions();
        
        // 팀원마다 각자의 PC에 개별 크롬 프로필(자동로그인 저장소)을 생성하도록 경로 지정
        String profilePath = System.getProperty("java.io.tmpdir") + "/EverytimeChromeProfile";
        options.addArguments("user-data-dir=" + profilePath);
        
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64)");

        WebDriver driver = new ChromeDriver(options);
        
        try {
            driver.get(LOGIN_URL);
            logger.info("에브리타임 로그인 페이지로 이동했습니다. 수동 로그인이 필요할 수 있습니다.");

            WebDriverWait loginWait = new WebDriverWait(driver, Duration.ofSeconds(60));
            loginWait.until(webDriver -> webDriver.getCurrentUrl().startsWith("https://everytime.kr/")
                    && !webDriver.getCurrentUrl().contains("/login"));

            int totalCount = 0;
            java.util.Set<String> seenUrls = new java.util.HashSet<>();
            
            for (int page = startPage; page <= endPage; page++) {
                String currentUrl = boardUrl;
                if (page > 1) {
                    if (boardUrl.contains("?")) {
                        int qIdx = boardUrl.indexOf("?");
                        currentUrl = boardUrl.substring(0, qIdx) + "/p/" + page + boardUrl.substring(qIdx);
                    } else {
                        currentUrl = boardUrl + "/p/" + page;
                    }
                }
                driver.get(currentUrl);
                logger.info("{} 페이지 크롤링 시작: {}", page, currentUrl);
                
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.wrap.articles, article > a.article, #container")));
                
                Document doc = Jsoup.parse(driver.getPageSource(), currentUrl);
                Elements articles = doc.select("article > a.article");
                if (articles.isEmpty()) {
                    articles = doc.select("article");
                }
                java.util.List<com.uniwiki.dto.CommunityPostImportItemDto> pagePosts = new java.util.ArrayList<>();
                
                if (articles.isEmpty()) {
                    logger.warn("게시물을 찾을 수 없습니다. 현재 페이지 DOM 일부: {}", 
                            driver.getPageSource().substring(0, Math.min(500, driver.getPageSource().length())));
                }
                
                for (Element article : articles) {
                    String sourceUrl = article.absUrl("href");
                    if (sourceUrl.isBlank()) {
                        Element articleLink = article.selectFirst("a.article, a[href]");
                        sourceUrl = articleLink == null ? currentUrl : articleLink.absUrl("href");
                    }
                    if (sourceUrl.isBlank()) sourceUrl = currentUrl;
                    sourceUrl = sourceUrl.split("\\?")[0];
                    if (!seenUrls.add(sourceUrl)) continue;

                    Element contentRoot = article;
                    Document detailDocument = null;
                    if (!sourceUrl.equals(currentUrl)) {
                        driver.get(sourceUrl);
                        new WebDriverWait(driver, Duration.ofSeconds(10))
                                .until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
                        Thread.sleep(750);
                        detailDocument = Jsoup.parse(driver.getPageSource(), sourceUrl);
                        String currentDetailUrl = driver.getCurrentUrl().split("\\?")[0];
                        Element detailArticle = currentDetailUrl.equals(sourceUrl)
                                ? findDetailArticle(detailDocument, sourceUrl) : null;
                        for (int retry = 0; detailArticle == null && retry < 3; retry++) {
                            Thread.sleep(500);
                            detailDocument = Jsoup.parse(driver.getPageSource(), sourceUrl);
                            currentDetailUrl = driver.getCurrentUrl().split("\\?")[0];
                            if (currentDetailUrl.equals(sourceUrl)) {
                                detailArticle = findDetailArticle(detailDocument, sourceUrl);
                            }
                        }
                        if (detailArticle != null) {
                            contentRoot = detailArticle;
                        } else {
                            logger.warn("에브리타임 상세 글 불일치로 건너뜀: requested={}, current={}",
                                    sourceUrl, driver.getCurrentUrl());
                        }
                    }

                    CommunityPostSnapshot snapshot = extractCommunityPost(contentRoot, detailDocument);
                    String title = snapshot.title();
                    String content = snapshot.content();
                    
                    if (title.isEmpty() && content.isEmpty()) {
                        content = contentRoot.text();
                    }

                    if (title.isEmpty() && content.isEmpty()) continue;
                    
                    if (content.isEmpty()) {
                        content = title;
                    }
                    if (isGenericArticleTitle(title)) {
                        title = content.length() > 45 ? content.substring(0, 45) + "..." : content;
                    }

                    int likesCount = snapshot.likesCount();
                    int commentsCount = snapshot.comments().size();
                    
                    pagePosts.add(new com.uniwiki.dto.CommunityPostImportItemDto(
                            sourceUrl,
                            boardType != null ? boardType : "알 수 없는 게시판", 
                            title, 
                            content, 
                            likesCount,
                            commentsCount,
                            serializeComments(snapshot.comments())
                    ));
                }

                totalCount += communityPostTransferService.transfer(pagePosts).saved();
                
                if (page < endPage) {
                    Thread.sleep(2000);
                }
            }
            logger.info("에브리타임 일반 게시판 크롤링 완료. 총 {}개의 원시 게시물을 저장했습니다.", totalCount);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("크롤링 중 인터럽트 발생", e);
        } catch (Exception e) {
            logger.error("크롤링 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("크롤링 실패: " + e.getMessage(), e);
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    private Element findDetailArticle(Document document, String sourceUrl) {
        Element exactMatch = null;
        for (Element candidate : document.select("article > a.article[href], a.article[href]")) {
            String candidateUrl = candidate.absUrl("href").split("\\?")[0];
            if (candidateUrl.equals(sourceUrl)
                    && (exactMatch == null || candidate.text().length() > exactMatch.text().length())) {
                exactMatch = candidate;
            }
        }
        return exactMatch != null ? exactMatch : document.selectFirst("article.item");
    }

    private boolean isGenericArticleTitle(String title) {
        if (title == null || title.isBlank()) return true;
        String normalized = title.replace(" ", "");
        return normalized.equals("익명")
                || normalized.equals("자유게시판")
                || normalized.equals("비밀게시판")
                || normalized.equals("핫게시판");
    }

    public void crawlLectureAndSave(String lectureUrl, int startPage, int endPage) {
        ChromeOptions options = new ChromeOptions();
        
        String profilePath = System.getProperty("java.io.tmpdir") + "/EverytimeChromeProfile";
        options.addArguments("user-data-dir=" + profilePath);
        
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64)");

        WebDriver driver = new ChromeDriver(options);
        
        try {
            driver.get(LOGIN_URL);
            logger.info("에브리타임 로그인 페이지로 이동했습니다. 수동 로그인이 필요할 수 있습니다.");

            WebDriverWait loginWait = new WebDriverWait(driver, Duration.ofSeconds(60));
            loginWait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("a.my, a.message")));

            int totalCount = 0;
            
            String baseUrl = lectureUrl.contains("?") ? lectureUrl.split("\\?")[0] : lectureUrl;
            
            // 1. 강의 정보 수집 (Base URL)
            driver.get(baseUrl);
            logger.info("강의 정보 크롤링 시작: {}", baseUrl);
            
            WebDriverWait infoWait = new WebDriverWait(driver, Duration.ofSeconds(10));
            infoWait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("section.info, header h1, div.info")));
            
            Document infoDoc = Jsoup.parse(driver.getPageSource());
            String courseName = "강의명 미상";
            String professor = "교수 미상";
            
            // 1. 강의명 추출
            Element h1 = infoDoc.selectFirst("header div.navbar h1");
            if (h1 != null) {
                courseName = h1.text().replace(" 강의실", "").trim();
            } else {
                Element headH2 = infoDoc.selectFirst("div.head h2");
                if (headH2 != null) courseName = headH2.text();
            }
            
            // section.info에서 과목명 재확인
            Elements items = infoDoc.select("section.info div.item");
            for (Element item : items) {
                Element label = item.selectFirst("label");
                if (label != null && label.text().contains("과목명")) {
                    Element a = item.selectFirst("a.link");
                    if (a != null && !a.text().isEmpty()) {
                        courseName = a.text().trim();
                    } else {
                        courseName = item.text().replace("과목명", "").trim();
                    }
                }
                if (label != null && label.text().contains("교수명")) {
                    Element multiline = item.selectFirst("div.multiline");
                    if (multiline != null && !multiline.text().isEmpty()) {
                        professor = multiline.text().trim();
                    } else {
                        professor = item.text().replace("교수명", "").trim();
                    }
                }
            }
            
            // 예전 버전 폴백 (만약 SPA 버전이 아닐 경우)
            if (professor.equals("교수 미상")) {
                Element infoDiv = infoDoc.selectFirst("div.head");
                if (infoDiv == null) infoDiv = infoDoc.selectFirst("div.info");
                if (infoDiv != null) {
                    Elements spans = infoDiv.select("span");
                    for (Element span : spans) {
                        if (span.text().contains("교수")) {
                            professor = span.text().replace("교수", "").trim();
                            break;
                        }
                    }
                    if (professor.equals("교수 미상")) {
                        Element p = infoDiv.selectFirst("p");
                        if (p != null) {
                            String[] parts = p.text().split(" ");
                            if (parts.length > 0) professor = parts[0];
                        }
                    }
                }
            }
            
            // 2. 강의평가 리뷰 수집 (Review URL)
            String reviewUrl = baseUrl + "?tab=article";
            driver.get(reviewUrl);
            logger.info("강의평가 리뷰 크롤링 시작: {}", reviewUrl);
            
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.articles > article, div.articles > div.article, article")));
            
            Document doc = Jsoup.parse(driver.getPageSource());
            Elements articles = doc.select("div.articles > article, div.articles > div.article");
            if (articles.isEmpty()) {
                articles = doc.select("article, div.article");
            }
            
            if (articles.isEmpty()) {
                logger.warn("강의평가를 찾을 수 없습니다. 현재 페이지 DOM 일부: {}", 
                        driver.getPageSource().substring(0, Math.min(500, driver.getPageSource().length())));
            }
            
            for (Element article : articles) {
                String content = article.select("p.text").text();
                if (content.isEmpty()) {
                    content = article.select("div.text").text();
                }
                if (content.isEmpty()) {
                    content = article.text(); 
                }
                if (content.isEmpty()) continue;
                
                int starRating = 0;
                Element starEl = article.selectFirst("span.star > span.on");
                if (starEl != null) {
                    String widthStyle = starEl.attr("style");
                    if (widthStyle.contains("100%")) starRating = 5;
                    else if (widthStyle.contains("80%")) starRating = 4;
                    else if (widthStyle.contains("60%")) starRating = 3;
                    else if (widthStyle.contains("40%")) starRating = 2;
                    else if (widthStyle.contains("20%")) starRating = 1;
                }
                
                int likesCount = 0;
                Element voteEl = article.selectFirst("li.vote");
                if (voteEl == null) {
                    voteEl = article.selectFirst("span.vote");
                }
                if (voteEl != null) {
                    try {
                        likesCount = Integer.parseInt(voteEl.text());
                    } catch (NumberFormatException ignored) {}
                }
                
                RawLectureEvaluation eval = new RawLectureEvaluation(
                        lectureUrl,
                        courseName, 
                        professor, 
                        starRating, 
                        likesCount, 
                        content
                );
                rawLectureEvaluationRepository.save(eval);
                totalCount++;
            }
                
            logger.info("에브리타임 강의평가 크롤링 완료. 총 {}개의 리뷰를 저장했습니다.", totalCount);
        } catch (Exception e) {
            logger.error("크롤링 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("크롤링 실패: " + e.getMessage(), e);
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }
    CommunityPostSnapshot extractCommunityPost(Element listArticle, Document detailDocument) {
        Element article = detailDocument == null ? listArticle : findPostRoot(detailDocument, listArticle);
        Element cleanArticle = article == null ? null : article.clone();
        if (cleanArticle != null) {
            cleanArticle.select("div.comments, section.comments, ul.comments").remove();
        }
        String title = textOfFirst(cleanArticle,
                "h1.title, h2.title, h3.title, h1.large, h2.large, h3.large, p.title");
        String content = textOfFirst(cleanArticle,
                "p.large, p.text, div.content, p.medium");
        if (content.isBlank()) {
            content = cleanArticle == null ? "" : cleanArticle.ownText().trim();
        }

        int likesCount = numberOfFirst(article,
                "ul.status > li.vote, li.vote, span.vote, button.vote, .status .vote, [class~=vote]");
        java.util.List<String> comments = extractComments(detailDocument);
        return new CommunityPostSnapshot(title, content, likesCount, comments);
    }

    private Element findPostRoot(Document detailDocument, Element fallback) {
        Element root = detailDocument.selectFirst(
                "#container > div.wrap.articles > article > a.article, "
                        + "div.wrap.articles > article > a.article, article.item, main article a.article");
        return root == null ? fallback : root;
    }

    private java.util.List<String> extractComments(Document detailDocument) {
        if (detailDocument == null) return java.util.List.of();

        Elements commentRoots = detailDocument.select(
                "div.comments > article, section.comments > article, div.wrap.comments > article, "
                        + "article.comment, div.comments > div.comment, section.comments > div.comment, "
                        + "ul.comments > li.comment");
        return commentRoots.stream()
                .map(comment -> {
                    Element text = comment.selectFirst(
                            "div.wrap > p.text, p.text, div.text, p");
                    return text == null ? "" : text.text().trim();
                })
                .filter(comment -> !comment.isBlank())
                .filter(comment -> !comment.equals("삭제된 댓글입니다."))
                .distinct()
                .limit(30)
                .toList();
    }

    private String textOfFirst(Element root, String selector) {
        if (root == null) return "";
        Element element = root.selectFirst(selector);
        return element == null ? "" : element.text().trim();
    }

    private int numberOfFirst(Element root, String selector) {
        String text = textOfFirst(root, selector);
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("-?\\d[\\d,]*").matcher(text);
        if (!matcher.find()) return 0;
        try {
            return Math.max(0, Integer.parseInt(matcher.group().replace(",", "")));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private String serializeComments(java.util.List<String> comments) {
        try {
            return objectMapper.writeValueAsString(comments);
        } catch (Exception ignored) {
            return "[]";
        }
    }

    record CommunityPostSnapshot(
            String title,
            String content,
            int likesCount,
            java.util.List<String> comments
    ) { }
}
