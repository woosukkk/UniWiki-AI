package com.uniwiki.config;

import com.uniwiki.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "uniwiki.questions.close-answered-on-startup",
        havingValue = "true"
)
public class AnsweredQuestionStatusBootstrap implements ApplicationRunner {

    private final QuestionRepository questionRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int updated = questionRepository.closeAnsweredQuestions();
        log.info("Closed {} answered questions; unanswered questions were unchanged", updated);
    }
}
