package com.kiki.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "subscribers",
    indexes = [
        Index(name = "idx_subscriber_email", columnList = "email"),
        Index(name = "idx_subscriber_is_active", columnList = "is_active"),
        Index(name = "idx_subscriber_unsubscribe_token", columnList = "unsubscribe_token")
    ]
)
data class Subscriber(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(name = "email", nullable = false, unique = true, length = 255)
    val email: String,
    
    @Column(name = "unsubscribe_token", nullable = false, unique = true, length = 100)
    val unsubscribeToken: String,
    
    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,
    
    @Column(name = "subscribed_at", nullable = false)
    val subscribedAt: LocalDateTime = LocalDateTime.now(),
    
    @OneToMany(mappedBy = "subscriber", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val emailLogs: List<EmailLog> = emptyList()
) {
    // JPA requires no-arg constructor
    constructor() : this(
        id = 0,
        email = "",
        unsubscribeToken = "",
        isActive = true,
        subscribedAt = LocalDateTime.now(),
        emailLogs = emptyList()
    )
}