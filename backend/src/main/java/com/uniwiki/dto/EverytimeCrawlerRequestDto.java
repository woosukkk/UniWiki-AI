package com.uniwiki.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EverytimeCrawlerRequestDto {

    private String boardUrl; // e.g., https://everytime.kr/370441
    private String targetTable; // "Question" or "WikiPost"
    private Long categoryId; // Required if targetTable is "WikiPost"
}
