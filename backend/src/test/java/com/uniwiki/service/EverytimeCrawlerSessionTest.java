package com.uniwiki.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniwiki.dto.CommunityPostImportResponseDto;
import com.uniwiki.repository.RawLectureEvaluationRepository;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EverytimeCrawlerSessionTest {
    @Test
    void reusesAuthenticatedProfileWithoutOpeningLoginPage() {
        CommunityPostTransferService transfer = mock(CommunityPostTransferService.class);
        when(transfer.transfer(anyList())).thenReturn(new CommunityPostImportResponseDto(0, 0, 0));
        EverytimeCrawlerService service = new EverytimeCrawlerService(
                transfer, mock(RawLectureEvaluationRepository.class), new ObjectMapper());
        WebElement accountMenu = mock(WebElement.class);
        when(accountMenu.isDisplayed()).thenReturn(true);
        try (MockedConstruction<ChromeDriver> construction = mockConstruction(ChromeDriver.class,
                (driver, context) -> {
                    when(driver.findElement(any(By.class))).thenReturn(accountMenu);
                    when(driver.getPageSource()).thenReturn("<html><body><div class='wrap articles'></div></body></html>");
                })) {
            service.crawlBoardAndSave("https://everytime.kr/hotarticle", "핫 게시판", 1, 1, null, null);
            ChromeDriver driver = construction.constructed().get(0);
            verify(driver, never()).get("https://account.everytime.kr/login");
            verify(driver).quit();
        }
    }
}
