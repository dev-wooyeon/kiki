package com.kiki.repository

import com.kiki.entity.EmailLog
import com.kiki.entity.EmailStatus
import com.kiki.entity.Subscriber
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
import kotlin.test.assertTrue

@DataJpaTest
@ActiveProfiles("test")
class EmailLogRepositoryTest {

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var emailLogRepository: EmailLogRepository

    private lateinit var subscriber1: Subscriber
    private lateinit var subscriber2: Subscriber
    private lateinit var successLog: EmailLog
    private lateinit var failedLog: EmailLog
    private lateinit var recentLog: EmailLog

    @BeforeEach
    fun setUp() {
        val now = LocalDateTime.now()
        
        subscriber1 = Subscriber(
            email = "subscriber1@example.com",
            unsubscribeToken = "token1",
            isActive = true
        )

        subscriber2 = Subscriber(
            email = "subscriber2@example.com",
            unsubscribeToken = "token2",
            isActive = true
        )

        entityManager.persistAndFlush(subscriber1)
        entityManager.persistAndFlush(subscriber2)

        successLog = EmailLog(
            subscriber = subscriber1,
            sentAt = now.minusDays(1),
            noticeCount = 3,
            status = EmailStatus.SUCCESS
        )

        failedLog = EmailLog(
            subscriber = subscriber1,
            sentAt = now.minusDays(2),
            noticeCount = 2,
            status = EmailStatus.FAILED,
            errorMessage = "SMTP connection failed"
        )

        recentLog = EmailLog(
            subscriber = subscriber2,
            sentAt = now.minusHours(1),
            noticeCount = 1,
            status = EmailStatus.SUCCESS
        )

        entityManager.persistAndFlush(successLog)
        entityManager.persistAndFlush(failedLog)
        entityManager.persistAndFlush(recentLog)
    }

    @Test
    fun `should find email logs by subscriber`() {
        val logs = emailLogRepository.findBySubscriber(subscriber1)
        
        assertEquals(2, logs.size)
        assertTrue(logs.all { it.subscriber.id == subscriber1.id })
    }

    @Test
    fun `should find email logs by subscriber with pagination`() {
        val pageable = PageRequest.of(0, 10)
        val page = emailLogRepository.findBySubscriberOrderBySentAtDesc(subscriber1, pageable)
        
        assertEquals(2, page.totalElements)
        assertTrue(page.content[0].sentAt.isAfter(page.content[1].sentAt))
    }

    @Test
    fun `should find email logs by status`() {
        val successLogs = emailLogRepository.findByStatus(EmailStatus.SUCCESS)
        val failedLogs = emailLogRepository.findByStatus(EmailStatus.FAILED)
        
        assertEquals(2, successLogs.size)
        assertEquals(1, failedLogs.size)
        assertTrue(successLogs.all { it.status == EmailStatus.SUCCESS })
        assertTrue(failedLogs.all { it.status == EmailStatus.FAILED })
    }

    @Test
    fun `should find email logs sent after specific date`() {
        val twoDaysAgo = LocalDateTime.now().minusDays(2)
        val recentLogs = emailLogRepository.findBySentAtAfter(twoDaysAgo)
        
        assertEquals(2, recentLogs.size)
        assertTrue(recentLogs.all { it.sentAt.isAfter(twoDaysAgo) })
    }

    @Test
    fun `should find failed email logs ordered by sent date desc`() {
        val failedLogs = emailLogRepository.findByStatusOrderBySentAtDesc(EmailStatus.FAILED)
        
        assertEquals(1, failedLogs.size)
        assertEquals(EmailStatus.FAILED, failedLogs[0].status)
        assertEquals("SMTP connection failed", failedLogs[0].errorMessage)
    }

    @Test
    fun `should find email logs by subscriber and status`() {
        val subscriber1SuccessLogs = emailLogRepository.findBySubscriberAndStatus(subscriber1, EmailStatus.SUCCESS)
        val subscriber1FailedLogs = emailLogRepository.findBySubscriberAndStatus(subscriber1, EmailStatus.FAILED)
        
        assertEquals(1, subscriber1SuccessLogs.size)
        assertEquals(1, subscriber1FailedLogs.size)
    }

    @Test
    fun `should count emails by status and sent after date`() {
        val twoDaysAgo = LocalDateTime.now().minusDays(2)
        val successCount = emailLogRepository.countByStatusAndSentAtAfter(EmailStatus.SUCCESS, twoDaysAgo)
        val failedCount = emailLogRepository.countByStatusAndSentAtAfter(EmailStatus.FAILED, twoDaysAgo)
        
        assertEquals(2L, successCount)
        assertEquals(0L, failedCount) // failed log is older than 2 days
    }

    @Test
    fun `should find latest email log by subscriber`() {
        val latestLog = emailLogRepository.findLatestBySubscriber(subscriber1)
        
        assertNotNull(latestLog)
        assertEquals(EmailStatus.SUCCESS, latestLog.status)
        assertEquals(3, latestLog.noticeCount)
    }

    @Test
    fun `should get email statistics for date range`() {
        val fromDate = LocalDateTime.now().minusDays(3)
        val toDate = LocalDateTime.now()
        val statistics = emailLogRepository.getEmailStatistics(fromDate, toDate)
        
        assertTrue(statistics.isNotEmpty())
        // Statistics should contain status and count pairs
        assertTrue(statistics.any { it[0] == EmailStatus.SUCCESS })
    }

    @Test
    fun `should find recent failed emails for retry`() {
        val oneDayAgo = LocalDateTime.now().minusDays(1)
        val recentFailedEmails = emailLogRepository.findRecentFailedEmails(oneDayAgo)
        
        // Our failed log is 2 days old, so it shouldn't be in recent failed emails
        assertEquals(0, recentFailedEmails.size)
        
        // Test with older date
        val threeDaysAgo = LocalDateTime.now().minusDays(3)
        val allFailedEmails = emailLogRepository.findRecentFailedEmails(threeDaysAgo)
        assertEquals(1, allFailedEmails.size)
    }
}