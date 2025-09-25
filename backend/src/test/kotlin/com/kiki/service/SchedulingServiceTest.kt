package com.kiki.service

import com.kiki.scraper.service.GameScrapingResult
import com.kiki.scraper.service.ScrapingResult
import com.kiki.scraper.service.ScrapingService
import com.kiki.service.RunTrigger
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
class SchedulingServiceTest {
    
    private lateinit var scrapingService: ScrapingService
    private lateinit var schedulingService: SchedulingService
    
    @BeforeEach
    fun setUp() {
        scrapingService = mockk()
        val emailService = mockk<EmailService>()
        val gameNoticeRepository = mockk<com.kiki.repository.GameNoticeRepository>()
        schedulingService = SchedulingService(scrapingService, emailService, gameNoticeRepository)
    }
    
    @Test
    fun `executeScrapingJob should successfully execute scraping and log results`() {
        // Given
        val mockResult = ScrapingResult(
            startTime = LocalDateTime.now().minusMinutes(1),
            endTime = LocalDateTime.now(),
            totalGames = 2,
            totalNewNotices = 3,
            gameResults = mapOf(
                "NIKKE" to GameScrapingResult(
                    gameName = "NIKKE",
                    startTime = LocalDateTime.now().minusMinutes(1),
                    endTime = LocalDateTime.now(),
                    scrapedNoticesCount = 5,
                    newNoticesCount = 2,
                    success = true
                ),
                "원신" to GameScrapingResult(
                    gameName = "원신",
                    startTime = LocalDateTime.now().minusMinutes(1),
                    endTime = LocalDateTime.now(),
                    scrapedNoticesCount = 3,
                    newNoticesCount = 1,
                    success = true
                )
            ),
            success = true
        )
        
        every { scrapingService.scrapeAllGames() } returns mockResult
        
        // When
        schedulingService.executeScrapingJob()
        
        // Then
        verify(exactly = 1) { scrapingService.scrapeAllGames() }
    }
    
    @Test
    fun `executeScrapingJob should handle scraping failure gracefully`() {
        // Given
        val mockResult = ScrapingResult(
            startTime = LocalDateTime.now().minusMinutes(1),
            endTime = LocalDateTime.now(),
            totalGames = 2,
            totalNewNotices = 0,
            gameResults = mapOf(
                "NIKKE" to GameScrapingResult(
                    gameName = "NIKKE",
                    startTime = LocalDateTime.now().minusMinutes(1),
                    endTime = LocalDateTime.now(),
                    scrapedNoticesCount = 0,
                    newNoticesCount = 0,
                    success = false,
                    errorMessage = "Network error"
                )
            ),
            success = false,
            errorMessage = "Scraping failed"
        )
        
        every { scrapingService.scrapeAllGames() } returns mockResult
        
        // When
        schedulingService.executeScrapingJob()
        
        // Then
        verify(exactly = 1) { scrapingService.scrapeAllGames() }
    }
    
    @Test
    fun `executeScrapingJob should handle exception during scraping`() {
        // Given
        every { scrapingService.scrapeAllGames() } throws RuntimeException("Critical error")
        
        // When & Then (should not throw exception)
        schedulingService.executeScrapingJob()
        
        verify(exactly = 1) { scrapingService.scrapeAllGames() }
    }
    
    @Test
    fun `executeScrapingJob should log when new notices are found`() {
        // Given
        val mockResult = ScrapingResult(
            startTime = LocalDateTime.now().minusMinutes(1),
            endTime = LocalDateTime.now(),
            totalGames = 1,
            totalNewNotices = 5, // 새로운 공지사항 있음
            gameResults = mapOf(
                "NIKKE" to GameScrapingResult(
                    gameName = "NIKKE",
                    startTime = LocalDateTime.now().minusMinutes(1),
                    endTime = LocalDateTime.now(),
                    scrapedNoticesCount = 10,
                    newNoticesCount = 5,
                    success = true
                )
            ),
            success = true
        )
        
        every { scrapingService.scrapeAllGames() } returns mockResult
        
        // When
        schedulingService.executeScrapingJob()
        
        // Then
        verify(exactly = 1) { scrapingService.scrapeAllGames() }
        // 로그에서 이메일 트리거 메시지가 출력되어야 함
    }
    
    @Test
    fun `executeManualScraping should execute scraping and return result`() {
        // Given
        val mockResult = ScrapingResult(
            startTime = LocalDateTime.now().minusMinutes(1),
            endTime = LocalDateTime.now(),
            totalGames = 2,
            totalNewNotices = 1,
            gameResults = mapOf(
                "NIKKE" to GameScrapingResult(
                    gameName = "NIKKE",
                    startTime = LocalDateTime.now().minusMinutes(1),
                    endTime = LocalDateTime.now(),
                    scrapedNoticesCount = 3,
                    newNoticesCount = 1,
                    success = true
                )
            ),
            success = true
        )
        
        every { scrapingService.scrapeAllGames() } returns mockResult
        
        // When
        val result = schedulingService.executeManualScraping()
        
        // Then
        assertEquals(mockResult, result)
        verify(exactly = 1) { scrapingService.scrapeAllGames() }
    }
    
