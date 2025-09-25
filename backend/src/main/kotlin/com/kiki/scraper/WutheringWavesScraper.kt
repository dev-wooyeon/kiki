package com.kiki.scraper

import com.kiki.entity.Game
import com.kiki.entity.GameNotice
import com.kiki.scraper.client.WutheringWavesClient
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 명조(워더링 웨이브) 공지사항 스크래퍼
 * 공식 웹사이트의 JSON 엔드포인트를 사용해 공지 목록을 가져옵니다.
 */
@Component
class WutheringWavesScraper(
    private val client: WutheringWavesClient
) : AbstractGameScraper() {

    companion object {
        private const val GAME_NAME = "명조"
        private const val BASE_URL = "https://wutheringwaves.kurogames.com"
        private const val NOTICE_URL = "$BASE_URL/kr/main#news"
        private const val DETAIL_PATH = "$BASE_URL/kr/news/detail/"
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        private val ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
    }

    override fun getGameName(): String = GAME_NAME

    override fun getBaseUrl(): String = BASE_URL

    override fun getNoticeUrl(): String = NOTICE_URL

    override fun scrapeNotices(): List<GameNotice> {
        logger.info("Starting scraping for game: {} via JSON API", getGameName())

        val game = getOrCreateGame()
        val articleTypes = client.fetchArticleTypes()
        val typeMap = articleTypes.associate { it.contentId!! to it.contentLabel!!.trim() }

        val rawArticles = client.fetchArticles()

        val notices = rawArticles
            .filter { it.articleId != null && it.articleTitle != null }
            .filter { it.articleType != null && typeMap.containsKey(it.articleType) }
            .distinctBy { it.articleId }
            .mapNotNull { article ->
                try {
                    val publishedDate = parseDate(article.startTime)
                    val summary = buildSummary(
                        category = typeMap[article.articleType!!],
                        desc = article.articleDesc,
                        content = article.articleContent
                    )
                    createGameNotice(
                        game = game,
                        title = article.articleTitle!!,
                        url = DETAIL_PATH + article.articleId!!,
                        publishedDate = publishedDate,
                        summary = summary
                    )
                } catch (ex: Exception) {
                    logger.warn("Skipping article {} due to parse failure: {}", article.articleId, ex.message)
                    null
                }
            }
            .sortedByDescending { it.publishedDate }

        logger.info("Successfully scraped {} notices for game: {}", notices.size, getGameName())
        return notices
    }

    override fun parseNotices(document: Document, game: Game): List<GameNotice> {
        // JSON 기반 수집을 사용하므로 HTML 파싱은 수행하지 않습니다.
        return emptyList()
    }

    private fun parseDate(raw: String?): LocalDateTime {
        if (raw.isNullOrBlank()) {
            return LocalDateTime.now(ZONE_ID)
        }
        return LocalDateTime.parse(raw.trim(), DATE_FORMATTER)
    }

    private fun buildSummary(category: String?, desc: String?, content: String?): String? {
        val base = when {
            !desc.isNullOrBlank() -> desc
            !content.isNullOrBlank() -> Jsoup.parse(content).text()
            else -> null
        }
        val cleaned = base?.let { parsingUtil.cleanText(it) }?.takeIf { it.isNotBlank() }
        return when {
            cleaned == null -> category?.takeIf { it.isNotBlank() }
            category.isNullOrBlank() -> cleaned
            else -> "[$category] $cleaned"
        }
    }
}
