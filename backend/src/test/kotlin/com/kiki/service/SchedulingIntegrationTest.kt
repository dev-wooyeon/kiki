package com.kiki.service

import com.kiki.entity.Game
import com.kiki.entity.GameNotice
import com.kiki.repository.GameRepository
import com.kiki.repository.GameNoticeRepository
import com.kiki.scraper.GameScraper
import com.kiki.scraper.service.ScrapingService
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SchedulingIntegrationTest {
    
    @Autowired
    private lateinit var schedulingService: SchedulingService
    
    @Autowired
    private lateinit var scrapingService: ScrapingService
    
    @Autowired
    private lateinit var gameRepository: GameRepository
    
    @Autowired
    private lateinit var gameNoticeRepository: GameNoticeRepository
    
    @Autowired
    private lateinit var gameScraper: GameScraper
    
    @TestConfiguration
    class TestConfig {
        
        @Bean
        @Primary
        fun mockGameScraper(): GameScraper {
            return mockk<GameScraper>().apply {
                every { getGameName() } returns "Test Game"
                every { getBaseUrl() } returns "https://test.com"
                every { getNoticeUrl() } returns "https://test.com/notices"
                every { scrapeNotices() } returns listOf(
                    GameNotice(
                        game = Game(
                            name = "Test Game",
                            baseUrl = "https://test.com",
                            scraperClass = "TestScraper"
                        ),
                        title = "Test Notice",
                        url = "https://test.com/notice/1",
                        summary = "Test summary",
                        publishedDate = LocalDateTime.now()
                    )
                )
            }
        }
    }
    
    @BeforeEach
    fun setUp() {
        // 테스트 데이터 정리
        gameNoticeRepository.deleteAll()
        gameRepository.deleteAll()
        
        // 테스트 게임 생성
        val testGame = Game(
            name = "Test Game",
            baseUrl = "https://test.com",
            scraperClass = "TestScraper",
            isActive = true
        )
        gameRepository.save(testGame)

        val persistedGame = gameRepository.findByName("Test Game")!!
        every { gameScraper.scrapeNotices() } returns listOf(
            GameNotice(
                game = persistedGame,
                title = "Test Notice",
                url = "https://test.com/notice/1",
                summary = "Test summary",
                publishedDate = LocalDateTime.now()
            )
        )
    }
    
    @Test
    fun `scheduling service integration test - full workflow`() {
        // Given
        val initialNoticeCount = gameNoticeRepository.count()
        
        // When - 수동 스크래핑 실행
        val result = schedulingService.executeManualScraping()
        
        // Then
        assertTrue(result.success)
        assertEquals(1, result.totalGames)
        
        // 데이터베이스에 새로운 공지사항이 저장되었는지 확인
        val finalNoticeCount = gameNoticeRepository.count()
        assertTrue(finalNoticeCount > initialNoticeCount)
        
        // 저장된 공지사항 확인
        val savedNotices = gameNoticeRepository.findAll()
        assertTrue(savedNotices.isNotEmpty())
        
        val savedNotice = savedNotices.first()
        assertEquals("Test Notice", savedNotice.title)
        assertEquals("https://test.com/notice/1", savedNotice.url)
        assertEquals(false, savedNotice.isSent)
    }
    
    @Test
    fun `scheduling service should handle duplicate notices correctly`() {
        // Given - 첫 번째 스크래핑으로 공지사항 저장
        schedulingService.executeManualScraping()
        val firstCount = gameNoticeRepository.count()
        
        // When - 동일한 공지사항으로 두 번째 스크래핑
        val result = schedulingService.executeManualScraping()
        
        // Then - 중복 공지사항은 저장되지 않아야 함
        val secondCount = gameNoticeRepository.count()
        assertEquals(firstCount, secondCount)
        assertEquals(0, result.totalNewNotices) // 새로운 공지사항 없음
    }
    
    @Test
    fun `getSchedulingInfo should return correct information`() {
        // When
        val info = schedulingService.getSchedulingInfo()
        
        // Then
        assertEquals(1800000L, info.intervalMs) // 30분
        assertEquals(30L, info.intervalMinutes)
        assertTrue(info.isEnabled)
        assertTrue(info.nextExecutionEstimate.isAfter(LocalDateTime.now()))
    }
    
    @Test
    fun `scraping service should handle inactive games`() {
        // Given - 게임을 비활성화
        val game = gameRepository.findByName("Test Game")!!
        val inactiveGame = game.copy(isActive = false)
        gameRepository.save(inactiveGame)
        
        // When
        val result = schedulingService.executeManualScraping()
        
        // Then - 비활성화된 게임은 스크래핑되지 않아야 함
        assertTrue(result.success)
        assertEquals(0, result.totalGames)
        assertEquals(0, result.totalNewNotices)
    }
    
    @Test
    fun `unsent notices workflow test`() {
        // Given - 스크래핑으로 공지사항 생성
        schedulingService.executeManualScraping()
        
        // When - 발송되지 않은 공지사항 조회
        val unsentNotices = scrapingService.getUnsentNotices()
        
        // Then
        assertTrue(unsentNotices.isNotEmpty())
        assertTrue(unsentNotices.all { !it.isSent })
        
        // When - 공지사항을 발송 완료로 표시
        scrapingService.markNoticesAsSent(unsentNotices)
        
        // Then - 발송되지 않은 공지사항이 없어야 함
        val remainingUnsentNotices = scrapingService.getUnsentNotices()
        assertTrue(remainingUnsentNotices.isEmpty())
    }
}
