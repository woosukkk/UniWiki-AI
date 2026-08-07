package com.uniwiki.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OfficialSourcePipelineServiceTest {

    private final OfficialSourcePipelineService service =
            new OfficialSourcePipelineService(null, null, null, null, null, null, null, null);

    @Test
    void buildsListPageUrlsForSejongAndToscBoards() {
        assertThat(service.listPageUrl(
                "https://www.sejong.ac.kr/kor/intro/notice3.do", 3))
                .isEqualTo("https://www.sejong.ac.kr/kor/intro/notice3.do"
                        + "?mode=list&articleLimit=100&article.offset=200");
        assertThat(service.listPageUrl(
                "https://tosc.sejong.ac.kr/ko/cusomter_support/notice", 3))
                .isEqualTo("https://tosc.sejong.ac.kr/ko/cusomter_support/notice?p=3");
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
}
