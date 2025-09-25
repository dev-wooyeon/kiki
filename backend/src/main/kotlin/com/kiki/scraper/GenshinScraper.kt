package com.kiki.scraper

import com.kiki.entity.Game
import com.kiki.entity.GameNotice
import org.jsoup.nodes.Document
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * 원신(Genshin) 공지사항 스크래퍼
 * HoYoverse 공식 사이트에서 원신 공지사항을 스크래핑합니다.
 */
@Component
class GenshinScraper : AbstractGameScraper() {
    
    companion object {
        private const val GAME_NAME = "원신"
        private const val BASE_URL = "https://genshin.hoyoverse.com"
        private const val NOTICE_URL = "https://genshin.hoyoverse.com/ko/news"
        
        // 날짜 파싱 패턴들
        private val DATE_PATTERNS = listOf(
            "yyyy.MM.dd",
            "yyyy-MM-dd",
            "yyyy/MM/dd",
            "MM.dd",
            "MM-dd",
            "MM/dd",
            "yyyy년 MM월 dd일",
            "MM월 dd일"
        )
    }
    
    override fun getGameName(): String = GAME_NAME
    
    override fun getBaseUrl(): String = BASE_URL
    
    override fun getNoticeUrl(): String = NOTICE_URL
    
    override fun parseNotices(document: Document, game: Game): List<GameNotice> {
        val notices = mutableListOf<GameNotice>()
        
        try {
            // HoYoverse 사이트의 공지사항 목록 선택자 (실제 사이트 구조에 따라 조정 필요)
            val noticeElements = document.select(".news-list .news-item, .article-list .article-item, .post-list .post-item")
            
            if (noticeElements.isEmpty()) {
                // 대안 선택자들 시도
                val alternativeSelectors = listOf(
                    ".list-item",
                    ".news-card",
                    ".article-card",
                    ".post-card",
                    ".content-item",
                    ".notice-item",
                    ".info-item"
                )
                
                for (selector in alternativeSelectors) {
                    val elements = document.select(selector)
                    if (elements.isNotEmpty()) {
                        logger.debug("Found {} notice elements using selector: {}", elements.size, selector)
                        return parseNoticeElements(elements, game)
                    }
                }
                
                logger.warn("No notice elements found for {}. Available selectors might have changed.", GAME_NAME)
                return notices
            }
            
            return parseNoticeElements(noticeElements, game)
            
        } catch (e: Exception) {
            logger.error("Error parsing {} notices", GAME_NAME, e)
            throw ScrapingException("Failed to parse ${GAME_NAME} notices", e)
        }
    }
    
    private fun parseNoticeElements(elements: org.jsoup.select.Elements, game: Game): List<GameNotice> {
        val notices = mutableListOf<GameNotice>()
        
        elements.forEach { element ->
            try {
                // 제목 추출 (HoYoverse 사이트 구조에 맞게)
                val titleElement = element.selectFirst("a, .title, .subject, .news-title, .article-title, .post-title, h3, h4")
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
                
                // 날짜 추출 (HoYoverse 사이트의 다양한 날짜 형식 지원)
                val dateElement = element.selectFirst(".date, .time, .publish-time, .created-time, .post-date, .news-date, .article-date")
                val dateText = parsingUtil.safeText(dateElement)
                val publishedDate = if (dateText.isNotBlank()) {
                    parsingUtil.parseDate(dateText, DATE_PATTERNS)
                } else {
                    LocalDateTime.now()
                }
                
                // 요약 추출 (선택사항)
                val summaryElement = element.selectFirst(".summary, .excerpt, .content, .description, .news-content, .article-content")
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
