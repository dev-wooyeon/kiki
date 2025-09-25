package com.kiki.scraper

import com.kiki.entity.Game
import com.kiki.repository.GameRepository
import com.kiki.scraper.client.WutheringWavesClient
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime

class WutheringWavesScraperTest {

    private val client: WutheringWavesClient = mockk()
    private val gameRepository: GameRepository = mockk()
    private val parsingUtil = ParsingUtil()
    private val httpClientUtil = HttpClientUtil()

    private lateinit var scraper: WutheringWavesScraper

    @BeforeEach
    fun setUp() {
        scraper = WutheringWavesScraper(client)
        ReflectionTestUtils.setField(scraper, "parsingUtil", parsingUtil)
        ReflectionTestUtils.setField(scraper, "httpClientUtil", httpClientUtil)
        ReflectionTestUtils.setField(scraper, "gameRepository", gameRepository)
    }

    @Test
    fun `scrapeNotices should map JSON articles to GameNotice`() {
        val game = Game(
            id = 1L,
            name = "명조",
            baseUrl = "https://wutheringwaves.kurogames.com",
            scraperClass = WutheringWavesScraper::class.qualifiedName ?: "WutheringWavesScraper",
            isActive = true
        )

        every { gameRepository.findByName("명조") } returns game

        every { client.fetchArticleTypes() } returns listOf(
            WutheringWavesClient.ArticleTypeDto(contentId = 65, contentLabel = "공지"),
            WutheringWavesClient.ArticleTypeDto(contentId = 66, contentLabel = "뉴스")
        )

        every { client.fetchArticles() } returns listOf(
            WutheringWavesClient.ArticleMenuItemDto(
                articleId = 1001,
                articleTitle = "정기 점검 안내",
                articleType = 65,
                startTime = "2025-01-10 09:30:00",
                articleDesc = "서버 점검이 진행됩니다.",
                top = 1
            ),
            // Duplicate entry with type 0 should be ignored
            WutheringWavesClient.ArticleMenuItemDto(
                articleId = 1001,
                articleTitle = "정기 점검 안내",
                articleType = 0,
                startTime = "2025-01-10 09:30:00",
                articleContent = "<div>서버 점검 내용</div>",
                top = 1
            ),
            WutheringWavesClient.ArticleMenuItemDto(
                articleId = 1002,
                articleTitle = "신규 이벤트",
                articleType = 66,
                startTime = "2025-01-09 08:00:00",
                articleContent = "<p>새로운 이벤트가 시작됩니다.</p>"
            )
        )

        val notices = scraper.scrapeNotices()

        assertEquals(2, notices.size)

        val notice1 = notices.first()
        assertEquals("정기 점검 안내", notice1.title)
        assertEquals("https://wutheringwaves.kurogames.com/kr/news/detail/1001", notice1.url)
        assertEquals(LocalDateTime.of(2025, 1, 10, 9, 30), notice1.publishedDate)
        assertTrue(notice1.summary?.contains("공지") == true)
        assertEquals(game, notice1.game)

        val notice2 = notices.last()
        assertEquals("신규 이벤트", notice2.title)
        assertEquals("https://wutheringwaves.kurogames.com/kr/news/detail/1002", notice2.url)
        assertEquals(LocalDateTime.of(2025, 1, 9, 8, 0), notice2.publishedDate)
        assertTrue(notice2.summary?.contains("뉴스") == true)
    }
}
