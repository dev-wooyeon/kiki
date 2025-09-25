package com.kiki.config

import com.kiki.entity.Game
import com.kiki.repository.GameRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GameDataInitializer {

    private val logger = LoggerFactory.getLogger(GameDataInitializer::class.java)

    private val scraperClassMap = mapOf(
        "nikke" to "com.kiki.scraper.NikkeScraper",
        "mabinogi-mobile" to "com.kiki.scraper.MabinogiMobileScraper",
        "genshin" to "com.kiki.scraper.GenshinScraper",
        "wuthering-waves" to "com.kiki.scraper.WutheringWavesScraper"
    )

    @Bean
    fun seedGames(gameRepository: GameRepository, kikiProperties: KikiProperties) = ApplicationRunner {
        kikiProperties.games.forEach { (key, gameConfig) ->
            val scraperClass = scraperClassMap[key]
            if (scraperClass == null) {
                logger.warn("No scraper class mapping found for game key '{}'. Skipping seeding.", key)
                return@forEach
            }

            val existing = gameRepository.findByName(gameConfig.name)
            if (existing == null) {
                val game = Game(
                    name = gameConfig.name,
                    baseUrl = gameConfig.baseUrl,
                    scraperClass = scraperClass
                )
                gameRepository.save(game)
                logger.info("Seeded game '{}'.", gameConfig.name)
            } else {
                if (existing.baseUrl != gameConfig.baseUrl || existing.scraperClass != scraperClass) {
                    gameRepository.save(
                        existing.copy(
                            baseUrl = gameConfig.baseUrl,
                            scraperClass = scraperClass
                        )
                    )
                    logger.info("Updated game '{}'.", gameConfig.name)
                }
            }
        }
    }
}
