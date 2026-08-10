package com.uniwiki.service;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import static org.assertj.core.api.Assertions.assertThat;

class OfficialSourceSchedulerTest {

    @Test
    void runsAtMidnightInKorea() throws Exception {
        Scheduled scheduled = OfficialSourceScheduler.class
                .getMethod("collect")
                .getAnnotation(Scheduled.class);

        assertThat(scheduled.cron()).isEqualTo("${uniwiki.official-sources.cron:0 0 0 * * *}");
        assertThat(scheduled.zone()).isEqualTo("${uniwiki.official-sources.zone:Asia/Seoul}");
    }
}
