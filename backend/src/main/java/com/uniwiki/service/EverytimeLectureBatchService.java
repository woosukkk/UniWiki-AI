package com.uniwiki.service;

import com.uniwiki.dto.EverytimeLectureBatchRequestDto;
import com.uniwiki.dto.EverytimeLectureBatchResponseDto;
import com.uniwiki.entity.RawLectureEvaluation;
import com.uniwiki.repository.RawLectureEvaluationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class EverytimeLectureBatchService {

    private static final String LOGIN_URL = "https://account.everytime.kr/login";
    private static final String LECTURE_SEARCH_URL = "https://everytime.kr/lecture/search?keyword=";

    private final SejongCourseCatalogService courseCatalogService;
    private final RawLectureEvaluationRepository rawLectureEvaluationRepository;

    public EverytimeLectureBatchResponseDto crawl(EverytimeLectureBatchRequestDto request) {
        List<SejongCourseCatalogService.CourseTarget> targets = courseCatalogService.loadTargets(
                request.getTerms(), request.getMaxCourseProfessorPairs());
        List<EverytimeLectureBatchResponseDto.Item> items = new ArrayList<>();
        int matched = 0;
        int saved = 0;
        int duplicates = 0;

        WebDriver driver = new ChromeDriver(chromeOptions());
        try {
            waitForLogin(driver);
            for (SejongCourseCatalogService.CourseTarget target : targets) {
                EverytimeLectureBatchResponseDto.Item item;
                try {
                    SearchResult search = findLecture(driver, target);
                    if (search.status() != SearchStatus.MATCHED) {
                        item = new EverytimeLectureBatchResponseDto.Item(
                                target.courseName(), target.professor(), search.status().name(), null,
                                0, 0, search.message());
                    } else {
                        matched++;
                        CrawlCount count = crawlReviews(
                                driver,
                                search.url(),
                                target,
                                request.getStartPage(),
                                request.getEndPage());
                        saved += count.saved();
                        duplicates += count.duplicates();
                        item = new EverytimeLectureBatchResponseDto.Item(
                                target.courseName(), target.professor(), "MATCHED", search.url(),
                                count.saved(), count.duplicates(), null);
                    }
                } catch (Exception e) {
                    log.warn("강의평 배치 수집 실패: {} / {}", target.courseName(), target.professor(), e);
                    item = new EverytimeLectureBatchResponseDto.Item(
                            target.courseName(), target.professor(), "FAILED", null, 0, 0, e.getMessage());
                }
                items.add(item);
                pause(request.getRequestDelayMillis());
            }
        } finally {
            driver.quit();
        }
        return new EverytimeLectureBatchResponseDto(targets.size(), matched, saved, duplicates, items);
    }

    private ChromeOptions chromeOptions() {
        ChromeOptions options = new ChromeOptions();
        String profilePath = System.getProperty("java.io.tmpdir") + "/EverytimeChromeProfile";
        options.addArguments("user-data-dir=" + profilePath);
        options.addArguments("--disable-gpu", "--window-size=1920,1080", "--no-sandbox", "--disable-dev-shm-usage");
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        return options;
    }

    private void waitForLogin(WebDriver driver) {
        driver.get(LOGIN_URL);
        new WebDriverWait(driver, Duration.ofSeconds(60))
                .until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("a.my, a.message")));
    }

    private SearchResult findLecture(WebDriver driver, SejongCourseCatalogService.CourseTarget target) {
        String encoded = URLEncoder.encode(target.courseName(), StandardCharsets.UTF_8);
        driver.get(LECTURE_SEARCH_URL + encoded);
        waitForPage(driver);

        List<Candidate> candidates = readCandidates(driver);
        if (candidates.isEmpty()) {
            candidates = searchThroughForm(driver, target.courseName());
        }

        String expectedCourse = normalize(target.courseName());
        String expectedProfessor = normalizeProfessor(target.professor());
        Map<String, Candidate> exact = new LinkedHashMap<>();
        for (Candidate candidate : candidates) {
            String text = normalize(candidate.text());
            if (text.contains(expectedCourse) && text.contains(expectedProfessor)) {
                exact.putIfAbsent(candidate.url(), candidate);
            }
        }
        if (exact.size() == 1) {
            return new SearchResult(SearchStatus.MATCHED, exact.keySet().iterator().next(), null);
        }
        if (exact.size() > 1) {
            return new SearchResult(SearchStatus.AMBIGUOUS, null, "동일한 강의명·교수명 검색 결과가 여러 개입니다.");
        }
        return new SearchResult(SearchStatus.NOT_FOUND, null, "강의명·교수명이 모두 일치하는 결과가 없습니다.");
    }

    private void waitForPage(WebDriver driver) {
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
    }

    private List<Candidate> searchThroughForm(WebDriver driver, String keyword) {
        List<By> selectors = List.of(
                By.cssSelector("form.search input[type='text']"),
                By.cssSelector("input[name='keyword']"),
                By.cssSelector("input.search"),
                By.cssSelector("input[placeholder*='강의']")
        );
        for (By selector : selectors) {
            List<WebElement> inputs = driver.findElements(selector);
            if (inputs.isEmpty()) {
                continue;
            }
            WebElement input = inputs.getFirst();
            input.clear();
            input.sendKeys(keyword);
            input.sendKeys(Keys.ENTER);
            waitForPage(driver);
            return readCandidates(driver);
        }
        return List.of();
    }

    private List<Candidate> readCandidates(WebDriver driver) {
        Map<String, Candidate> candidates = new LinkedHashMap<>();
        Document document = Jsoup.parse(driver.getPageSource(), driver.getCurrentUrl());
        for (Element link : document.select("a[href*=/lecture/view/]")) {
            String url = link.absUrl("href");
            if (url == null || url.isBlank()) {
                continue;
            }
            String text = link.text();
            Element container = link.parent();
            for (int depth = 0; container != null && depth < 4; depth++, container = container.parent()) {
                String containerText = container.text();
                if (containerText.length() > text.length()) {
                    text = containerText;
                }
            }
            candidates.putIfAbsent(url.split("\\?")[0], new Candidate(url.split("\\?")[0], text));
        }
        return new ArrayList<>(candidates.values());
    }

    private CrawlCount crawlReviews(
            WebDriver driver,
            String lectureUrl,
            SejongCourseCatalogService.CourseTarget target,
            int startPage,
            int endPage
    ) {
        int saved = 0;
        int duplicates = 0;
        Set<String> seenThisRun = new LinkedHashSet<>();
        for (int page = startPage; page <= endPage; page++) {
            String reviewUrl = lectureUrl + "?tab=article&page=" + page;
            driver.get(reviewUrl);
            waitForPage(driver);
            Document document = Jsoup.parse(driver.getPageSource());
            Elements reviews = document.select("div.articles > article, div.articles > div.article, article.review");
            if (reviews.isEmpty()) {
                break;
            }
            int newOnPage = 0;
            for (Element review : reviews) {
                String content = review.select("p.text, div.text").text().trim();
                if (content.isBlank()) {
                    continue;
                }
                int starRating = readStarRating(review);
                int likesCount = readInteger(review.selectFirst("li.vote, span.vote"));
                String fingerprint = starRating + "|" + content;
                if (!seenThisRun.add(fingerprint)
                        || rawLectureEvaluationRepository.existsBySourceUrlAndCourseNameAndProfessorAndContent(
                        lectureUrl, target.courseName(), target.professor(), content)) {
                    duplicates++;
                    continue;
                }
                rawLectureEvaluationRepository.save(new RawLectureEvaluation(
                        lectureUrl, target.courseName(), target.professor(), starRating, likesCount, content));
                saved++;
                newOnPage++;
            }
            if (newOnPage == 0 && page > startPage) {
                break;
            }
        }
        return new CrawlCount(saved, duplicates);
    }

    private int readStarRating(Element review) {
        Element star = review.selectFirst("span.star > span.on");
        if (star == null) {
            return 0;
        }
        String style = star.attr("style");
        for (int rating = 5; rating >= 1; rating--) {
            if (style.contains((rating * 20) + "%")) {
                return rating;
            }
        }
        return 0;
    }

    private int readInteger(Element element) {
        if (element == null) {
            return 0;
        }
        String digits = element.text().replaceAll("[^0-9]", "");
        return digits.isBlank() ? 0 : Integer.parseInt(digits);
    }

    static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^0-9a-z가-힣]", "");
    }

    static String normalizeProfessor(String value) {
        return normalize(value).replace("교수", "");
    }

    private void pause(Integer milliseconds) {
        if (milliseconds == null || milliseconds <= 0) {
            return;
        }
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("강의평 배치 수집이 중단되었습니다.", e);
        }
    }

    private enum SearchStatus { MATCHED, NOT_FOUND, AMBIGUOUS }

    private record Candidate(String url, String text) { }
    private record SearchResult(SearchStatus status, String url, String message) { }
    private record CrawlCount(int saved, int duplicates) { }
}
