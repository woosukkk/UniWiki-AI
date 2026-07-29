package com.uniwiki.service;

import com.uniwiki.dto.AdminDashboardDto;
import com.uniwiki.entity.User;
import com.uniwiki.entity.VectorSyncStatus;
import com.uniwiki.repository.AnswerRepository;
import com.uniwiki.repository.QuestionRepository;
import com.uniwiki.repository.UserRepository;
import com.uniwiki.repository.WikiPostRepository;
import com.uniwiki.repository.WikiVectorSyncJobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {
    @Mock UserRepository userRepository;
    @Mock WikiPostRepository wikiPostRepository;
    @Mock QuestionRepository questionRepository;
    @Mock AnswerRepository answerRepository;
    @Mock WikiVectorSyncJobRepository syncJobRepository;
    @InjectMocks AdminDashboardService service;

    @Test
    void returnsCountsForAdmin() {
        User admin = User.builder().id(1L).role("ADMIN").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.count()).thenReturn(3L);
        when(wikiPostRepository.count()).thenReturn(4L);
        when(questionRepository.count()).thenReturn(5L);
        when(answerRepository.count()).thenReturn(6L);
        when(syncJobRepository.countByStatus(VectorSyncStatus.PENDING)).thenReturn(1L);
        when(syncJobRepository.countByStatus(VectorSyncStatus.FAILED)).thenReturn(2L);
        when(syncJobRepository.findTop20ByOrderByCreatedAtDesc()).thenReturn(List.of());

        AdminDashboardDto result = service.getDashboard(1L);
        assertThat(result.userCount()).isEqualTo(3);
        assertThat(result.failedSyncCount()).isEqualTo(2);
    }

    @Test
    void rejectsNonAdmin() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(User.builder().id(2L).role("USER").build()));
        assertThatThrownBy(() -> service.getDashboard(2L)).isInstanceOf(ResponseStatusException.class);
    }
}
