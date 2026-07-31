package com.uniwiki.service;

import com.uniwiki.entity.*;
import com.uniwiki.repository.CategoryRepository;
import com.uniwiki.repository.QuestionRepository;
import com.uniwiki.repository.UserRepository;
import com.uniwiki.repository.WikiPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Map;
import java.time.Duration;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

@Slf4j
@Service
@RequiredArgsConstructor
public class EverytimeCrawlerService {

    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final WikiPostRepository wikiPostRepository;
    private final CategoryRepository categoryRepository;
    
    private static final Logger logger = LoggerFactory.getLogger(EverytimeCrawlerService.class);

    private static final String LOGIN_URL = "https://everytime.kr/user/login";

    @Transactional
    public void crawlAndSave(String boardUrl, String targetTable, Long categoryId) {
        // 1. 셀레니움 옵션 설정 (디버깅 및 수동 로그인을 위해 창을 띄움)
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
            logger.info("에브리타임 크롤링 시작: 브라우저 초기화 완료");

            // 2. 봇 유저(작성자) 확보 (없으면 생성)
            User botUser = getOrCreateBotUser();

            // 3. 에브리타임 메인 접속 (로그인 확인용)
            driver.get("https://everytime.kr");
            
            // 4. 수동 로그인 대기 (최대 60초)
            // 우측 상단의 '내 정보(a.my)' 버튼이나 '쪽지함(a.message)' 버튼이 보이면 로그인된 것으로 간주
            try {
                logger.info("에브리타임 로그인 상태 확인 중... (미로그인 시 브라우저에서 직접 로그인해주세요. 60초 대기)");
                WebDriverWait loginWait = new WebDriverWait(driver, Duration.ofSeconds(60));
                loginWait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("a.my, a.message")));
                logger.info("에브리타임 로그인 확인 완료!");
            } catch (Exception e) {
                logger.error("60초 내에 로그인이 감지되지 않았습니다. 브라우저에서 직접 아이디/비번을 입력해 로그인해주세요.");
                throw new RuntimeException("수동 로그인 대기 시간 초과");
            }


            // 4. 실제 게시판 URL로 이동
            driver.get(boardUrl);
            
            // 5. 자바스크립트 렌더링 대기 (글 목록 article 엘리먼트가 나타날 때까지 최대 10초 대기)
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("article > a.article")));
            
            // 6. 페이지가 로드되면 Jsoup으로 HTML 소스 파싱
            Document doc = Jsoup.parse(driver.getPageSource());
            Elements articles = doc.select("article > a.article");
            
            int count = 0;
            for (Element article : articles) {
                String title = article.select("h2").text();
                String content = article.select("p").text();

                if (title.isEmpty() && content.isEmpty()) {
                    continue; // 제목도 내용도 없으면 패스
                }
                
                // 제목이 없으면 내용의 첫 20자를 제목으로 사용
                if (title.isEmpty()) {
                    title = content.length() > 20 ? content.substring(0, 20) + "..." : content;
                }
                if (content.isEmpty()) {
                    content = title;
                }

                // 7. DB에 저장
                if ("Question".equalsIgnoreCase(targetTable)) {
                    Question question = new Question(botUser, title, content);
                    questionRepository.save(question);
                    count++;
                } else if ("WikiPost".equalsIgnoreCase(targetTable)) {
                    if (categoryId == null) {
                        throw new IllegalArgumentException("WikiPost로 저장하려면 categoryId가 필수입니다.");
                    }
                    Category category = categoryRepository.findById(categoryId)
                            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리 ID입니다."));
                    
                    WikiPost wikiPost = new WikiPost(
                            category, 
                            botUser, 
                            title, 
                            content, 
                            "에브리타임 자동 수집", 
                            WikiPostStatus.APPROVED
                    );
                    wikiPostRepository.save(wikiPost);
                    count++;
                }
            }
            logger.info("에브리타임 셀레니움 크롤링 완료. 총 {}개의 게시물을 {} 테이블에 저장했습니다.", count, targetTable);
            
        } catch (Exception e) {
            logger.error("에브리타임 셀레니움 크롤링 중 오류 발생", e);
            throw new RuntimeException("크롤링 실패: " + e.getMessage());
        } finally {
            // 브라우저 자원 반납
            if (driver != null) {
                driver.quit();
            }
        }
    }



    private User getOrCreateBotUser() {
        return userRepository.findByEmail("everytime_bot@uniwiki.com")
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email("everytime_bot@uniwiki.com")
                            .password("1234") // 더미 비밀번호
                            .nickname("에타크롤링봇")
                            .role("ADMIN")
                            .build();
                    return userRepository.save(newUser);
                });
    }
}
