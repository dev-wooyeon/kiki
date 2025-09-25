package com.kiki.service

import com.kiki.entity.Game
import com.kiki.entity.GameNotice
import com.kiki.repository.GameRepository
import com.kiki.repository.GameNoticeRepository
import com.kiki.scraper.GameScraper
import com.kiki.scraper.ScrapingException
import com.kiki.scraper.service.ScrapingService
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
class ScrapingServiceTest {
    
    private lateinit var gameRepository: GameRepository
    private lateinit var gameNoticeRepository: GameNoticeRepository
    private lateinit var mockScraper1: GameScraper
    private lateinit var mockScraper2: GameScraper
    private lateinit var scrapingService: ScrapingService
    
    private lateinit var testGame1: Game
    private lateinit var testGame2: Game
    private lateinit var testNotice1: GameNotice
    private lateinit var testNotice2: GameNotice
    
    @BeforeEach
    fun setUp() {
        gameRepository = mockk()
        gameNoticeRepository = mockk()
        mockScraper1 = mockk()
        mockScraper2 = mockk()
        
        scrapingService = ScrapingService(
            gameRepository = gameRepository,
            gameNoticeRepository = gameNoticeRepository,
            scrapers = listOf(mockScraper1, mockScraper2)
        )
        
        // 테스트 데이터 설정
        testGame1 = Game(
            id = 1L,
            name = "NIKKE",
            baseUrl = "https://nikke-kr.com",
            scraperClass = "NikkeScraper",
            isActive = true
        )
        
        testGame2 = Game(
            id = 2L,
            name = "원신",
            baseUrl = "https://genshin.hoyoverse.com",
            scraperClass = "GenshinScraper",
            isActive = true
        )
        
        testNotice1 = GameNotice(
            id = 1L,
            game = testGame1,
            title = "Test Notice 1",
            url = "https://nikke-kr.com/notice/1",
            summary = "Test summary 1",
            publishedDate = LocalDateTime.now().minusHours(1),
            isSent = false
        )
        
        testNotice2 = GameNotice(
            id = 2L,
            game = testGame2,
            title = "Test Notice 2",
            url = "https://genshin.hoyoverse.com/notice/2",
            summary = "Test summary 2",
            publishedDate = LocalDateTime.now().minusHours(2),
            isSent = false
        )
    }
    
    @Test
    fun `scrapeAllGames should successfully scrape all active games`() {
        // Given
        every { gameRepository.findByIsActiveTrue() } returns listOf(testGame1, testGame2)
        every { mockScraper1.getGameName() } returns "NIKKE"
        every { mockScraper2.getGameName() } returns "원신"
        every { mockScraper1.scrapeNotices() } returns listOf(testNotice1)
        every { mockScraper2.scrapeNotices() } returns listOf(testNotice2)
        every { gameNoticeRepository.existsByUrl(any()) } returns false
        every { gameNoticeRepository.save(any<GameNotice>()) } returnsArgument 0
        
        // When
        val result = scrapingService.scrapeAllGames()
        
        // Then
        assertTrue(result.success)
        assertEquals(2, result.totalGames)
        assertEquals(2, result.totalNewNotices)
        assertEquals(2, result.gameResults.size)
        
        verify { mockScraper1.scrapeNotices() }
        verify { mockScraper2.scrapeNotices() }
        verify(exactly = 2) { gameNoticeRepository.save(any<GameNotice>()) }
    }
    
    @Test
    fun `scrapeAllGames should handle no active games`() {
        // Given
        every { gameRepository.findByIsActiveTrue() } returns emptyList()
        
        // When
        val result = scrapingService.scrapeAllGames()
        
        // Then
        assertTrue(result.success)
        assertEquals(0, result.totalGames)
        assertEquals(0, result.totalNewNotices)
        assertTrue(result.gameResults.isEmpty())
        
        verify { gameRepository.findByIsActiveTrue() }
        verify(exactly = 0) { gameNoticeRepository.save(any<GameNotice>()) }
    }
    
