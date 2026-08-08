package com.uniwiki.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import com.uniwiki.entity.OfficialSource;

import static org.assertj.core.api.Assertions.assertThat;

class OfficialSourcePipelineServiceTest {

    private final OfficialSourcePipelineService service =
            new OfficialSourcePipelineService(null, null, null, null, null, null, null, null, null, null);

    @Test
    void buildsListPageUrlsForSupportedBoards() {
        assertThat(service.listPageUrl(
                "https://www.sejong.ac.kr/kor/intro/notice3.do", 1))
                .isEqualTo("https://www.sejong.ac.kr/kor/intro/notice3.do"
                        + "?mode=list&articleLimit=100&article.offset=0");
        assertThat(service.listPageUrl(
                "https://www.sejong.ac.kr/kor/intro/notice3.do", 3))
                .isEqualTo("https://www.sejong.ac.kr/kor/intro/notice3.do"
                        + "?mode=list&articleLimit=100&article.offset=200");
        assertThat(service.listPageUrl(
                "https://tosc.sejong.ac.kr/ko/cusomter_support/notice", 3))
                .isEqualTo("https://tosc.sejong.ac.kr/ko/cusomter_support/notice?p=3");
        assertThat(service.listPageUrl(
                "https://udream.sejong.ac.kr/Career/CareerTask/ProgramList.aspx", 3))
                .isEqualTo("https://udream.sejong.ac.kr/Career/CareerTask/ProgramList.aspx?rp=3");
    }

    @Test
    void extractsUdreamProgramUrlFromOnclick() {
        OfficialSource source = new OfficialSource(null, "uDream",
                "https://udream.sejong.ac.kr/Career/CareerTask/ProgramList.aspx",
                "li[onclick]", "#Title", "#Info", true);
        Document document = Jsoup.parse("""
                <li onclick="javascript:goView('C633F505CDA2E669B80E42B7E8C5015D')"></li>
                """);

        assertThat(service.articleUrl(source, document.selectFirst("li")))
                .isEqualTo("https://udream.sejong.ac.kr/community/Program/programView.aspx"
                        + "?pgdx=C633F505CDA2E669B80E42B7E8C5015D");
    }

    @Test
    void addsAcademicYearToCatalogTitles() {
        OfficialSource source = new OfficialSource(null, "학사안내 전체",
                "https://www.sejong.ac.kr/kor/academics/academic-calendar.do",
                "a[href^='/kor/academics/']", ".page-title", "#cms-content", true);

        assertThat(service.catalogTitle(source,
                "https://www.sejong.ac.kr/kor/academics/freshman-scholarship_2027.do",
                "신입생장학금"))
                .isEqualTo("2027학년도 신입생장학금");
        assertThat(service.catalogTitle(source,
                "https://www.sejong.ac.kr/kor/academics/curriculum2025.do",
                "연도별 교과과정"))
                .isEqualTo("2025학년도 교과과정");
    }

    @Test
    void mapsKnownCourseMaterialNoticesToSemesterTitles() {
        OfficialSource source = new OfficialSource(null, "학사공지",
                "https://www.sejong.ac.kr/kor/intro/notice3.do",
                "a", "h1", ".content", true);

        assertThat(service.catalogTitle(source,
                "https://www.sejong.ac.kr/kor/intro/notice3.do?articleNo=805695&mode=view",
                "수강신청 안내"))
                .isEqualTo("2024-1 수강편람 및 강의시간표");
        assertThat(service.catalogTitle(source,
                "https://www.sejong.ac.kr/kor/intro/notice3.do?articleNo=891086&mode=view",
                "수강편람 안내"))
                .isEqualTo("2026-2 수강편람 및 강의시간표");
    }

    @Test
    void removesPaginationParametersFromArticleIdentity() {
        assertThat(service.canonicalArticleUrl(
                "https://www.sejong.ac.kr/kor/intro/notice3.do"
                        + "?mode=view&articleNo=891086&article.offset=20&articleLimit=10"))
                .isEqualTo("https://www.sejong.ac.kr/kor/intro/notice3.do"
                        + "?articleNo=891086&mode=view");
        assertThat(service.canonicalArticleUrl(
                "https://tosc.sejong.ac.kr/ko/cusomter_support/notice/view/64?p=2"))
                .isEqualTo("https://tosc.sejong.ac.kr/ko/cusomter_support/notice/view/64");
    }

    @Test
    void readsToscOpenGraphTitleAndContent() {
        Document document = Jsoup.parse("""
                <meta property="og:title" content="TOSC 시험 안내">
                <meta property="og:description" content="시험 접수와 고사장 안내">
                """);

        assertThat(service.requiredText(document, "meta[property=og:title]", "제목"))
                .isEqualTo("TOSC 시험 안내");
        assertThat(service.requiredContent(document, "meta[property=og:description]"))
                .isEqualTo("시험 접수와 고사장 안내");
    }

    @Test
    void preservesParagraphsLineBreaksListsAndTableRows() {
        Document document = Jsoup.parse("""
                <main class="content">
                  <h2>Course registration</h2>
                  <p>Period: August 10<br>From 10 AM</p>
                  <ul><li>Check student ID</li><li>Check class time</li></ul>
                  <table><tr><th>Type</th><th>Period</th></tr><tr><td>Registration</td><td>August</td></tr></table>
                </main>
                """);

        assertThat(service.requiredContent(document, "main.content"))
                .isEqualTo("""
                        Course registration

                        Period: August 10
                        From 10 AM

                        - Check student ID
                        - Check class time

                        Type Period

                        Registration August
                        """.trim());
    }

    @Test
    void readsPublicationYearFromListEntry() {
        Document document = Jsoup.parse("""
                <table>
                  <tr><td class="date">2024-01-03</td><td><a class="title">Notice</a></td></tr>
                </table>
                """);

        assertThat(service.listEntryYear(document.selectFirst("a.title"))).isEqualTo(2024);
    }

    @Test
    void groupsSameToscRoundAndSemesterCourseGuides() {
        OfficialTopicKeyResolver resolver = new OfficialTopicKeyResolver();
        OfficialSource tosc = new OfficialSource(null, "TOSC", "https://tosc.sejong.ac.kr/notice",
                "a", "h1", ".content", true);
        OfficialSource academic = new OfficialSource(null, "학사공지", "https://www.sejong.ac.kr/notice3.do",
                "a", "h1", ".content", true);

        assertThat(resolver.resolve(tosc,
                "2026년 제16회 TOSC 시험 안내", "https://tosc.sejong.ac.kr/view/1"))
                .isEqualTo(resolver.resolve(tosc,
                        "[고사장 안내] 제16회 SW코딩역량평가 (2026년)",
                        "https://tosc.sejong.ac.kr/view/2"));
        assertThat(resolver.resolve(academic,
                "2026-2학기 수강편람", "https://www.sejong.ac.kr/view/1"))
                .isEqualTo(resolver.resolve(academic,
                        "2026학년도 2학기 강의시간표", "https://www.sejong.ac.kr/view/2"));
    }
}
