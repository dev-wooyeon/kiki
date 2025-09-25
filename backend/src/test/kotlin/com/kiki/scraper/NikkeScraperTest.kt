package com.kiki.scraper

import com.kiki.entity.Game
import com.kiki.scraper.client.NikkeCmsClient
import com.kiki.scraper.client.NikkeCmsClient.NikkeNotice
import io.mockk.every
import io.mockk.mockk
import org.springframework.test.util.ReflectionTestUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class NikkeScraperTest {

    private val nikkeCmsClient: NikkeCmsClient = mockk()
    private val gameRepository: com.kiki.repository.GameRepository = mockk()
    private val parsingUtil = ParsingUtil()
    private val httpClientUtil = HttpClientUtil()

    private lateinit var scraper: NikkeScraper

    @BeforeEach
    fun setUp() {
        scraper = NikkeScraper(nikkeCmsClient)
        ReflectionTestUtils.setField(scraper, "parsingUtil", parsingUtil)
        ReflectionTestUtils.setField(scraper, "httpClientUtil", httpClientUtil)
        ReflectionTestUtils.setField(scraper, "gameRepository", gameRepository)
    }

    @Test
    fun `scrapeNotices should map CMS notices to GameNotice`() {
        val game = Game(
            id = 1,
            name = "승리의 여신 니케",
            baseUrl = "https://nikke-kr.com",
            scraperClass = NikkeScraper::class.qualifiedName ?: "NikkeScraper"
        )

        every { gameRepository.findByName("승리의 여신 니케") } returns game

        val publishedAt = LocalDateTime.of(2024, 9, 24, 9, 0)
        every { nikkeCmsClient.fetchLatestNotices() } returns listOf(
            NikkeNotice(
                contentId = "abc123",
                title = "업데이트 공지",
                summary = "새로운 업데이트가 적용되었습니다.",
                publishedAt = publishedAt,
                url = "https://nikke-kr.com/newsdetail.html?content_id=abc123"
            )
        )

        val notices = scraper.scrapeNotices()

        assertEquals(1, notices.size)
        val notice = notices.first()

        assertEquals("업데이트 공지", notice.title)
        assertEquals("https://nikke-kr.com/newsdetail.html?content_id=abc123", notice.url)
        assertEquals(publishedAt, notice.publishedDate)
        assertEquals("새로운 업데이트가 적용되었습니다.", notice.summary)
        assertEquals(game, notice.game)
        assertTrue(notice.scrapedAt.isAfter(publishedAt.minusSeconds(1)))
        assertTrue(notice.scrapedAt.isBefore(LocalDateTime.now().plusSeconds(1)))
    }
}
