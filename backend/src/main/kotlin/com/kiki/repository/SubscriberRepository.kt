package com.kiki.repository

import com.kiki.entity.Subscriber
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface SubscriberRepository : JpaRepository<Subscriber, Long> {
    
    /**
     * Find all active subscribers
     */
    fun findByIsActiveTrue(): List<Subscriber>
    
    /**
     * Find subscriber by email
     */
    fun findByEmail(email: String): Subscriber?
    
    /**
     * Find subscriber by unsubscribe token
     */
    fun findByUnsubscribeToken(unsubscribeToken: String): Subscriber?
    
    /**
     * Check if subscriber exists by email
     */
    fun existsByEmail(email: String): Boolean
    
    /**
     * Check if active subscriber exists by email
     */
    fun existsByEmailAndIsActive(email: String, isActive: Boolean): Boolean
    
    /**
     * Find active subscriber by email
     */
    fun findByEmailAndIsActive(email: String, isActive: Boolean): Subscriber?
    
    /**
     * Count active subscribers
     */
    @Query("SELECT COUNT(s) FROM Subscriber s WHERE s.isActive = true")
    fun countActiveSubscribers(): Long
    
    /**
     * Find subscribers who subscribed after a specific date
     */
    fun findBySubscribedAtAfter(subscribedAt: LocalDateTime): List<Subscriber>
    
    /**
     * Find active subscribers who subscribed after a specific date
     */
    fun findByIsActiveTrueAndSubscribedAtAfter(subscribedAt: LocalDateTime): List<Subscriber>
    
    /**
     * Check if unsubscribe token exists
     */
    fun existsByUnsubscribeToken(unsubscribeToken: String): Boolean
}