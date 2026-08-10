package com.uniwiki.service;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

class OfficialRobotsPolicyTest {
    private final OfficialRobotsPolicy policy = new OfficialRobotsPolicy();

    @Test
    void blocksRestrictedHostsAndDownloadsButKeepsPublicNotices() {
        assertFalse(policy.isAllowed(URI.create("https://tosc.sejong.ac.kr/ko/notice")));
        assertFalse(policy.isAllowed(URI.create("https://udream.sejong.ac.kr/Career/ProgramList.aspx")));
        assertFalse(policy.isAllowed(URI.create("https://www.sejong.ac.kr/notice.do?mode=download&id=1")));
        assertFalse(policy.isAllowed(URI.create("https://sw.sejong.ac.kr/_attach/file.pdf")));
        assertTrue(policy.isAllowed(URI.create("https://www.sejong.ac.kr/kor/intro/notice3.do?mode=view")));
        assertTrue(policy.isAllowed(URI.create("https://dept.sejong.ac.kr/softwaredpt/board/notice.do")));
    }
}
