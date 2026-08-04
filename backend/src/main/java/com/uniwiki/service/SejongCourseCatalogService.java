package com.uniwiki.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SejongCourseCatalogService {

    private final ObjectMapper objectMapper;

    @Value("${uniwiki.everytime.course-data-path:../ai/data/normalized/sejong/software-course-schedules.json}")
    private String courseDataPath;

    public List<CourseTarget> loadTargets(List<String> requestedTerms, Integer limit) {
        Path path = Path.of(courseDataPath).toAbsolutePath().normalize();
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException("강의시간표 데이터를 찾을 수 없습니다: " + path);
        }

        Set<String> terms = requestedTerms == null ? Set.of() : new LinkedHashSet<>(requestedTerms);
        LinkedHashSet<CourseTarget> targets = new LinkedHashSet<>();
        try {
            JsonNode root = objectMapper.readTree(path.toFile());
            for (JsonNode termNode : root.path("terms")) {
                if (!terms.isEmpty() && !terms.contains(termNode.path("term").asText())) {
                    continue;
                }
                for (JsonNode course : termNode.path("courses")) {
                    String courseName = course.path("교과목명").asText("").trim();
                    String professor = course.path("메인 교수명").asText("").trim();
                    if (courseName.isBlank() || professor.isBlank() || professor.equals("-")) {
                        continue;
                    }
                    targets.add(new CourseTarget(courseName, professor));
                    if (limit != null && targets.size() >= limit) {
                        return new ArrayList<>(targets);
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("강의시간표 데이터를 읽을 수 없습니다: " + path, e);
        }
        return new ArrayList<>(targets);
    }

    public record CourseTarget(String courseName, String professor) {
    }
}
