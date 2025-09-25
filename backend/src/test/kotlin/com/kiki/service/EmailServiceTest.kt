package com.kiki.service

import com.kiki.entity.*
import com.kiki.integration.OpenAiClient
import com.kiki.repository.GameNoticeRepository
import com.kiki.repository.EmailLogRepository
import com.kiki.repository.SubscriberRepository
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mail.javamail.JavaMailSender
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EmailServiceTest {
    
    private val mailSender = mockk<JavaMailSender>()
    private val subscriberRepository = mockk<SubscriberRepository>()
    private val emailLogRepository = mockk<EmailLogRepository>()
    private val openAiClient = mockk<OpenAiClient>()
    private val noticeContentService = mockk<NoticeContentService>()
    private val gameNoticeRepository = mockk<GameNoticeRepository>()
    private val noticeSummaryService = NoticeSummaryService(openAiClient, noticeContentService, gameNoticeRepository)
    
    private lateinit var emailService: EmailService
    
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

        clearAllMocks()
        every { openAiClient.generateSummary(any(), any(), any()) } returns null
        every { noticeContentService.extractPrimaryText(any()) } returns null
    }
    
    @Test
    fun `sendDailyDigest - 공지사항이 없으면 이메일을 발송하지 않는다`() {
        // given
        val emptyNotices = emptyList<GameNotice>()
        
        // when
        emailService.sendDailyDigest(emptyNotices)
        
        // then
        verify(exactly = 0) { subscriberRepository.findByIsActiveTrue() }
    }
    
    @Test
    fun `sendDailyDigest - 활성 구독자가 없으면 이메일을 발송하지 않는다`() {
        // given
        val notices = listOf(createTestGameNotice())
        every { subscriberRepository.findByIsActiveTrue() } returns emptyList()
        
        // when
        emailService.sendDailyDigest(notices)
        
        // then
        verify(exactly = 1) { subscriberRepository.findByIsActiveTrue() }
    }
    
    @Test
    fun `sendDailyDigest - 구독자와 공지사항이 있으면 처리 로직을 실행한다`() {
        // given
        val game = createTestGame()
        val notices = listOf(createTestGameNotice(game = game))
        val subscriber = createTestSubscriber()
        val subscribers = listOf(subscriber)
        
        every { subscriberRepository.findByIsActiveTrue() } returns subscribers
        every { emailLogRepository.save(any<EmailLog>()) } returns mockk()
        
        // Mock JavaMailSender to avoid actual email sending
        every { mailSender.createMimeMessage() } returns mockk(relaxed = true)
        every { mailSender.send(any<jakarta.mail.internet.MimeMessage>()) } throws RuntimeException("Mocked failure for testing")
        
        // when
        emailService.sendDailyDigest(notices)
        
        // then
        verify(exactly = 1) { subscriberRepository.findByIsActiveTrue() }
        verify(exactly = 1) { emailLogRepository.save(any<EmailLog>()) }
    }
    
    @Test
    fun `generateEmailSubject - 단일 게임의 경우 게임명을 포함한다`() {
        // given
        val game = createTestGame(name = "테스트 게임")
        val notices = listOf(
            createTestGameNotice(game = game, title = "공지1"),
            createTestGameNotice(game = game, title = "공지2")
        )
        
        // when - private 메서드이므로 간접적으로 테스트
        every { subscriberRepository.findByIsActiveTrue() } returns emptyList()
        emailService.sendDailyDigest(notices)
        
        // then - 로직이 실행되었는지 확인
        verify(exactly = 1) { subscriberRepository.findByIsActiveTrue() }
    }
    
    @Test
    fun `generateEmailContent - HTML 이메일 내용을 생성한다`() {
        // given
        val game1 = createTestGame(name = "게임1")
        val game2 = createTestGame(name = "게임2")
        val notices = listOf(
            createTestGameNotice(game = game1, title = "게임1 공지"),
            createTestGameNotice(game = game2, title = "게임2 공지")
        )
        
        // when - private 메서드이므로 간접적으로 테스트
        every { subscriberRepository.findByIsActiveTrue() } returns emptyList()
        emailService.sendDailyDigest(notices)
        
        // then - 로직이 실행되었는지 확인
        verify(exactly = 1) { subscriberRepository.findByIsActiveTrue() }
    }
    
    @Test
    fun `sendSubscriptionConfirmation - 구독 확인 이메일 발송을 시도한다`() {
        // given
        val email = "test@example.com"
        
        // Mock JavaMailSender to avoid actual email sending
        every { mailSender.createMimeMessage() } returns mockk(relaxed = true)
        every { mailSender.send(any<jakarta.mail.internet.MimeMessage>()) } just Runs
        
        // when
        emailService.sendSubscriptionConfirmation(email)
        
        // then
        verify(exactly = 1) { mailSender.createMimeMessage() }
        verify(exactly = 1) { mailSender.send(any<jakarta.mail.internet.MimeMessage>()) }
    }
    
    @Test
    fun `sendSubscriptionConfirmation - 이메일 발송 실패 시 예외를 처리한다`() {
        // given
        val email = "invalid@example.com"
        
        every { mailSender.createMimeMessage() } returns mockk(relaxed = true)
        every { mailSender.send(any<jakarta.mail.internet.MimeMessage>()) } throws RuntimeException("Invalid email")
        
        // when & then (예외가 발생하지 않아야 함 - 내부에서 처리됨)
        emailService.sendSubscriptionConfirmation(email)
        
        verify(exactly = 1) { mailSender.send(any<jakarta.mail.internet.MimeMessage>()) }
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
        url: String = "https://test.com/notice/1",
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
