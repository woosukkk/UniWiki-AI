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
    static final String SW_CENTER_TOPIC = "SOURCE:SW_CENTER";
    static final String UDREAM_TOPIC = "SOURCE:UDREAM";

    public String resolve(OfficialSource source, String title, String sourceUrl) {
        String normalized = title == null ? "" : title.replaceAll("\\s+", " ").trim();
        if (source.getListUrl().contains("sw.sejong.ac.kr/sw/notice.do")) {
            return SW_CENTER_TOPIC;
        }
        if (source.getListUrl().contains("udream.sejong.ac.kr")) {
            return UDREAM_TOPIC;
        }
        if (isTosc(source, normalized)) {
            Matcher round = ROUND.matcher(normalized);
            if (round.find()) {
                return "TOSC:" + round.group(1);
            }
        }
        return "DOCUMENT:" + sha256(source.getId() + ":" + sourceUrl);
    }

    public String displayTitle(String topicKey, String fallbackTitle) {
        if (topicKey.startsWith("TOSC:")) {
            String[] parts = topicKey.split(":");
            return "제" + parts[1] + "회 TOSC";
        }
        if (SW_CENTER_TOPIC.equals(topicKey)) return "SW중심대학사업단 공지 모음";
        if (UDREAM_TOPIC.equals(topicKey)) return "uDream 비교과·진로 프로그램 모음";
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
