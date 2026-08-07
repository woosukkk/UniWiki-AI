package com.uniwiki.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EverytimeCrawlerServiceTest {

    private final EverytimeCrawlerService service =
            new EverytimeCrawlerService(null, null, new ObjectMapper());

    @Test
    void extractsPostBodyCommentsAndRecommendationCountSeparately() {
        Document detail = Jsoup.parse("""
                <div id="container">
                  <div class="wrap articles">
                    <article>
                      <a class="article" href="/123">
                        <h2 class="title">수강 신청 질문</h2>
                        <p class="text">게시글 본문입니다.</p>
                        <ul class="status">
                          <li class="vote">추천 1,234</li>
                          <li class="comment">댓글 2</li>
                        </ul>
                      </a>
                    </article>
                  </div>
                  <div class="comments">
                    <article><div class="wrap"><p class="text">첫 번째 댓글</p></div></article>
                    <article><div class="wrap"><p class="text">두 번째 댓글</p></div></article>
                  </div>
                </div>
                """, "https://everytime.kr/123");
        Element listArticle = detail.selectFirst("a.article");

        EverytimeCrawlerService.CommunityPostSnapshot result =
                service.extractCommunityPost(listArticle, detail);

        assertThat(result.title()).isEqualTo("수강 신청 질문");
        assertThat(result.content()).isEqualTo("게시글 본문입니다.");
        assertThat(result.likesCount()).isEqualTo(1234);
        assertThat(result.comments()).containsExactly("첫 번째 댓글", "두 번째 댓글");
        assertThat(result.content()).doesNotContain("첫 번째 댓글", "추천 1,234");
    }

    @Test
    void supportsCommentDivMarkupAndRemovesDuplicateComments() {
        Document detail = Jsoup.parse("""
                <main><article class="item"><h1 class="title">제목</h1><div class="content">본문</div>
                  <button class="vote">5</button></article></main>
                <section class="comments">
                  <div class="comment"><div class="text">같은 댓글</div></div>
                  <div class="comment"><div class="text">같은 댓글</div></div>
                </section>
                """);

        EverytimeCrawlerService.CommunityPostSnapshot result =
                service.extractCommunityPost(detail.selectFirst("article"), detail);

        assertThat(result.likesCount()).isEqualTo(5);
        assertThat(result.comments()).containsExactly("같은 댓글");
    }

    @Test
    void extractsTitlelessLargeBodyWithoutCommentOrStatusText() {
        Document detail = Jsoup.parse("""
                <article class="item">
                  <a class="article"><h3>익명</h3><time>08/07</time>
                    <p class="large">제목 없는 게시글 본문</p>
                    <ul class="status"><li class="vote">12</li><li class="comment">1</li></ul>
                  </a>
                  <div class="comments"><article><p class="text">댓글 본문</p></article></div>
                </article>
                """);

        EverytimeCrawlerService.CommunityPostSnapshot result =
                service.extractCommunityPost(detail.selectFirst("article.item"), detail);

        assertThat(result.title()).isBlank();
        assertThat(result.content()).isEqualTo("제목 없는 게시글 본문");
        assertThat(result.comments()).containsExactly("댓글 본문");
    }
}
