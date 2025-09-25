package com.kiki.scraper

import com.kiki.entity.Game
import com.kiki.entity.GameNotice
import org.jsoup.nodes.Document
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * 마비노기 모바일 공지사항 스크래퍼
 * 넥슨 공식 사이트에서 마비노기 모바일 공지사항을 스크래핑합니다.
 */
@Component
class MabinogiMobileScraper : AbstractGameScraper() {
    
    companion object {
        private const val GAME_NAME = "마비노기 모바일"
        private const val BASE_URL = "https://mabinogimobile.nexon.com"
        private const val NOTICE_URL = "https://mabinogimobile.nexon.com/News/Notice"
        
        // 날짜 파싱 패턴들
        private val DATE_PATTERNS = listOf(
            "yyyy.MM.dd",
            "yyyy-MM-dd",
            "yyyy/MM/dd",
            "MM.dd",
            "MM-dd",
            "MM/dd"
        )
    }
    
    override fun getGameName(): String = GAME_NAME
    
    override fun getBaseUrl(): String = BASE_URL
    
    override fun getNoticeUrl(): String = NOTICE_URL
    
    override fun parseNotices(document: Document, game: Game): List<GameNotice> {
        val notices = mutableListOf<GameNotice>()
        
        try {
            val modernBoard = document.select("div.list_area[data-mm-boardlist] ul.list > li.item[data-threadid]")
            if (modernBoard.isNotEmpty()) {
                logger.debug("Detected modern board list structure with {} items", modernBoard.size)
                return parseModernBoard(modernBoard, game)
            }

            // 기존 구조 대응 (백업)
            val legacyElements = document.select(
                ".board-list .board-item, .notice-list .notice-item, .news-list .news-item, " +
                    ".list-item, .board-row, .notice-row, tr.notice, .post-item, .article-item, tbody tr"
            )

            if (legacyElements.isEmpty()) {
                logger.warn("No notice elements found for {}. Available selectors might have changed.", GAME_NAME)
                return notices
            }

            return parseNoticeElements(legacyElements, game)

        } catch (e: Exception) {
            logger.error("Error parsing {} notices", GAME_NAME, e)
            throw ScrapingException("Failed to parse ${GAME_NAME} notices", e)
        }
    }

    private fun parseModernBoard(elements: org.jsoup.select.Elements, game: Game): List<GameNotice> {
        val notices = mutableListOf<GameNotice>()

        elements.forEach { element ->
            try {
                val titleElement = element.selectFirst("a.title span") ?: element.selectFirst("a.title")
                val title = parsingUtil.safeText(titleElement)
                if (title.isBlank()) {
                    logger.debug("Skipping modern board element with empty title")
                    return@forEach
                }

                val threadId = parsingUtil.safeAttr(element, "data-threadid")
                val detailPath = if (threadId.isNotBlank()) "/News/Notice/$threadId" else ""

                val dateElement = element.selectFirst(".sub_info .date span")
                val dateText = parsingUtil.safeText(dateElement)
                val publishedDate = if (dateText.isNotBlank()) {
                    parsingUtil.parseDate(dateText, DATE_PATTERNS)
                } else {
                    LocalDateTime.now()
                }

                val category = parsingUtil.safeText(element.selectFirst(".type span"))
                val summaryText = if (category.isNotBlank()) "[$category] $title" else title

                val notice = createGameNotice(
                    game = game,
                    title = title,
                    url = detailPath,
                    publishedDate = publishedDate,
                    summary = summaryText
                )

                notices.add(notice)
                logger.debug("Parsed modern {} notice: {} ({})", GAME_NAME, title, publishedDate)

            } catch (e: Exception) {
                logger.warn("Failed to parse modern board {} notice element", GAME_NAME, e)
            }
        }

        logger.info("Successfully parsed {} {} notices (modern board)", notices.size, GAME_NAME)
        return notices
    }

    private fun parseNoticeElements(elements: org.jsoup.select.Elements, game: Game): List<GameNotice> {
        val notices = mutableListOf<GameNotice>()
        
        elements.forEach { element ->
            try {
                // 제목 추출 (넥슨 사이트 구조에 맞게)
                val titleElement = element.selectFirst("a, .title, .subject, .board-title, td.title, .article-title")
                val title = parsingUtil.safeText(titleElement)
                
                if (title.isBlank()) {
                    logger.debug("Skipping element with empty title")
                    return@forEach
                }
                
                // URL 추출
                val linkElement = element.selectFirst("a") ?: titleElement
                val relativeUrl = parsingUtil.safeAttr(linkElement, "href")
                
                if (relativeUrl.isBlank()) {
                    logger.debug("Skipping notice with empty URL: {}", title)
                    return@forEach
                }
                
                // 날짜 추출 (넥슨 사이트의 다양한 날짜 형식 지원)
                val dateElement = element.selectFirst(".date, .reg-date, .created-date, .post-date, .board-date, td.date, time")
                val dateText = parsingUtil.safeText(dateElement)
                val publishedDate = if (dateText.isNotBlank()) {
                    parsingUtil.parseDate(dateText, DATE_PATTERNS)
                } else {
                    LocalDateTime.now()
                }
                
                // 요약 추출 (선택사항)
                val summaryElement = element.selectFirst(".summary, .excerpt, .content, .description, .board-content")
                val summary = summaryElement?.let { parsingUtil.safeText(it) }
                
                val notice = createGameNotice(
                    game = game,
                    title = title,
                    url = relativeUrl,
                    publishedDate = publishedDate,
                    summary = summary
                )
                
                notices.add(notice)
                logger.debug("Parsed {} notice: {} ({})", GAME_NAME, title, publishedDate)
                
            } catch (e: Exception) {
                logger.warn("Failed to parse individual {} notice element", GAME_NAME, e)
            }
        }
        
        logger.info("Successfully parsed {} {} notices", notices.size, GAME_NAME)
        return notices
    }
}
