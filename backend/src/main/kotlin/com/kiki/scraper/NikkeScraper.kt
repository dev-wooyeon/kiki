package com.kiki.scraper

import com.kiki.entity.Game
import com.kiki.entity.GameNotice
import com.kiki.scraper.client.NikkeCmsClient
import org.jsoup.nodes.Document
import org.springframework.stereotype.Component

/**
 * 승리의 여신 니케 공지사항 스크래퍼
 * CMS API를 통해 최신 공지 데이터를 확보합니다.
 */
@Component
class NikkeScraper(
    private val nikkeCmsClient: NikkeCmsClient
) : AbstractGameScraper() {

    companion object {
        private const val GAME_NAME = "승리의 여신 니케"
        private const val BASE_URL = "https://nikke-kr.com"
        private const val NOTICE_URL = "https://nikke-kr.com/news.html"
    }

    override fun getGameName(): String = GAME_NAME

    override fun getBaseUrl(): String = BASE_URL

    override fun getNoticeUrl(): String = NOTICE_URL

    override fun scrapeNotices(): List<GameNotice> {
        try {
            logger.info("Starting scraping for game: {} via CMS API", getGameName())
            val game = getOrCreateGame()

            val notices = nikkeCmsClient.fetchLatestNotices().map { notice ->
                createGameNotice(
                    game = game,
                    title = notice.title,
                    url = notice.url,
                    publishedDate = notice.publishedAt,
                    summary = notice.summary
                )
            }

            logger.info(
                "Successfully scraped {} NIKKE notices (after filtering)",
                notices.size
            )
            return notices
        } catch (ex: Exception) {
            logger.error("Failed to scrape NIKKE notices", ex)
            throw ScrapingException("Failed to scrape notices for ${getGameName()}", ex)
        }
    }

    override fun parseNotices(document: Document, game: Game): List<GameNotice> {
        // CMS API 기반으로 수집하므로 HTML 파싱은 사용하지 않습니다.
        return emptyList()
    }
}
