package com.uniwiki.service;

import com.uniwiki.dto.AdminDashboardDto;
import com.uniwiki.entity.User;
import com.uniwiki.entity.VectorSyncStatus;
import com.uniwiki.entity.WikiVectorSyncJob;
import com.uniwiki.repository.AnswerRepository;
import com.uniwiki.repository.QuestionRepository;
import com.uniwiki.repository.UserRepository;
import com.uniwiki.repository.WikiPostRepository;
import com.uniwiki.repository.WikiVectorSyncJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {
    private final UserRepository userRepository;
    private final WikiPostRepository wikiPostRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final WikiVectorSyncJobRepository syncJobRepository;

    public AdminDashboardDto getDashboard(Long loginUserId) {
        User user = userRepository.findById(loginUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자를 찾을 수 없습니다."));
        if (!"ADMIN".equals(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자만 접근할 수 있습니다.");
        }

        var jobs = syncJobRepository.findTop20ByOrderByCreatedAtDesc();
        return new AdminDashboardDto(
                userRepository.count(),
                wikiPostRepository.count(),
                questionRepository.count(),
                answerRepository.count(),
                syncJobRepository.countByStatus(VectorSyncStatus.PENDING),
                syncJobRepository.countByStatus(VectorSyncStatus.FAILED),
                jobs.stream().map(this::toDto).toList()
        );
    }

    private AdminDashboardDto.SyncJob toDto(WikiVectorSyncJob job) {
        return new AdminDashboardDto.SyncJob(
                job.getId(), job.getWikiPostId(), job.getOperation().name(), job.getStatus().name(),
                job.getAttemptCount(), job.getLastError(), job.getCreatedAt(), job.getProcessedAt());
    }
}
