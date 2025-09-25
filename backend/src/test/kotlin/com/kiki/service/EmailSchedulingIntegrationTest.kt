package com.kiki.service

import com.kiki.entity.*
import com.kiki.integration.OpenAiClient
import com.kiki.repository.*
import com.kiki.scraper.service.ScrapingService
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mail.javamail.JavaMailSender
import java.time.LocalDateTime
import kotlin.test.assertEquals

class EmailSchedulingIntegrationTest {
    
    private val mailSender = mockk<JavaMailSender>()
    private val subscriberRepository = mockk<SubscriberRepository>()
    private val emailLogRepository = mockk<EmailLogRepository>()
    private val gameRepository = mockk<GameRepository>()
    private val gameNoticeRepository = mockk<GameNoticeRepository>()
    private val openAiClient = mockk<OpenAiClient>()
    private val scrapers = emptyList<com.kiki.scraper.GameScraper>()
    private val noticeContentService = mockk<NoticeContentService>()
    private val noticeSummaryService = NoticeSummaryService(openAiClient, noticeContentService, gameNoticeRepository)
    
    private lateinit var emailService: EmailService
    private lateinit var scrapingService: ScrapingService
    private lateinit var schedulingService: SchedulingService
    
    @BeforeEach
    fun setUp() {
        emailService = EmailService(
            mailSender = mailSender,
            subscriberRepository = subscriberRepository,
            emailLogRepository = emailLogRepository,
            noticeSummaryService = noticeSummaryService,
            fromEmail = "test@kiki.com",
            serverPort = "8080",
            frontendUrl = "http://localhost:3000"
        )
        
        scrapingService = ScrapingService(
            gameRepository = gameRepository,
            gameNoticeRepository = gameNoticeRepository,
            scrapers = scrapers
        )
        
        schedulingService = SchedulingService(
            scrapingService = scrapingService,
            emailService = emailService,
            gameNoticeRepository = gameNoticeRepository
        )
        
        clearAllMocks()
        every { openAiClient.generateSummary(any(), any(), any()) } returns null
        every { noticeContentService.extractPrimaryText(any()) } returns null
    }
    
    @Test
    fun `executeManualEmailNotification - 발송되지 않은 공지사항이 있으면 이메일을 발송한다`() {
        // given
        val game = createTestGame()
        val notices = listOf(
            createTestGameNotice(game = game, title = "공지1"),
            createTestGameNotice(game = game, title = "공지2")
        )
        val subscriber = createTestSubscriber()
        
        every { gameNoticeRepository.findByIsSentFalse() } returns notices
        every { subscriberRepository.findByIsActiveTrue() } returns listOf(subscriber)
        every { mailSender.createMimeMessage() } returns mockk(relaxed = true)
        every { mailSender.send(any<jakarta.mail.internet.MimeMessage>()) } just Runs
        every { emailLogRepository.save(any<EmailLog>()) } returns mockk()
        every { gameNoticeRepository.save(any<GameNotice>()) } returns mockk()
        
        // when
        val result = schedulingService.executeManualEmailNotification()
        
        // then
        assertEquals(2, result)
        verify(exactly = 1) { gameNoticeRepository.findByIsSentFalse() }
        verify(exactly = 1) { subscriberRepository.findByIsActiveTrue() }
        verify(exactly = 1) { mailSender.send(any<jakarta.mail.internet.MimeMessage>()) }
        verify(exactly = 2) { gameNoticeRepository.save(any<GameNotice>()) } // 2개 공지사항 업데이트
    }
    
    @Test
    fun `executeManualEmailNotification - 발송되지 않은 공지사항이 없으면 0을 반환한다`() {
        // given
        every { gameNoticeRepository.findByIsSentFalse() } returns emptyList()
        
        // when
        val result = schedulingService.executeManualEmailNotification()
        
        // then
        assertEquals(0, result)
        verify(exactly = 1) { gameNoticeRepository.findByIsSentFalse() }
        verify(exactly = 0) { subscriberRepository.findByIsActiveTrue() }
        verify(exactly = 0) { mailSender.send(any<jakarta.mail.internet.MimeMessage>()) }
    }
    
    @Test
    fun `executeManualEmailNotification - 활성 구독자가 없으면 이메일을 발송하지 않는다`() {
        // given
        val game = createTestGame()
        val notices = listOf(createTestGameNotice(game = game))
        
        every { gameNoticeRepository.findByIsSentFalse() } returns notices
        every { subscriberRepository.findByIsActiveTrue() } returns emptyList()
        every { gameNoticeRepository.save(any<GameNotice>()) } returns mockk()
        
        // when
        val result = schedulingService.executeManualEmailNotification()
        
        // then
        assertEquals(1, result) // 공지사항은 처리되지만 이메일은 발송되지 않음
        verify(exactly = 1) { gameNoticeRepository.findByIsSentFalse() }
        verify(exactly = 1) { subscriberRepository.findByIsActiveTrue() }
        verify(exactly = 0) { mailSender.send(any<jakarta.mail.internet.MimeMessage>()) }
        verify(exactly = 1) { gameNoticeRepository.save(any<GameNotice>()) } // 공지사항은 여전히 sent로 표시
    }
    
    @Test
    fun `getSchedulingInfo - 스케줄링 정보를 반환한다`() {
        // when
        val info = schedulingService.getSchedulingInfo()
        
        // then
        assertEquals(1800000L, info.intervalMs) // 30분
        assertEquals(30L, info.intervalMinutes)
        assertEquals(true, info.isEnabled)
    }
    
    private fun createTestGame(
        id: Long = 1L,
        name: String = "테스트 게임",
        baseUrl: String = "https://test.com",
        scraperClass: String = "TestScraper"
    ): Game {
        return Game(
            id = id,
            name = name,
            baseUrl = baseUrl,
            scraperClass = scraperClass,
            isActive = true,
            createdAt = LocalDateTime.now()
        )
    }
    
    private fun createTestGameNotice(
        id: Long = 1L,
        game: Game = createTestGame(),
        title: String = "테스트 공지사항",
        url: String = "https://test.com/notice/${System.currentTimeMillis()}",
        summary: String = "테스트 공지사항 요약입니다.",
        publishedDate: LocalDateTime = LocalDateTime.now()
    ): GameNotice {
        return GameNotice(
            id = id,
            game = game,
            title = title,
            url = url,
            summary = summary,
            publishedDate = publishedDate,
            scrapedAt = LocalDateTime.now(),
            isSent = false
        )
    }
    
    private fun createTestSubscriber(
        id: Long = 1L,
        email: String = "test@example.com",
        unsubscribeToken: String = "test-token-123"
    ): Subscriber {
        return Subscriber(
            id = id,
            email = email,
            unsubscribeToken = unsubscribeToken,
            isActive = true,
            subscribedAt = LocalDateTime.now()
        )
    }
}
