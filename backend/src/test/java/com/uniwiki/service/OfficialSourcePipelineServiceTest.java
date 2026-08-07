package com.uniwiki.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import com.uniwiki.entity.OfficialSource;

import static org.assertj.core.api.Assertions.assertThat;

class OfficialSourcePipelineServiceTest {

    private final OfficialSourcePipelineService service =
            new OfficialSourcePipelineService(null, null, null, null, null, null, null, null, null);

    @Test
    void buildsListPageUrlsForSupportedBoards() {
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
    void removesPaginationParametersFromArticleIdentity() {
        assertThat(service.canonicalArticleUrl(
                "https://www.sejong.ac.kr/kor/intro/notice3.do"
                        + "?mode=view&articleNo=891086&article.offset=20&articleLimit=10"))
                .isEqualTo("https://www.sejong.ac.kr/kor/intro/notice3.do"
                        + "?mode=view&articleNo=891086");
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
