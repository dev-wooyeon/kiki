package com.kiki.repository

import com.kiki.entity.Game
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

@DataJpaTest
@ActiveProfiles("test")
class GameRepositoryTest {

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var gameRepository: GameRepository

    private lateinit var testGame1: Game
    private lateinit var testGame2: Game
    private lateinit var inactiveGame: Game

    @BeforeEach
    fun setUp() {
        testGame1 = Game(
            name = "NIKKE",
            baseUrl = "https://nikke-kr.com",
            scraperClass = "com.kiki.scraper.NikkeScraper",
            isActive = true
        )

        testGame2 = Game(
            name = "원신",
            baseUrl = "https://genshin.hoyoverse.com",
            scraperClass = "com.kiki.scraper.GenshinScraper",
            isActive = true
        )

        inactiveGame = Game(
            name = "Inactive Game",
            baseUrl = "https://inactive.com",
            scraperClass = "com.kiki.scraper.InactiveScraper",
            isActive = false
        )

        entityManager.persistAndFlush(testGame1)
        entityManager.persistAndFlush(testGame2)
        entityManager.persistAndFlush(inactiveGame)
    }

    @Test
    fun `should find all active games`() {
        val activeGames = gameRepository.findByIsActiveTrue()
        
        assertEquals(2, activeGames.size)
        assertTrue(activeGames.all { it.isActive })
        assertTrue(activeGames.any { it.name == "NIKKE" })
        assertTrue(activeGames.any { it.name == "원신" })
    }

    @Test
    fun `should find game by name`() {
        val foundGame = gameRepository.findByName("NIKKE")
        
        assertNotNull(foundGame)
        assertEquals("NIKKE", foundGame.name)
        assertEquals("https://nikke-kr.com", foundGame.baseUrl)
    }

    @Test
    fun `should return null when game not found by name`() {
        val foundGame = gameRepository.findByName("Non-existent Game")
        
        assertNull(foundGame)
    }

    @Test
    fun `should find game by name and active status`() {
        val activeGame = gameRepository.findByNameAndIsActive("NIKKE", true)
        val inactiveGameResult = gameRepository.findByNameAndIsActive("Inactive Game", false)
        val nonExistentActive = gameRepository.findByNameAndIsActive("Inactive Game", true)
        
        assertNotNull(activeGame)
        assertEquals("NIKKE", activeGame.name)
        
        assertNotNull(inactiveGameResult)
        assertEquals("Inactive Game", inactiveGameResult.name)
        
        assertNull(nonExistentActive)
    }

    @Test
    fun `should check if game exists by name`() {
        assertTrue(gameRepository.existsByName("NIKKE"))
        assertTrue(gameRepository.existsByName("Inactive Game"))
        assertFalse(gameRepository.existsByName("Non-existent Game"))
    }

    @Test
    fun `should count active games correctly`() {
        val count = gameRepository.countActiveGames()
        
        assertEquals(2L, count)
    }
}
