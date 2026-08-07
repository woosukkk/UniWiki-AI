package com.uniwiki.service;

import com.uniwiki.entity.Category;
import com.uniwiki.entity.User;
import com.uniwiki.entity.WikiPost;
import com.uniwiki.entity.WikiPostStatus;
import com.uniwiki.repository.CategoryRepository;
import com.uniwiki.repository.UserRepository;
import com.uniwiki.repository.WikiPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Order(21)
@ConditionalOnProperty(name = "uniwiki.official-sources.bootstrap-enabled", havingValue = "true")
public class PinnedWikiBootstrap implements ApplicationRunner {

    private static final String SOURCE_TITLE = "UniWiki 공식 데이터 출처 안내";
    private static final String COURSE_GUIDE_TITLE = "세종대학교 수강편람";

    private final WikiPostRepository wikiPostRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final WikiVectorSyncService vectorSyncService;

    @Value("${uniwiki.official-sources.author-id:1}")
    private Long authorId;

    @Value("${uniwiki.official-sources.author-email:official-source@local.invalid}")
    private String authorEmail;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        User author = userRepository.findByEmail(authorEmail)
                .or(() -> userRepository.findById(authorId))
                .orElseThrow(() -> new IllegalStateException("공식 위키 작성자를 찾을 수 없습니다."));
        Category campusLife = categoryRepository.findByName("학교생활")
                .orElseThrow(() -> new IllegalStateException("학교생활 카테고리가 없습니다."));
        Category academic = categoryRepository.findByName("학사")
                .orElseThrow(() -> new IllegalStateException("학사 카테고리가 없습니다."));

        upsertPinned(campusLife, author, SOURCE_TITLE, sourceContent(),
                "UniWiki가 자동 수집하는 세종대학교 공식 데이터 출처 안내입니다.", 1);
        upsertPinned(academic, author, COURSE_GUIDE_TITLE, courseGuideContent(),
                "학기별 수강편람과 강의시간표를 확인하는 고정 안내 문서입니다.", 2);
    }

    private void upsertPinned(Category category, User author, String title, String content,
                              String summary, int order) {
        WikiPost post = wikiPostRepository.findByTitle(title)
                .orElseGet(() -> wikiPostRepository.save(new WikiPost(
                        category, author, title, content, summary, WikiPostStatus.APPROVED)));
        post.update(category, title, content, summary, WikiPostStatus.APPROVED);
        post.pin(order);
        vectorSyncService.enqueueUpsert(post);
    }

    private String sourceContent() {
        return """
                # UniWiki 공식 데이터 출처 안내

                UniWiki의 공식 학교 위키는 아래 세종대학교 공식 사이트의 게시물과 첨부파일을 자동으로 수집합니다. 신규·변경 자료만 갱신하고 원문 URL과 내용 해시를 함께 보존합니다.

                ## 등록된 공식 게시판

                - [세종대학교 학사공지](https://www.sejong.ac.kr/kor/intro/notice3.do)
                - [세종대학교 취업공지](https://www.sejong.ac.kr/kor/intro/notice6.do)
                - [세종대학교 장학공지](https://www.sejong.ac.kr/kor/intro/notice7.do)
                - [소프트웨어학과 공지](https://dept.sejong.ac.kr/softwaredpt/board/notice.do)
                - [SW중심대학사업단 공지](https://sw.sejong.ac.kr/sw/notice.do)
                - [TOSC 공지](https://tosc.sejong.ac.kr/ko/cusomter_support/notice)
                - [uDream 비교과·진로 프로그램](https://udream.sejong.ac.kr/Career/CareerTask/ProgramList.aspx)
                - [세종대학교 학사안내 전체](https://www.sejong.ac.kr/kor/academics/academic-calendar.do)

                ## 처리 범위

                게시글 제목과 본문뿐 아니라 PDF, XLS, XLSX, DOCX, TXT, CSV 첨부파일의 텍스트도 추출합니다. 같은 시험 회차나 같은 학기 수강편람처럼 주제가 같은 자료는 하나의 위키로 통합하고 모든 원문 출처를 남깁니다.

                개설 강좌의 최종 시간·강의실·담당 교수는 학사정보시스템을 최종 기준으로 확인해야 합니다.
                """;
    }

    private String courseGuideContent() {
        return """
                # 세종대학교 수강편람

                이 문서는 학기별 수강편람과 강의시간표를 찾기 위한 고정 안내입니다. 수강편람 원문과 첨부된 PDF·Excel 파일은 세종대학교 학사공지에서 자동 수집됩니다.

                ## 공식 확인 위치

                - [세종대학교 학사공지](https://www.sejong.ac.kr/kor/intro/notice3.do)
                - [세종대학교 수강신청 안내](https://www.sejong.ac.kr/kor/academics/register-for-class.do)
                - 세종대학교 학사정보시스템

                ## 확인할 내용

                - 학기별 수강신청 일정과 절차
                - 교양·전공 이수 기준과 졸업요건
                - 학점 제한, 재수강, 철회 및 폐강 기준
                - 강의시간표, 강의실과 담당 교수 변경사항

                수강편람은 게시 시점 기준 자료이므로 실제 수강신청 전에는 학사정보시스템에서 개설 강좌를 다시 확인해야 합니다.
                """;
    }
}
