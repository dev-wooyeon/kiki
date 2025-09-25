package com.kiki.scraper

import com.kiki.entity.Game
import com.kiki.entity.GameNotice
import com.kiki.repository.GameRepository
import org.jsoup.nodes.Document
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

/**
 * 게임 스크래퍼의 추상 기본 클래스
 * 공통 기능을 제공하고 구체적인 스크래핑 로직은 하위 클래스에서 구현합니다.
 */
abstract class AbstractGameScraper : GameScraper {
    
    protected val logger = LoggerFactory.getLogger(this::class.java)
    
    @Autowired
    protected lateinit var httpClientUtil: HttpClientUtil
    
    @Autowired
    protected lateinit var parsingUtil: ParsingUtil
    
    @Autowired
    protected lateinit var gameRepository: GameRepository
    
    /**
     * 게임 엔티티를 조회하거나 생성합니다.
     * @return 게임 엔티티
     */
    protected fun getOrCreateGame(): Game {
        return gameRepository.findByName(getGameName())
            ?: throw ScrapingException("Game not found: ${getGameName()}. Please ensure the game is registered in the database.")
    }
    
    /**
     * 공지사항 페이지를 가져와서 파싱합니다.
     * @return 스크래핑된 공지사항 목록
     */
    override fun scrapeNotices(): List<GameNotice> {
        try {
            logger.info("Starting scraping for game: {}", getGameName())
            
            val document = httpClientUtil.fetchDocument(getNoticeUrl())
            val game = getOrCreateGame()
            val notices = parseNotices(document, game)
            
            logger.info("Successfully scraped {} notices for game: {}", notices.size, getGameName())
            return notices
            
        } catch (e: Exception) {
            logger.error("Failed to scrape notices for game: {}", getGameName(), e)
            throw ScrapingException("Failed to scrape notices for ${getGameName()}", e)
        }
    }
    
    /**
     * HTML 문서에서 공지사항 목록을 파싱합니다.
     * 하위 클래스에서 구현해야 합니다.
     * @param document 파싱할 HTML 문서
     * @param game 게임 엔티티
     * @return 파싱된 공지사항 목록
     */
    protected abstract fun parseNotices(document: Document, game: Game): List<GameNotice>
    
    /**
     * 공지사항 객체를 생성하는 헬퍼 메서드
     * @param game 게임 엔티티
     * @param title 공지사항 제목
     * @param url 공지사항 URL
     * @param publishedDate 게시일자
     * @param summary 요약 (선택사항)
     * @return 생성된 GameNotice 객체
     */
    protected fun createGameNotice(
        game: Game,
        title: String,
        url: String,
        publishedDate: LocalDateTime,
        summary: String? = null
    ): GameNotice {
        return GameNotice(
            game = game,
            title = parsingUtil.cleanText(title),
            url = parsingUtil.resolveUrl(getBaseUrl(), url),
            publishedDate = publishedDate,
            summary = summary?.let { parsingUtil.summarizeText(it) },
            scrapedAt = LocalDateTime.now(),
            isSent = false
        )
    }
}