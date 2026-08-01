package com.uniwiki.service;

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

    private final com.uniwiki.repository.RawCommunityPostRepository rawCommunityPostRepository;
    private final com.uniwiki.repository.RawLectureEvaluationRepository rawLectureEvaluationRepository;
    
    private static final Logger logger = LoggerFactory.getLogger(EverytimeCrawlerService.class);

    private static final String LOGIN_URL = "https://account.everytime.kr/login";

    public void crawlBoardAndSave(String boardUrl, String boardType, int startPage, int endPage) {
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
            loginWait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("a.my, a.message")));

            int totalCount = 0;
            
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
                
                Document doc = Jsoup.parse(driver.getPageSource());
                Elements articles = doc.select("article > a.article");
                if (articles.isEmpty()) {
                    articles = doc.select("article");
                }
                
                if (articles.isEmpty()) {
                    logger.warn("게시물을 찾을 수 없습니다. 현재 페이지 DOM 일부: {}", 
                            driver.getPageSource().substring(0, Math.min(500, driver.getPageSource().length())));
                }
                
                for (Element article : articles) {
                    String title = article.select("h2").text();
                    String content = article.select("p").text();
                    
                    if (title.isEmpty() && content.isEmpty()) {
                        content = article.text();
                    }

                    if (title.isEmpty() && content.isEmpty()) continue;
                    
                    if (title.isEmpty()) {
                        title = content.length() > 20 ? content.substring(0, 20) + "..." : content;
                    }
                    if (content.isEmpty()) {
                        content = title;
                    }

                    int likesCount = 0;
                    Element voteEl = article.selectFirst(".vote");
                    if (voteEl != null && !voteEl.text().isEmpty()) {
                        try {
                            likesCount = Integer.parseInt(voteEl.text());
                        } catch (NumberFormatException ignored) {}
                    }
                    
                    RawCommunityPost post = new RawCommunityPost(
                            currentUrl, 
                            boardType != null ? boardType : "알 수 없는 게시판", 
                            title, 
                            content, 
                            likesCount, 
                            "[]"
                    );
                    rawCommunityPostRepository.save(post);
                    totalCount++;
                }
                
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
            
            driver.get(lectureUrl);
            logger.info("강의평가 크롤링 시작: {}", lectureUrl);
            
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.info, article")));
            
            Document doc = Jsoup.parse(driver.getPageSource());
            Elements articles = doc.select("div.articles > article, div.articles > div.article");
            if (articles.isEmpty()) {
                articles = doc.select("article, div.article");
            }
            
            if (articles.isEmpty()) {
                logger.warn("강의평가를 찾을 수 없습니다. 현재 페이지 DOM 일부: {}", 
                        driver.getPageSource().substring(0, Math.min(500, driver.getPageSource().length())));
            }
            
            String courseName = "강의명 미상";
            String professor = "교수 미상";
            
            Element infoDiv = doc.selectFirst("div.info");
            if (infoDiv != null) {
                Element h2 = infoDiv.selectFirst("h2");
                if (h2 != null) courseName = h2.text();
                Element p = infoDiv.selectFirst("p");
                if (p != null) professor = p.text().split(" ")[0];
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


}