    @Test
    fun `scrapeAllGames should handle scraping exception for individual game`() {
        // Given
        every { gameRepository.findByIsActiveTrue() } returns listOf(testGame1, testGame2)
        every { mockScraper1.getGameName() } returns "NIKKE"
        every { mockScraper2.getGameName() } returns "원신"
        every { mockScraper1.scrapeNotices() } throws ScrapingException("Network error")
        every { mockScraper2.scrapeNotices() } returns listOf(testNotice2)
        every { gameNoticeRepository.existsByUrl(testNotice2.url) } returns false
        every { gameNoticeRepository.save(testNotice2) } returns testNotice2
        
        // When
        val result = scrapingService.scrapeAllGames()
        
        // Then
        assertTrue(result.success) // 전체 프로세스는 성공 (일부 게임 실패해도)
        assertEquals(2, result.totalGames)
        assertEquals(1, result.totalNewNotices) // 하나의 게임만 성공
        
        val nikkeResult = result.gameResults["NIKKE"]!!
        assertFalse(nikkeResult.success)
        assertEquals("Network error", nikkeResult.errorMessage)
        
        val genshinResult = result.gameResults["원신"]!!
        assertTrue(genshinResult.success)
        assertEquals(1, genshinResult.newNoticesCount)
    }
    
    @Test
    fun `filterAndSaveNewNotices should skip duplicate notices`() {
        // Given
        every { gameRepository.findByIsActiveTrue() } returns listOf(testGame1)
        every { mockScraper1.getGameName() } returns "NIKKE"
        every { mockScraper1.scrapeNotices() } returns listOf(testNotice1)
        every { gameNoticeRepository.existsByUrl(testNotice1.url) } returns true // 이미 존재
        
        // When
        val result = scrapingService.scrapeAllGames()
        
        // Then
        assertTrue(result.success)
        assertEquals(0, result.totalNewNotices) // 중복이므로 새로운 공지사항 없음
        
        verify { gameNoticeRepository.existsByUrl(testNotice1.url) }
        verify(exactly = 0) { gameNoticeRepository.save(any<GameNotice>()) }
    }
    
    @Test
    fun `scrapeAllGames should handle missing scraper for game`() {
        // Given
        val unknownGame = Game(
            id = 3L,
            name = "Unknown Game",
            baseUrl = "https://unknown.com",
            scraperClass = "UnknownScraper",
            isActive = true
        )
        
        every { gameRepository.findByIsActiveTrue() } returns listOf(unknownGame)
        every { mockScraper1.getGameName() } returns "NIKKE"
        every { mockScraper2.getGameName() } returns "원신"
        
        // When
        val result = scrapingService.scrapeAllGames()
        
        // Then
        assertTrue(result.success)
        assertEquals(1, result.totalGames)
        assertEquals(0, result.totalNewNotices)
        
        val unknownGameResult = result.gameResults["Unknown Game"]!!
        assertFalse(unknownGameResult.success)
        assertTrue(unknownGameResult.errorMessage!!.contains("No scraper found"))
    }
    
    @Test
    fun `getUnsentNotices should return unsent notices`() {
        // Given
        val unsentNotices = listOf(testNotice1, testNotice2)
        every { gameNoticeRepository.findByIsSentFalse() } returns unsentNotices
        
        // When
        val result = scrapingService.getUnsentNotices()
        
        // Then
        assertEquals(2, result.size)
        assertEquals(unsentNotices, result)
        
        verify { gameNoticeRepository.findByIsSentFalse() }
    }
    
    @Test
    fun `markNoticesAsSent should update notices as sent`() {
        // Given
        val notices = listOf(testNotice1, testNotice2)
        every { gameNoticeRepository.save(any<GameNotice>()) } returnsArgument 0
        
        // When
        scrapingService.markNoticesAsSent(notices)
        
        // Then
        verify(exactly = 2) { gameNoticeRepository.save(any<GameNotice>()) }
    }
    
    @Test
    fun `markNoticesAsSent should handle empty list`() {
        // Given
        val emptyNotices = emptyList<GameNotice>()
        
        // When
        scrapingService.markNoticesAsSent(emptyNotices)
        
        // Then
        verify(exactly = 0) { gameNoticeRepository.save(any<GameNotice>()) }
    }
    
    @Test
    fun `scrapeAllGames should handle database save error gracefully`() {
        // Given
        every { gameRepository.findByIsActiveTrue() } returns listOf(testGame1)
        every { mockScraper1.getGameName() } returns "NIKKE"
        every { mockScraper1.scrapeNotices() } returns listOf(testNotice1)
        every { gameNoticeRepository.existsByUrl(testNotice1.url) } returns false
        every { gameNoticeRepository.save(testNotice1) } throws RuntimeException("Database error")
        
        // When
        val result = scrapingService.scrapeAllGames()
        
        // Then
        assertTrue(result.success) // 전체 프로세스는 성공으로 처리
        assertEquals(0, result.totalNewNotices) // 저장 실패로 새로운 공지사항 0개
        
        val gameResult = result.gameResults["NIKKE"]!!
        assertTrue(gameResult.success) // 스크래핑 자체는 성공
        assertEquals(0, gameResult.newNoticesCount) // 저장 실패로 0개
    }
}
