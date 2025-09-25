package com.kiki.repository

import com.kiki.entity.Game
import com.kiki.entity.GameNotice
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

@DataJpaTest
@ActiveProfiles("test")
class GameNoticeRepositoryTest {

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var gameNoticeRepository: GameNoticeRepository

    private lateinit var testGame: Game
    private lateinit var recentNotice: GameNotice
    private lateinit var oldNotice: GameNotice
    private lateinit var unsentNotice: GameNotice

    @BeforeEach
    fun setUp() {
        testGame = Game(
            name = "NIKKE",
            baseUrl = "https://nikke-kr.com",
            scraperClass = "com.kiki.scraper.NikkeScraper",
            isActive = true
        )
        entityManager.persistAndFlush(testGame)

        val now = LocalDateTime.now()
        
        recentNotice = GameNotice(
            game = testGame,
            title = "Recent Update",
            url = "https://nikke-kr.com/recent",
            summary = "Recent update summary",
            publishedDate = now.minusHours(1),
            isSent = true
        )

        oldNotice = GameNotice(
            game = testGame,
            title = "Old Update",
            url = "https://nikke-kr.com/old",
            summary = "Old update summary",
            publishedDate = now.minusDays(5),
            isSent = true
        )

        unsentNotice = GameNotice(
            game = testGame,
            title = "Unsent Update",
            url = "https://nikke-kr.com/unsent",
            summary = "Unsent update summary",
            publishedDate = now.minusMinutes(30),
            isSent = false
        )

        entityManager.persistAndFlush(recentNotice)
        entityManager.persistAndFlush(oldNotice)
        entityManager.persistAndFlush(unsentNotice)
    }

    @Test
    fun `should find notices published after specific date`() {
        val twoDaysAgo = LocalDateTime.now().minusDays(2)
        val notices = gameNoticeRepository.findByPublishedDateAfter(twoDaysAgo)
        
        assertEquals(2, notices.size)
        assertTrue(notices.all { it.publishedDate.isAfter(twoDaysAgo) })
    }

    @Test
    fun `should find notices published after specific date ordered by date desc`() {
        val twoDaysAgo = LocalDateTime.now().minusDays(2)
        val notices = gameNoticeRepository.findByPublishedDateAfterOrderByPublishedDateDesc(twoDaysAgo)
        
        assertEquals(2, notices.size)
        assertTrue(notices[0].publishedDate.isAfter(notices[1].publishedDate))
    }

    @Test
    fun `should find unsent notices`() {
        val unsentNotices = gameNoticeRepository.findByIsSentFalse()
        
        assertEquals(1, unsentNotices.size)
        assertEquals("Unsent Update", unsentNotices[0].title)
        assertFalse(unsentNotices[0].isSent)
    }

    @Test
    fun `should find notices by game and published after date`() {
        val twoDaysAgo = LocalDateTime.now().minusDays(2)
        val notices = gameNoticeRepository.findByGameAndPublishedDateAfter(testGame, twoDaysAgo)
        
        assertEquals(2, notices.size)
        assertTrue(notices.all { it.game.id == testGame.id })
    }

    @Test
    fun `should check if notice exists by URL`() {
        assertTrue(gameNoticeRepository.existsByUrl("https://nikke-kr.com/recent"))
        assertFalse(gameNoticeRepository.existsByUrl("https://nikke-kr.com/nonexistent"))
    }

    @Test
    fun `should find notice by URL`() {
        val foundNotice = gameNoticeRepository.findByUrl("https://nikke-kr.com/recent")
        
        assertNotNull(foundNotice)
        assertEquals("Recent Update", foundNotice.title)
        
        val notFoundNotice = gameNoticeRepository.findByUrl("https://nikke-kr.com/nonexistent")
        assertNull(notFoundNotice)
    }

    @Test
    fun `should find recent notices with pagination`() {
        val thirtyDaysAgo = LocalDateTime.now().minusDays(30)
        val pageable = PageRequest.of(0, 10)
        val page = gameNoticeRepository.findRecentNotices(thirtyDaysAgo, pageable)
        
        assertEquals(3, page.totalElements)
        assertTrue(page.content.isNotEmpty())
    }

    @Test
    fun `should find unsent notices by game`() {
        val unsentNotices = gameNoticeRepository.findByGameAndIsSentFalse(testGame)
        
        assertEquals(1, unsentNotices.size)
        assertEquals("Unsent Update", unsentNotices[0].title)
    }

    @Test
    fun `should count notices published after specific date`() {
        val twoDaysAgo = LocalDateTime.now().minusDays(2)
        val count = gameNoticeRepository.countByPublishedDateAfter(twoDaysAgo)
        
        assertEquals(2L, count)
    }
}