    @Test
    fun `executeManualScraping should propagate exception`() {
        // Given
        val exception = RuntimeException("Manual scraping error")
        every { scrapingService.scrapeAllGames() } throws exception
        
        // When & Then
        try {
            schedulingService.executeManualScraping()
            assert(false) { "Expected exception to be thrown" }
        } catch (e: RuntimeException) {
            assertEquals("Manual scraping error", e.message)
        }
        
        verify(exactly = 1) { scrapingService.scrapeAllGames() }
    }
    
    @Test
    fun `getSchedulingInfo should return correct scheduling information`() {
        // When
        val info = schedulingService.getSchedulingInfo()
        
        // Then
        assertEquals(1800000L, info.intervalMs) // 30분 = 1,800,000ms
        assertEquals(30L, info.intervalMinutes) // 30분
        assertTrue(info.isEnabled)
        
        // 다음 실행 시간이 현재 시간보다 미래여야 함
        assertTrue(info.nextExecutionEstimate.isAfter(LocalDateTime.now()))
    }
    
    @Test
    fun `executeScrapingJob should handle mixed success and failure results`() {
        // Given
        val mockResult = ScrapingResult(
            startTime = LocalDateTime.now().minusMinutes(1),
            endTime = LocalDateTime.now(),
            totalGames = 3,
            totalNewNotices = 2,
            gameResults = mapOf(
                "NIKKE" to GameScrapingResult(
                    gameName = "NIKKE",
                    startTime = LocalDateTime.now().minusMinutes(1),
                    endTime = LocalDateTime.now(),
                    scrapedNoticesCount = 5,
                    newNoticesCount = 2,
                    success = true
                ),
                "원신" to GameScrapingResult(
                    gameName = "원신",
                    startTime = LocalDateTime.now().minusMinutes(1),
                    endTime = LocalDateTime.now(),
                    scrapedNoticesCount = 0,
                    newNoticesCount = 0,
                    success = false,
                    errorMessage = "Site unavailable"
                ),
                "마비노기 모바일" to GameScrapingResult(
                    gameName = "마비노기 모바일",
                    startTime = LocalDateTime.now().minusMinutes(1),
                    endTime = LocalDateTime.now(),
                    scrapedNoticesCount = 3,
                    newNoticesCount = 0,
                    success = true
                )
            ),
            success = true // 전체적으로는 성공
        )
        
        every { scrapingService.scrapeAllGames() } returns mockResult
        
        // When
        schedulingService.executeScrapingJob()
        
        // Then
        verify(exactly = 1) { scrapingService.scrapeAllGames() }
        // 로그에서 성공한 게임과 실패한 게임이 모두 기록되어야 함
    }
    
    @Test
    fun `executeScrapingJob should handle zero new notices`() {
        // Given
        val mockResult = ScrapingResult(
            startTime = LocalDateTime.now().minusMinutes(1),
            endTime = LocalDateTime.now(),
            totalGames = 2,
            totalNewNotices = 0, // 새로운 공지사항 없음
            gameResults = mapOf(
                "NIKKE" to GameScrapingResult(
                    gameName = "NIKKE",
                    startTime = LocalDateTime.now().minusMinutes(1),
                    endTime = LocalDateTime.now(),
                    scrapedNoticesCount = 5,
                    newNoticesCount = 0, // 모두 중복
                    success = true
                )
            ),
            success = true
        )
        
        every { scrapingService.scrapeAllGames() } returns mockResult
        
        // When
        schedulingService.executeScrapingJob()
        
        // Then
        verify(exactly = 1) { scrapingService.scrapeAllGames() }
        // 이메일 트리거 로그가 출력되지 않아야 함
    }

    @Test
    fun `getSchedulingStatus should reflect manual run metrics`() {
        val start = LocalDateTime.now().minusMinutes(2)
        val end = LocalDateTime.now().minusMinutes(1)
        val mockResult = ScrapingResult(
            startTime = start,
            endTime = end,
            totalGames = 1,
            totalNewNotices = 2,
            gameResults = emptyMap(),
            success = true
        )

        every { scrapingService.scrapeAllGames() } returns mockResult

        schedulingService.executeManualScraping()

        val status = schedulingService.getSchedulingStatus()
        assertEquals(RunTrigger.MANUAL, status.metrics.lastRunTriggeredBy)
        assertEquals(true, status.metrics.lastRunSuccess)
        assertEquals(2, status.metrics.lastRunTotalNewNotices)
        assertEquals(1, status.metrics.lastRunTotalGames)
    }

    @Test
    fun `getSchedulingStatus should report failure after exception`() {
        every { scrapingService.scrapeAllGames() } throws RuntimeException("boom")

        schedulingService.executeScrapingJob()

        val status = schedulingService.getSchedulingStatus()
        assertEquals(RunTrigger.SCHEDULED, status.metrics.lastRunTriggeredBy)
        assertEquals(false, status.metrics.lastRunSuccess)
    }

}
