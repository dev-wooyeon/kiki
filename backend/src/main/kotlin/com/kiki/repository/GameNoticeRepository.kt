package com.kiki.repository

import com.kiki.entity.Game
import com.kiki.entity.GameNotice
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface GameNoticeRepository : JpaRepository<GameNotice, Long> {
    
    /**
     * Find notices published after a specific date
     */
    fun findByPublishedDateAfter(publishedDate: LocalDateTime): List<GameNotice>
    
    /**
     * Find notices published after a specific date, ordered by published date desc
     */
    fun findByPublishedDateAfterOrderByPublishedDateDesc(publishedDate: LocalDateTime): List<GameNotice>
    
    /**
     * Find notices that haven't been sent yet
     */
    fun findByIsSentFalse(): List<GameNotice>
    
    /**
     * Find notices by game and published after date
     */
    fun findByGameAndPublishedDateAfter(game: Game, publishedDate: LocalDateTime): List<GameNotice>
    
    /**
     * Find notices by game ID and published after date
     */
    fun findByGameIdAndPublishedDateAfter(gameId: Long, publishedDate: LocalDateTime): List<GameNotice>
    
    /**
     * Check if notice exists by URL
     */
    fun existsByUrl(url: String): Boolean
    
    /**
     * Find notice by URL
     */
    fun findByUrl(url: String): GameNotice?
    
    /**
     * Find recent notices (last 30 days) with pagination
     */
    @Query("SELECT gn FROM GameNotice gn WHERE gn.publishedDate >= :fromDate ORDER BY gn.publishedDate DESC")
    fun findRecentNotices(@Param("fromDate") fromDate: LocalDateTime, pageable: Pageable): Page<GameNotice>
    
    /**
     * Find recent notices by game (last 30 days) with pagination
     */
    @Query("SELECT gn FROM GameNotice gn WHERE gn.game.id = :gameId AND gn.publishedDate >= :fromDate ORDER BY gn.publishedDate DESC")
    fun findRecentNoticesByGame(@Param("gameId") gameId: Long, @Param("fromDate") fromDate: LocalDateTime, pageable: Pageable): Page<GameNotice>
    
    /**
     * Find unsent notices by game
     */
    fun findByGameAndIsSentFalse(game: Game): List<GameNotice>
    
    /**
     * Count notices published after a specific date
     */
    fun countByPublishedDateAfter(publishedDate: LocalDateTime): Long
    
    /**
     * Find latest notice by game
     */
    @Query("SELECT gn FROM GameNotice gn WHERE gn.game = :game ORDER BY gn.publishedDate DESC LIMIT 1")
    fun findLatestByGame(@Param("game") game: Game): GameNotice?
}