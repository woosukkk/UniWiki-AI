package com.uniwiki.service;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OfficialRobotsPolicy {
    private static final long WWW_DELAY_MILLIS = 10_000;
    private final Map<String, Long> lastRequestAt = new ConcurrentHashMap<>();

    public void awaitAllowed(String url) throws InterruptedException {
        URI uri = URI.create(url);
        requireAllowed(url);
        if (!"www.sejong.ac.kr".equalsIgnoreCase(uri.getHost())) return;
        synchronized (lastRequestAt) {
            long wait = WWW_DELAY_MILLIS
                    - (System.currentTimeMillis() - lastRequestAt.getOrDefault(uri.getHost(), 0L));
            if (wait > 0) Thread.sleep(wait);
            lastRequestAt.put(uri.getHost(), System.currentTimeMillis());
        }
    }

    public void requireAllowed(String url) {
        if (!isAllowed(URI.create(url))) {
            throw new IllegalArgumentException("robots.txt가 차단한 URL입니다: " + url);
        }
    }

    boolean isAllowed(URI uri) {
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
        String path = uri.getRawPath() == null ? "/" : uri.getRawPath();
        String target = path + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
        if (host.equals("tosc.sejong.ac.kr") || host.equals("udream.sejong.ac.kr")) {
            return path.equals("/") && uri.getRawQuery() == null;
        }
        if (host.equals("www.sejong.ac.kr") && target.contains("mode=download")) return false;
        if (host.equals("www.sejong.ac.kr") || host.equals("dept.sejong.ac.kr")
                || host.equals("sw.sejong.ac.kr")) {
            return !path.startsWith("/_fox") && !path.startsWith("/_attach/")
                    && !path.startsWith("/_custom/") && !path.startsWith("/_res/");
        }
        return true;
    }
}
