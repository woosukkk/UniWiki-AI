package com.uniwiki.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EverytimeBoardRequestDto {

    private String boardUrl; // e.g., https://everytime.kr/370441
    private String boardType; // "Question" or "WikiPost"
    private Integer startPage = 1; // Default to page 1
    private Integer endPage = 1;   // Default to page 1
    
    private java.util.List<String> titleKeywords;
    private java.util.List<String> contentKeywords;
}
