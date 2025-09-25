package com.kiki.service

import com.kiki.dto.SubscriptionResult
import com.kiki.entity.Subscriber
import com.kiki.repository.SubscriberRepository
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class SubscriptionServiceTest {
    
    private val subscriberRepository = mockk<SubscriberRepository>()
    private lateinit var subscriptionService: SubscriptionService
    
    @BeforeEach
    fun setUp() {
        clearAllMocks()
        subscriptionService = SubscriptionService(subscriberRepository)
    }
    
    @Test
    fun `subscribe - should successfully subscribe new email`() {
        // Given
        val email = "test@example.com"
        every { subscriberRepository.findByEmail(email) } returns null
        every { subscriberRepository.save(any<Subscriber>()) } answers { 
            val subscriber = firstArg<Subscriber>()
            subscriber.copy(id = 1L)
        }
        
        // When
        val result = subscriptionService.subscribe(email)
        
        // Then
        assertTrue(result.success)
        assertEquals("구독이 성공적으로 등록되었습니다", result.message)
        assertNotNull(result.subscriber)
        assertEquals(email, result.subscriber?.email)
        assertTrue(result.subscriber?.isActive ?: false)
        assertNotNull(result.subscriber?.unsubscribeToken)
        
        verify { subscriberRepository.findByEmail(email) }
        verify { subscriberRepository.save(any<Subscriber>()) }
    }
    
    @Test
    fun `subscribe - should reject invalid email format`() {
        // Given
        val invalidEmail = "invalid-email"
        
        // When
        val result = subscriptionService.subscribe(invalidEmail)
        
        // Then
        assertFalse(result.success)
        assertEquals("올바른 이메일 주소를 입력해주세요", result.message)
        assertNull(result.subscriber)
        
        verify(exactly = 0) { subscriberRepository.findByEmail(any()) }
        verify(exactly = 0) { subscriberRepository.save(any<Subscriber>()) }
    }
    
    @Test
    fun `subscribe - should reject already active subscriber`() {
        // Given
        val email = "test@example.com"
        val existingSubscriber = Subscriber(
            id = 1L,
            email = email,
            unsubscribeToken = "token123",
            isActive = true,
            subscribedAt = LocalDateTime.now()
        )
        every { subscriberRepository.findByEmail(email) } returns existingSubscriber
        
        // When
        val result = subscriptionService.subscribe(email)
        
        // Then
        assertFalse(result.success)
        assertEquals("이미 구독된 이메일입니다", result.message)
        assertNull(result.subscriber)
        
        verify { subscriberRepository.findByEmail(email) }
        verify(exactly = 0) { subscriberRepository.save(any<Subscriber>()) }
    }
    
    @Test
    fun `subscribe - should reactivate inactive subscriber`() {
        // Given
        val email = "test@example.com"
        val inactiveSubscriber = Subscriber(
            id = 1L,
            email = email,
            unsubscribeToken = "oldtoken",
            isActive = false,
            subscribedAt = LocalDateTime.now().minusDays(1)
        )
        every { subscriberRepository.findByEmail(email) } returns inactiveSubscriber
        every { subscriberRepository.save(any<Subscriber>()) } answers { firstArg() }
        
        // When
        val result = subscriptionService.subscribe(email)
        
        // Then
        assertTrue(result.success)
        assertEquals("구독이 성공적으로 등록되었습니다", result.message)
        assertNotNull(result.subscriber)
        assertTrue(result.subscriber?.isActive ?: false)
        assertNotEquals("oldtoken", result.subscriber?.unsubscribeToken)
        
        verify { subscriberRepository.findByEmail(email) }
        verify { subscriberRepository.save(any<Subscriber>()) }
    }
    
    @Test
    fun `unsubscribe - should successfully unsubscribe with valid token`() {
        // Given
        val token = "validtoken123"
        val subscriber = Subscriber(
            id = 1L,
            email = "test@example.com",
            unsubscribeToken = token,
            isActive = true,
            subscribedAt = LocalDateTime.now()
        )
        every { subscriberRepository.findByUnsubscribeToken(token) } returns subscriber
        every { subscriberRepository.save(any<Subscriber>()) } answers { firstArg() }
        
        // When
        val result = subscriptionService.unsubscribe(token)
        
        // Then
        assertTrue(result)
        
        verify { subscriberRepository.findByUnsubscribeToken(token) }
        verify { subscriberRepository.save(any<Subscriber>()) }
    }
    
    @Test
    fun `unsubscribe - should return false for invalid token`() {
        // Given
        val invalidToken = "invalidtoken"
        every { subscriberRepository.findByUnsubscribeToken(invalidToken) } returns null
        
        // When
        val result = subscriptionService.unsubscribe(invalidToken)
        
        // Then
        assertFalse(result)
        
        verify { subscriberRepository.findByUnsubscribeToken(invalidToken) }
        verify(exactly = 0) { subscriberRepository.save(any<Subscriber>()) }
    }
    
    @Test
    fun `unsubscribe - should return true for already inactive subscriber`() {
        // Given
        val token = "validtoken123"
        val inactiveSubscriber = Subscriber(
            id = 1L,
            email = "test@example.com",
            unsubscribeToken = token,
            isActive = false,
            subscribedAt = LocalDateTime.now()
        )
        every { subscriberRepository.findByUnsubscribeToken(token) } returns inactiveSubscriber
        
        // When
        val result = subscriptionService.unsubscribe(token)
        
        // Then
        assertTrue(result)
        
        verify { subscriberRepository.findByUnsubscribeToken(token) }
        verify(exactly = 0) { subscriberRepository.save(any<Subscriber>()) }
    }
    
    @Test
    fun `unsubscribe - should return false for empty token`() {
        // Given
        val emptyToken = ""
        
        // When
        val result = subscriptionService.unsubscribe(emptyToken)
        
        // Then
        assertFalse(result)
        
        verify(exactly = 0) { subscriberRepository.findByUnsubscribeToken(any()) }
        verify(exactly = 0) { subscriberRepository.save(any<Subscriber>()) }
    }
    
    @Test
    fun `getActiveSubscribers - should return list of active subscribers`() {
        // Given
        val activeSubscribers = listOf(
            Subscriber(
                id = 1L,
                email = "user1@example.com",
                unsubscribeToken = "token1",
                isActive = true,
                subscribedAt = LocalDateTime.now()
            ),
            Subscriber(
                id = 2L,
                email = "user2@example.com",
                unsubscribeToken = "token2",
                isActive = true,
                subscribedAt = LocalDateTime.now()
            )
        )
        every { subscriberRepository.findByIsActiveTrue() } returns activeSubscribers
        
        // When
        val result = subscriptionService.getActiveSubscribers()
        
        // Then
        assertEquals(2, result.size)
        assertEquals(activeSubscribers, result)
        
        verify { subscriberRepository.findByIsActiveTrue() }
    }
    
    @Test
    fun `isEmailSubscribed - should return true for active subscriber`() {
        // Given
        val email = "test@example.com"
        every { subscriberRepository.existsByEmailAndIsActive(email, true) } returns true
        
        // When
        val result = subscriptionService.isEmailSubscribed(email)
        
        // Then
        assertTrue(result)
        
        verify { subscriberRepository.existsByEmailAndIsActive(email, true) }
    }
    
    @Test
    fun `isEmailSubscribed - should return false for inactive or non-existent subscriber`() {
        // Given
        val email = "test@example.com"
        every { subscriberRepository.existsByEmailAndIsActive(email, true) } returns false
        
        // When
        val result = subscriptionService.isEmailSubscribed(email)
        
        // Then
        assertFalse(result)
        
        verify { subscriberRepository.existsByEmailAndIsActive(email, true) }
    }
    
    @Test
    fun `getSubscriberByEmail - should return subscriber when found`() {
        // Given
        val email = "test@example.com"
        val subscriber = Subscriber(
            id = 1L,
            email = email,
            unsubscribeToken = "token123",
            isActive = true,
            subscribedAt = LocalDateTime.now()
        )
        every { subscriberRepository.findByEmail(email) } returns subscriber
        
        // When
        val result = subscriptionService.getSubscriberByEmail(email)
        
        // Then
        assertEquals(subscriber, result)
        
        verify { subscriberRepository.findByEmail(email) }
    }
    
    @Test
    fun `getSubscriberByToken - should return subscriber when found`() {
        // Given
        val token = "token123"
        val subscriber = Subscriber(
            id = 1L,
            email = "test@example.com",
            unsubscribeToken = token,
            isActive = true,
            subscribedAt = LocalDateTime.now()
        )
        every { subscriberRepository.findByUnsubscribeToken(token) } returns subscriber
        
        // When
        val result = subscriptionService.getSubscriberByToken(token)
        
        // Then
        assertEquals(subscriber, result)
        
        verify { subscriberRepository.findByUnsubscribeToken(token) }
    }
}