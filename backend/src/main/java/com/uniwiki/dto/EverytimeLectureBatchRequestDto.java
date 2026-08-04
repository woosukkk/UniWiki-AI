package com.uniwiki.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class EverytimeLectureBatchRequestDto {

    private List<String> terms;

    @Min(1)
    @Max(200)
    private Integer maxCourseProfessorPairs;

    @Min(1)
    @NotNull
    private Integer startPage = 1;

    @Min(1)
    @Max(20)
    @NotNull
    private Integer endPage = 3;

    @Min(0)
    @Max(10000)
    @NotNull
    private Integer requestDelayMillis = 1500;

    @AssertTrue(message = "endPage는 startPage보다 작을 수 없습니다.")
    public boolean isPageRangeValid() {
        return startPage == null || endPage == null || endPage >= startPage;
    }
}
