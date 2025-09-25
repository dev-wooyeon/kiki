package com.kiki.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "games",
    indexes = [
        Index(name = "idx_game_name", columnList = "name"),
        Index(name = "idx_game_is_active", columnList = "is_active")
    ]
)
data class Game(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(name = "name", unique = true, nullable = false, length = 100)
    val name: String,
    
    @Column(name = "base_url", nullable = false, length = 500)
    val baseUrl: String,
    
    @Column(name = "scraper_class", nullable = false, length = 200)
    val scraperClass: String,
    
    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @OneToMany(mappedBy = "game", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val notices: List<GameNotice> = emptyList()
) {
    // JPA requires no-arg constructor
    constructor() : this(
        id = 0,
        name = "",
        baseUrl = "",
        scraperClass = "",
        isActive = true,
        createdAt = LocalDateTime.now(),
        notices = emptyList()
    )
}