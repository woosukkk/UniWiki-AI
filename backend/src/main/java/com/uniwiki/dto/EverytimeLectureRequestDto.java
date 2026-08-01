package com.uniwiki.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EverytimeLectureRequestDto {

    private String lectureUrl; // e.g., https://everytime.kr/lecture/view/1234567
    private Integer startPage = 1; // Default to page 1
    private Integer endPage = 1;   // Default to page 1
}
