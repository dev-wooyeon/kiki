package com.kiki.repository

import com.kiki.entity.EmailLog
import com.kiki.entity.EmailStatus
import com.kiki.entity.Subscriber
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface EmailLogRepository : JpaRepository<EmailLog, Long> {
    
    /**
     * Find email logs by subscriber
     */
    fun findBySubscriber(subscriber: Subscriber): List<EmailLog>
    
    /**
     * Find email logs by subscriber with pagination
     */
    fun findBySubscriberOrderBySentAtDesc(subscriber: Subscriber, pageable: Pageable): Page<EmailLog>
    
    /**
     * Find email logs by status
     */
    fun findByStatus(status: EmailStatus): List<EmailLog>
    
    /**
     * Find email logs sent after a specific date
     */
    fun findBySentAtAfter(sentAt: LocalDateTime): List<EmailLog>
    
    /**
     * Find failed email logs
     */
    fun findByStatusOrderBySentAtDesc(status: EmailStatus): List<EmailLog>
    
    /**
     * Find email logs by subscriber and status
     */
    fun findBySubscriberAndStatus(subscriber: Subscriber, status: EmailStatus): List<EmailLog>
    
    /**
     * Count successful emails sent after a specific date
     */
    @Query("SELECT COUNT(el) FROM EmailLog el WHERE el.status = :status AND el.sentAt >= :fromDate")
    fun countByStatusAndSentAtAfter(@Param("status") status: EmailStatus, @Param("fromDate") fromDate: LocalDateTime): Long
    
    /**
     * Find latest email log by subscriber
     */
    @Query("SELECT el FROM EmailLog el WHERE el.subscriber = :subscriber ORDER BY el.sentAt DESC LIMIT 1")
    fun findLatestBySubscriber(@Param("subscriber") subscriber: Subscriber): EmailLog?
    
    /**
     * Get email statistics for a date range
     */
    @Query("""
        SELECT el.status, COUNT(el) 
        FROM EmailLog el 
        WHERE el.sentAt BETWEEN :fromDate AND :toDate 
        GROUP BY el.status
    """)
    fun getEmailStatistics(@Param("fromDate") fromDate: LocalDateTime, @Param("toDate") toDate: LocalDateTime): List<Array<Any>>
    
    /**
     * Find recent failed emails for retry
     */
    @Query("""
        SELECT el FROM EmailLog el 
        WHERE el.status = 'FAILED' 
        AND el.sentAt >= :fromDate 
        ORDER BY el.sentAt DESC
    """)
    fun findRecentFailedEmails(@Param("fromDate") fromDate: LocalDateTime): List<EmailLog>
}