package com.kiki.repository

import com.kiki.entity.Game
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface GameRepository : JpaRepository<Game, Long> {
    
    /**
     * Find all active games
     */
    fun findByIsActiveTrue(): List<Game>
    
    /**
     * Find game by name
     */
    fun findByName(name: String): Game?
    
    /**
     * Find game by name and active status
     */
    fun findByNameAndIsActive(name: String, isActive: Boolean): Game?
    
    /**
     * Check if game exists by name
     */
    fun existsByName(name: String): Boolean
    
    /**
     * Get count of active games
     */
    @Query("SELECT COUNT(g) FROM Game g WHERE g.isActive = true")
    fun countActiveGames(): Long
}