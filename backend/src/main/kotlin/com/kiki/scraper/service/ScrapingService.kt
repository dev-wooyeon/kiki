package com.kiki.scraper.service

import com.kiki.entity.Game
import com.kiki.entity.GameNotice
import com.kiki.repository.GameNoticeRepository
import com.kiki.repository.GameRepository
import com.kiki.scraper.GameScraper
import com.kiki.scraper.ScrapingException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDateTime

/**
 * 스크래핑 서비스
 * 모든 게임 스크래퍼를 통합 관리하고 중복 필터링 및 데이터베이스 저장을 담당합니다.
 */
@Service
@Transactional
class ScrapingService(
    private val gameRepository: GameRepository,
    private val gameNoticeRepository: GameNoticeRepository,
    private val scrapers: List<GameScraper>
) {
    
    companion object {
        private val logger = LoggerFactory.getLogger(ScrapingService::class.java)
    }
    
    /**
     * 모든 활성화된 게임의 공지사항을 스크래핑합니다.
     * @return 스크래핑 결과 정보
     */
    fun scrapeAllGames(): ScrapingResult {
        logger.info("Starting scraping process for all games")
        
        val startTime = LocalDateTime.now()
        val results = mutableMapOf<String, GameScrapingResult>()
        var totalNewNotices = 0
        
        try {
            // 활성화된 게임 목록 조회
            val activeGames = gameRepository.findByIsActiveTrue()
            logger.info("Found {} active games to scrape", activeGames.size)
            
            if (activeGames.isEmpty()) {
                logger.warn("No active games found for scraping")
                return ScrapingResult(
                    startTime = startTime,
                    endTime = LocalDateTime.now(),
                    totalGames = 0,
                    totalNewNotices = 0,
                    gameResults = emptyMap(),
                    success = true
                )
            }
            
            // 각 게임별로 스크래핑 실행
            for (game in activeGames) {
                val gameResult = scrapeGame(game)
                results[game.name] = gameResult
                totalNewNotices += gameResult.newNoticesCount
                
                logger.info("Game '{}' scraping completed: {} new notices", 
                    game.name, gameResult.newNoticesCount)
            }
            
            val endTime = LocalDateTime.now()
            logger.info("Scraping process completed. Total new notices: {}", totalNewNotices)
            
            return ScrapingResult(
                startTime = startTime,
                endTime = endTime,
                totalGames = activeGames.size,
                totalNewNotices = totalNewNotices,
                gameResults = results,
                success = true
            )
            
        } catch (e: Exception) {
            logger.error("Critical error during scraping process", e)
            return ScrapingResult(
                startTime = startTime,
                endTime = LocalDateTime.now(),
                totalGames = 0,
                totalNewNotices = totalNewNotices,
                gameResults = results,
                success = false,
                errorMessage = e.message
            )
        }
    }
    
    /**
     * 특정 게임의 공지사항을 스크래핑합니다.
     * @param game 스크래핑할 게임
     * @return 게임별 스크래핑 결과
     */
    private fun scrapeGame(game: Game): GameScrapingResult {
        val startTime = LocalDateTime.now()
        
        try {
            // 해당 게임의 스크래퍼 찾기
            val scraper = findScraperForGame(game)
            if (scraper == null) {
                logger.warn("No scraper found for game: {}", game.name)
                return GameScrapingResult(
                    gameName = game.name,
                    startTime = startTime,
                    endTime = LocalDateTime.now(),
                    scrapedNoticesCount = 0,
                    newNoticesCount = 0,
                    success = false,
                    errorMessage = "No scraper found for game: ${game.name}"
                )
            }
            
            logger.info("Starting scraping for game: {} using scraper: {}", 
                game.name, scraper::class.simpleName)
            
            // 공지사항 스크래핑
            val scrapedNotices = scraper.scrapeNotices()
            logger.info("Scraped {} notices for game: {}", scrapedNotices.size, game.name)
            scrapedNotices.forEach { notice ->
                logger.info(
                    "Scraped notice preview | game={} | title='{}' | publishedAt={}",
                    game.name,
                    notice.title,
                    notice.publishedDate
                )
            }
            
            // 중복 필터링 및 새로운 공지사항만 저장
            val newNotices = filterAndSaveNewNotices(game, scrapedNotices)
            if (newNotices.isNotEmpty()) {
                newNotices.forEach { notice ->
                    logger.info(
                        "Persisted new notice | game={} | id={} | title='{}' | url={}",
                        game.name,
                        notice.id,
                        notice.title,
                        notice.url
                    )
                }
            } else {
                logger.info("No new notices persisted for game: {}", game.name)
            }
            
            val endTime = LocalDateTime.now()
            logger.info("Game '{}' scraping completed successfully. New notices: {}/{}", 
                game.name, newNotices.size, scrapedNotices.size)
            
            return GameScrapingResult(
                gameName = game.name,
                startTime = startTime,
                endTime = endTime,
                scrapedNoticesCount = scrapedNotices.size,
                newNoticesCount = newNotices.size,
                success = true
            )
            
        } catch (e: ScrapingException) {
            logger.error("Scraping error for game '{}': {}", game.name, e.message, e)
            return GameScrapingResult(
                gameName = game.name,
                startTime = startTime,
                endTime = LocalDateTime.now(),
                scrapedNoticesCount = 0,
                newNoticesCount = 0,
                success = false,
                errorMessage = e.message
            )
        } catch (e: Exception) {
            logger.error("Unexpected error during scraping for game '{}'", game.name, e)
            return GameScrapingResult(
                gameName = game.name,
                startTime = startTime,
                endTime = LocalDateTime.now(),
                scrapedNoticesCount = 0,
                newNoticesCount = 0,
                success = false,
                errorMessage = "Unexpected error: ${e.message}"
            )
        }
    }
    
    /**
     * 게임에 해당하는 스크래퍼를 찾습니다.
     * @param game 게임 엔티티
     * @return 해당 게임의 스크래퍼 또는 null
     */
    private fun findScraperForGame(game: Game): GameScraper? {
        return scrapers.find { scraper ->
            scraper::class.qualifiedName == game.scraperClass ||
                scraper.getGameName().equals(game.name, ignoreCase = true)
        }
    }
    
    /**
     * 중복 공지사항을 필터링하고 새로운 공지사항만 데이터베이스에 저장합니다.
     * @param game 게임 엔티티
     * @param scrapedNotices 스크래핑된 공지사항 목록
     * @return 새로 저장된 공지사항 목록
     */
    private fun filterAndSaveNewNotices(game: Game, scrapedNotices: List<GameNotice>): List<GameNotice> {
        if (scrapedNotices.isEmpty()) {
            logger.debug("No notices to process for game: {}", game.name)
            return emptyList()
        }
        
        val newNotices = mutableListOf<GameNotice>()
        
        for (notice in scrapedNotices) {
            try {
                // URL 기반 중복 체크
                if (gameNoticeRepository.existsByUrl(notice.url)) {
                    logger.debug("Notice already exists, skipping: {}", notice.title)
                    continue
                }
                
                // 새로운 공지사항 저장
                val savedNotice = gameNoticeRepository.save(notice)
                newNotices.add(savedNotice)
                
                logger.debug("Saved new notice: {} (ID: {})", notice.title, savedNotice.id)
                
            } catch (e: Exception) {
                logger.warn("Failed to save notice '{}' for game '{}': {}", 
                    notice.title, game.name, e.message)
            }
        }
        
        logger.info("Filtered and saved {} new notices out of {} scraped for game: {}", 
            newNotices.size, scrapedNotices.size, game.name)
        
        return newNotices
    }
    
    /**
     * 발송되지 않은 새로운 공지사항을 조회합니다.
     * @return 발송되지 않은 공지사항 목록
     */
    @Transactional(readOnly = true)
    fun getUnsentNotices(): List<GameNotice> {
        return gameNoticeRepository.findByIsSentFalse()
    }
    
    /**
     * 공지사항을 발송 완료로 표시합니다.
     * @param notices 발송 완료할 공지사항 목록
     */
    fun markNoticesAsSent(notices: List<GameNotice>) {
        if (notices.isEmpty()) return
        
        try {
            logger.info("Marking {} notices as sent", notices.size)
            
            // 각 공지사항을 개별적으로 업데이트 (JPA에서 bulk update는 복잡하므로)
            notices.forEach { notice ->
                val updatedNotice = notice.copy(isSent = true)
                gameNoticeRepository.save(updatedNotice)
            }
            
            logger.info("Successfully marked {} notices as sent", notices.size)
            
        } catch (e: Exception) {
            logger.error("Failed to mark notices as sent", e)
            throw e
        }
    }
}

/**
 * 전체 스크래핑 결과
 */
data class ScrapingResult(
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val totalGames: Int,
    val totalNewNotices: Int,
    val gameResults: Map<String, GameScrapingResult>,
    val success: Boolean,
    val errorMessage: String? = null
) {
    val duration: Long
        get() = Duration.between(startTime, endTime).toMillis()
}

/**
 * 게임별 스크래핑 결과
 */
data class GameScrapingResult(
    val gameName: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val scrapedNoticesCount: Int,
    val newNoticesCount: Int,
    val success: Boolean,
    val errorMessage: String? = null
) {
    val duration: Long
        get() = Duration.between(startTime, endTime).toMillis()
}
