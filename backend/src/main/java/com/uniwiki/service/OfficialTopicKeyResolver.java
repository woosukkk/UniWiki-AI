package com.uniwiki.service;

import com.uniwiki.entity.OfficialSource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class OfficialTopicKeyResolver {

    private static final Pattern ROUND = Pattern.compile("제\\s*(\\d+)\\s*회");
    private static final Pattern SEMESTER = Pattern.compile("(20\\d{2})(?:학년도)?[-\\s]*(1|2)학기");

    public String resolve(OfficialSource source, String title, String sourceUrl) {
        String normalized = title == null ? "" : title.replaceAll("\\s+", " ").trim();
        if (isTosc(source, normalized)) {
            Matcher round = ROUND.matcher(normalized);
            if (round.find()) {
                return "TOSC:" + round.group(1);
            }
        }
        if (normalized.contains("수강편람") || normalized.contains("강의시간표")) {
            Matcher semester = SEMESTER.matcher(normalized);
            if (semester.find()) {
                return "COURSE_GUIDE:" + semester.group(1) + ":" + semester.group(2);
            }
        }
        return "DOCUMENT:" + sha256(source.getId() + ":" + sourceUrl);
    }

    public String displayTitle(String topicKey, String fallbackTitle) {
        if (topicKey.startsWith("TOSC:")) {
            String[] parts = topicKey.split(":");
            return "제" + parts[1] + "회 TOSC";
        }
        if (topicKey.startsWith("COURSE_GUIDE:")) {
            String[] parts = topicKey.split(":");
            return parts[1] + "-" + parts[2] + " 수강편람 및 강의시간표";
        }
        return fallbackTitle;
    }

    private boolean isTosc(OfficialSource source, String title) {
        return source.getListUrl().contains("tosc.sejong.ac.kr")
                || title.contains("TOSC")
                || title.contains("SW코딩역량평가");
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("공식 문서 주제 키 생성에 실패했습니다.", exception);
        }
    }
}
