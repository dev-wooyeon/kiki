package com.kiki.service

import com.kiki.entity.Game
import com.kiki.entity.GameNotice
import com.kiki.integration.OpenAiClient
import com.kiki.repository.GameNoticeRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class NoticeSummaryServiceTest {

    private val openAiClient = mockk<OpenAiClient>()
    private val noticeContentService = mockk<NoticeContentService>()
    private val gameNoticeRepository = mockk<GameNoticeRepository>()
    private lateinit var noticeSummaryService: NoticeSummaryService

    @BeforeEach
    fun setUp() {
        every { openAiClient.generateSummary(any(), any(), any()) } returns null
        every { noticeContentService.extractPrimaryText(any()) } returns null
        noticeSummaryService = NoticeSummaryService(openAiClient, noticeContentService, gameNoticeRepository)
    }

    @Test
    fun `generateDigestSummary groups notices by game and limits highlights`() {
        val gameA = Game(name = "Game A", baseUrl = "https://a.com", scraperClass = "ScraperA")
        val gameB = Game(name = "Game B", baseUrl = "https://b.com", scraperClass = "ScraperB")

        val notices = listOf(
            GameNotice(title = "Notice 1", url = "https://a.com/1", summary = "첫 번째 공지", game = gameA, publishedDate = LocalDateTime.now()),
            GameNotice(title = "Notice 2", url = "https://a.com/2", summary = "두 번째 공지", game = gameA, publishedDate = LocalDateTime.now().minusHours(1)),
            GameNotice(title = "Notice 3", url = "https://a.com/3", summary = "세 번째 공지", game = gameA, publishedDate = LocalDateTime.now().minusHours(2)),
            GameNotice(title = "Notice 4", url = "https://a.com/4", summary = "네 번째 공지", game = gameA, publishedDate = LocalDateTime.now().minusHours(3)),
            GameNotice(title = "Notice 5", url = "https://b.com/1", summary = "다른 게임 공지", game = gameB, publishedDate = LocalDateTime.now())
        )

        val summary = noticeSummaryService.generateDigestSummary(notices, highlightsPerGame = 2, summaryLength = 20)

        assertEquals(5, summary.totalCount)
        assertEquals(2, summary.gameSummaries.size)

        val gameASummary = summary.gameSummaries.first { it.gameName == "Game A" }
        assertEquals(4, gameASummary.totalCount)
        assertEquals(2, gameASummary.highlights.size)
        assertTrue(gameASummary.highlights.first().title.contains("Notice 1"))

        val formatted = noticeSummaryService.formatAsPlainText(summary)
        assertTrue(formatted.contains("Game A"))
        assertTrue(formatted.contains("Game B"))
    }

    @Test
    fun `generateDigestSummary handles empty list`() {
        val summary = noticeSummaryService.generateDigestSummary(emptyList())
        assertEquals(0, summary.totalCount)
        assertTrue(summary.gameSummaries.isEmpty())
        assertEquals("새로운 공지사항 요약이 없습니다.", noticeSummaryService.formatAsPlainText(summary))
    }
}
