package com.kiki.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "email_logs",
    indexes = [
        Index(name = "idx_email_log_subscriber_id", columnList = "subscriber_id"),
        Index(name = "idx_email_log_sent_at", columnList = "sent_at"),
        Index(name = "idx_email_log_status", columnList = "status")
    ]
)
data class EmailLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscriber_id", nullable = false)
    val subscriber: Subscriber,
    
    @Column(name = "sent_at", nullable = false)
    val sentAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "notice_count", nullable = false)
    val noticeCount: Int,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: EmailStatus,
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    val errorMessage: String? = null
) {
    // JPA requires no-arg constructor
    constructor() : this(
        id = 0,
        subscriber = Subscriber(),
        sentAt = LocalDateTime.now(),
        noticeCount = 0,
        status = EmailStatus.SUCCESS,
        errorMessage = null
    )
}

enum class EmailStatus {
    SUCCESS,
    FAILED
}