package com.kiki.service

import com.kiki.dto.SubscriptionResult
import com.kiki.entity.Subscriber
import com.kiki.repository.SubscriberRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.LocalDateTime
import java.util.*

/**
 * Service for managing email subscriptions
 */
@Service
@Transactional
class SubscriptionService(
    private val subscriberRepository: SubscriberRepository
) {
    
    private val logger = LoggerFactory.getLogger(SubscriptionService::class.java)
    private val secureRandom = SecureRandom()
    
    /**
     * Subscribe a user with the given email address
     * 
     * @param email The email address to subscribe
     * @return SubscriptionResult indicating success or failure with message
     */
    fun subscribe(email: String): SubscriptionResult {
        logger.info("Attempting to subscribe email: {}", email)
        
        // Validate email format
        if (!isValidEmail(email)) {
            logger.warn("Invalid email format: {}", email)
            return SubscriptionResult(
                success = false,
                message = "올바른 이메일 주소를 입력해주세요"
            )
        }
        
        // Check if email already exists and is active
        val existingSubscriber = subscriberRepository.findByEmail(email)
        if (existingSubscriber != null && existingSubscriber.isActive) {
            logger.warn("Email already subscribed and active: {}", email)
            return SubscriptionResult(
                success = false,
                message = "이미 구독된 이메일입니다"
            )
        }
        
        // If subscriber exists but is inactive, reactivate
        if (existingSubscriber != null && !existingSubscriber.isActive) {
            logger.info("Reactivating inactive subscriber: {}", email)
            val reactivatedSubscriber = existingSubscriber.copy(
                isActive = true,
                subscribedAt = LocalDateTime.now(),
                unsubscribeToken = generateUnsubscribeToken()
            )
            val savedSubscriber = subscriberRepository.save(reactivatedSubscriber)
            
            return SubscriptionResult(
                success = true,
                message = "구독이 성공적으로 등록되었습니다",
                subscriber = savedSubscriber
            )
        }
        
        // Create new subscriber
        try {
            val newSubscriber = Subscriber(
                email = email,
                unsubscribeToken = generateUnsubscribeToken(),
                isActive = true,
                subscribedAt = LocalDateTime.now()
            )
            
            val savedSubscriber = subscriberRepository.save(newSubscriber)
            logger.info("Successfully subscribed new email: {}", email)
            
            return SubscriptionResult(
                success = true,
                message = "구독이 성공적으로 등록되었습니다",
                subscriber = savedSubscriber
            )
        } catch (e: Exception) {
            logger.error("Failed to subscribe email: {}", email, e)
            return SubscriptionResult(
                success = false,
                message = "구독 등록 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요"
            )
        }
    }
    
    /**
     * Unsubscribe a user using the unsubscribe token
     * 
     * @param token The unsubscribe token
     * @return Boolean indicating success or failure
     */
    fun unsubscribe(token: String): Boolean {
        logger.info("Attempting to unsubscribe with token: {}", token)
        
        if (token.isBlank()) {
            logger.warn("Empty unsubscribe token provided")
            return false
        }
        
        try {
            val subscriber = subscriberRepository.findByUnsubscribeToken(token)
            if (subscriber == null) {
                logger.warn("No subscriber found for unsubscribe token: {}", token)
                return false
            }
            
            if (!subscriber.isActive) {
                logger.info("Subscriber already inactive for token: {}", token)
                return true // Already unsubscribed, consider it success
            }
            
            // Deactivate subscriber
            val unsubscribedSubscriber = subscriber.copy(isActive = false)
            subscriberRepository.save(unsubscribedSubscriber)
            
            logger.info("Successfully unsubscribed email: {}", subscriber.email)
            return true
            
        } catch (e: Exception) {
            logger.error("Failed to unsubscribe with token: {}", token, e)
            return false
        }
    }
    
    /**
     * Get all active subscribers
     * 
     * @return List of active subscribers
     */
    @Transactional(readOnly = true)
    fun getActiveSubscribers(): List<Subscriber> {
        logger.debug("Fetching all active subscribers")
        return subscriberRepository.findByIsActiveTrue()
    }

    /**
     * Get count of active subscribers
     */
    @Transactional(readOnly = true)
    fun getActiveSubscriberCount(): Long {
        return subscriberRepository.countActiveSubscribers()
    }
    
    /**
     * Get subscriber by email
     * 
     * @param email The email address
     * @return Subscriber if found, null otherwise
     */
    @Transactional(readOnly = true)
    fun getSubscriberByEmail(email: String): Subscriber? {
        return subscriberRepository.findByEmail(email)
    }
    
    /**
     * Get subscriber by unsubscribe token
     * 
     * @param token The unsubscribe token
     * @return Subscriber if found, null otherwise
     */
    @Transactional(readOnly = true)
    fun getSubscriberByToken(token: String): Subscriber? {
        return subscriberRepository.findByUnsubscribeToken(token)
    }
    
    /**
     * Check if email is already subscribed and active
     * 
     * @param email The email address to check
     * @return Boolean indicating if email is actively subscribed
     */
    @Transactional(readOnly = true)
    fun isEmailSubscribed(email: String): Boolean {
        return subscriberRepository.existsByEmailAndIsActive(email, true)
    }
    
    /**
     * Generate a secure unsubscribe token
     * 
     * @return A unique unsubscribe token
     */
    private fun generateUnsubscribeToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
    
    /**
     * Validate email format using a simple regex
     * 
     * @param email The email address to validate
     * @return Boolean indicating if email format is valid
     */
    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
        return email.matches(emailRegex.toRegex())
    }
}
