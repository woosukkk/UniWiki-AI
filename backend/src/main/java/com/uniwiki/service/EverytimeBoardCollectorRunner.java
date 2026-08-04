package com.uniwiki.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "uniwiki.everytime.board-run-on-startup", havingValue = "true")
public class EverytimeBoardCollectorRunner implements ApplicationRunner {
    private final EverytimeCrawlerService crawlerService;
    private final ConfigurableApplicationContext applicationContext;

    @Value("${uniwiki.everytime.board-url:https://everytime.kr/hotarticle}")
    private String boardUrl;
    @Value("${uniwiki.everytime.board-type:핫 게시판}")
    private String boardType;
    @Value("${uniwiki.everytime.board-start-page:1}")
    private int startPage;
    @Value("${uniwiki.everytime.board-end-page:3}")
    private int endPage;

    @Override
    public void run(ApplicationArguments args) {
        try {
            crawlerService.crawlBoardAndSave(boardUrl, boardType, startPage, endPage, null, null);
        } finally {
            applicationContext.close();
        }
    }
}
