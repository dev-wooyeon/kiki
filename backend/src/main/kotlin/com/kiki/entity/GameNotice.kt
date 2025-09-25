package com.kiki.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "game_notices",
    indexes = [
        Index(name = "idx_notice_published_date", columnList = "published_date"),
        Index(name = "idx_notice_is_sent", columnList = "is_sent"),
        Index(name = "idx_notice_game_id", columnList = "game_id"),
        Index(name = "idx_notice_scraped_at", columnList = "scraped_at")
    ]
)
data class GameNotice(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    val game: Game,
    
    @Column(name = "title", nullable = false, length = 500)
    val title: String,
    
    @Column(name = "url", nullable = false, unique = true, length = 1000)
    val url: String,
    
    @Column(name = "summary", columnDefinition = "TEXT")
    val summary: String? = null,
    
    @Column(name = "published_date", nullable = false)
    val publishedDate: LocalDateTime,
    
    @Column(name = "scraped_at", nullable = false)
    val scrapedAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "is_sent", nullable = false)
    val isSent: Boolean = false
) {
    // JPA requires no-arg constructor
    constructor() : this(
        id = 0,
        game = Game(),
        title = "",
        url = "",
        summary = null,
        publishedDate = LocalDateTime.now(),
        scrapedAt = LocalDateTime.now(),
        isSent = false
    )
}