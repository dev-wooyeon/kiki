package com.kiki.repository

import com.kiki.entity.Subscriber
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

@DataJpaTest
@ActiveProfiles("test")
class SubscriberRepositoryTest {

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var subscriberRepository: SubscriberRepository

    private lateinit var activeSubscriber: Subscriber
    private lateinit var inactiveSubscriber: Subscriber
    private lateinit var recentSubscriber: Subscriber

    @BeforeEach
    fun setUp() {
        val now = LocalDateTime.now()
        
        activeSubscriber = Subscriber(
            email = "active@example.com",
            unsubscribeToken = "token123",
            isActive = true,
            subscribedAt = now.minusDays(10)
        )

        inactiveSubscriber = Subscriber(
            email = "inactive@example.com",
            unsubscribeToken = "token456",
            isActive = false,
            subscribedAt = now.minusDays(20)
        )

        recentSubscriber = Subscriber(
            email = "recent@example.com",
            unsubscribeToken = "token789",
            isActive = true,
            subscribedAt = now.minusHours(1)
        )

        entityManager.persistAndFlush(activeSubscriber)
        entityManager.persistAndFlush(inactiveSubscriber)
        entityManager.persistAndFlush(recentSubscriber)
    }

    @Test
    fun `should find all active subscribers`() {
        val activeSubscribers = subscriberRepository.findByIsActiveTrue()
        
        assertEquals(2, activeSubscribers.size)
        assertTrue(activeSubscribers.all { it.isActive })
        assertTrue(activeSubscribers.any { it.email == "active@example.com" })
        assertTrue(activeSubscribers.any { it.email == "recent@example.com" })
    }

    @Test
    fun `should find subscriber by email`() {
        val foundSubscriber = subscriberRepository.findByEmail("active@example.com")
        
        assertNotNull(foundSubscriber)
        assertEquals("active@example.com", foundSubscriber.email)
        assertEquals("token123", foundSubscriber.unsubscribeToken)
    }

    @Test
    fun `should return null when subscriber not found by email`() {
        val foundSubscriber = subscriberRepository.findByEmail("nonexistent@example.com")
        
        assertNull(foundSubscriber)
    }

    @Test
    fun `should find subscriber by unsubscribe token`() {
        val foundSubscriber = subscriberRepository.findByUnsubscribeToken("token123")
        
        assertNotNull(foundSubscriber)
        assertEquals("active@example.com", foundSubscriber.email)
    }

    @Test
    fun `should check if subscriber exists by email`() {
        assertTrue(subscriberRepository.existsByEmail("active@example.com"))
        assertTrue(subscriberRepository.existsByEmail("inactive@example.com"))
        assertFalse(subscriberRepository.existsByEmail("nonexistent@example.com"))
    }

    @Test
    fun `should check if active subscriber exists by email`() {
        assertTrue(subscriberRepository.existsByEmailAndIsActive("active@example.com", true))
        assertFalse(subscriberRepository.existsByEmailAndIsActive("inactive@example.com", true))
        assertTrue(subscriberRepository.existsByEmailAndIsActive("inactive@example.com", false))
        assertFalse(subscriberRepository.existsByEmailAndIsActive("nonexistent@example.com", true))
    }

    @Test
    fun `should find active subscriber by email`() {
        val activeFound = subscriberRepository.findByEmailAndIsActive("active@example.com", true)
        val inactiveFound = subscriberRepository.findByEmailAndIsActive("inactive@example.com", true)
        
        assertNotNull(activeFound)
        assertEquals("active@example.com", activeFound.email)
        assertTrue(activeFound.isActive)
        
        assertNull(inactiveFound)
    }

    @Test
    fun `should count active subscribers correctly`() {
        val count = subscriberRepository.countActiveSubscribers()
        
        assertEquals(2L, count)
    }

    @Test
    fun `should find subscribers who subscribed after specific date`() {
        val fifteenDaysAgo = LocalDateTime.now().minusDays(15)
        val recentSubscribers = subscriberRepository.findBySubscribedAtAfter(fifteenDaysAgo)
        
        assertEquals(2, recentSubscribers.size)
        assertTrue(recentSubscribers.any { it.email == "active@example.com" })
        assertTrue(recentSubscribers.any { it.email == "recent@example.com" })
    }

    @Test
    fun `should find active subscribers who subscribed after specific date`() {
        val fifteenDaysAgo = LocalDateTime.now().minusDays(15)
        val activeRecentSubscribers = subscriberRepository.findByIsActiveTrueAndSubscribedAtAfter(fifteenDaysAgo)
        
        assertEquals(2, activeRecentSubscribers.size)
        assertTrue(activeRecentSubscribers.all { it.isActive })
        assertTrue(activeRecentSubscribers.all { it.subscribedAt.isAfter(fifteenDaysAgo) })
    }

    @Test
    fun `should check if unsubscribe token exists`() {
        assertTrue(subscriberRepository.existsByUnsubscribeToken("token123"))
        assertTrue(subscriberRepository.existsByUnsubscribeToken("token456"))
        assertFalse(subscriberRepository.existsByUnsubscribeToken("nonexistent-token"))
    }
}