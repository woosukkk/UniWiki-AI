package com.uniwiki.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LectureReviewWorkflowScheduler {

    private final LectureReviewWorkflowService workflowService;

    @Scheduled(fixedDelayString = "${uniwiki.lecture-review-workflow.interval-ms:10000}")
    public void process() {
        try {
            LectureReviewWorkflowService.Result result = workflowService.processPending();
            if (result.processed() > 0) {
                log.info("강의평 정제 완료: 처리 {}, 통과 {}, 제외 {}",
                        result.processed(), result.accepted(), result.rejected());
            }
        } catch (Exception e) {
            log.error("강의평 자동 정제 워크플로 실행 실패", e);
        }
    }
}
