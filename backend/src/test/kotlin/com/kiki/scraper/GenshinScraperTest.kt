package com.kiki.scraper

import com.kiki.entity.Game
import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles("test")
class GenshinScraperTest : GenshinScraper() {
    
    @BeforeEach
    fun setUp() {
        httpClientUtil = HttpClientUtil()
        parsingUtil = ParsingUtil()
    }
    
    @Test
    fun `should return correct game information`() {
        assertEquals("원신", getGameName())
        assertEquals("https://genshin.hoyoverse.com", getBaseUrl())
        assertEquals("https://genshin.hoyoverse.com/ko/news", getNoticeUrl())
    }
    
    @Test
    fun `should parse notices from mock HTML with news-list structure`() {
        val mockHtml = """
            <html>
                <body>
                    <div class="news-list">
                        <div class="news-item">
                            <a href="/news/123" class="news-title">버전 4.5 업데이트 공지</a>
                            <span class="publish-time">2024.01.15</span>
                            <div class="news-content">새로운 버전이 출시되었습니다.</div>
                        </div>
                        <div class="news-item">
                            <a href="/news/124" class="news-title">이벤트 안내</a>
                            <span class="publish-time">2024년 01월 14일</span>
                            <div class="news-content">특별 이벤트가 시작됩니다.</div>
                        </div>
                    </div>
                </body>
            </html>
        """.trimIndent()
        
        val document = Jsoup.parse(mockHtml)
        val game = Game(
            id = 1,
            name = "원신",
            baseUrl = "https://genshin.hoyoverse.com",
            scraperClass = "GenshinScraper"
        )
        
        val notices = parseNotices(document, game)
        
        assertEquals(2, notices.size)
        
        val firstNotice = notices[0]
        assertEquals("버전 4.5 업데이트 공지", firstNotice.title)
        assertEquals("https://genshin.hoyoverse.com/news/123", firstNotice.url)
        assertEquals(2024, firstNotice.publishedDate.year)
        assertEquals(1, firstNotice.publishedDate.monthValue)
        assertEquals(15, firstNotice.publishedDate.dayOfMonth)
        assertEquals("새로운 버전이 출시되었습니다.", firstNotice.summary)
        
        val secondNotice = notices[1]
        assertEquals("이벤트 안내", secondNotice.title)
        assertEquals("https://genshin.hoyoverse.com/news/124", secondNotice.url)
        assertEquals("특별 이벤트가 시작됩니다.", secondNotice.summary)
    }
    
    @Test
    fun `should parse notices from article structure`() {
        val mockHtml = """
            <html>
                <body>
                    <div class="article-list">
                        <div class="article-item">
                            <h3><a href="/article/456">캐릭터 밸런스 조정</a></h3>
                            <time class="article-date">2024-01-16</time>
                            <p class="article-content">캐릭터 밸런스가 조정되었습니다.</p>
                        </div>
                    </div>
                </body>
            </html>
        """.trimIndent()
        
        val document = Jsoup.parse(mockHtml)
        val game = Game(
            id = 1,
            name = "원신",
            baseUrl = "https://genshin.hoyoverse.com",
            scraperClass = "GenshinScraper"
        )
        
        val notices = parseNotices(document, game)
        
        assertEquals(1, notices.size)
        
        val notice = notices[0]
        assertEquals("캐릭터 밸런스 조정", notice.title)
        assertEquals("https://genshin.hoyoverse.com/article/456", notice.url)
        assertEquals(2024, notice.publishedDate.year)
        assertEquals(1, notice.publishedDate.monthValue)
        assertEquals(16, notice.publishedDate.dayOfMonth)
        assertEquals("캐릭터 밸런스가 조정되었습니다.", notice.summary)
    }
    
    @Test
    fun `should handle Korean date formats`() {
        val mockHtml = """
            <html>
                <body>
                    <div class="news-list">
                        <div class="news-item">
                            <a href="/news/123" class="title">공지 1</a>
                            <span class="date">2024년 01월 15일</span>
                        </div>
                        <div class="news-item">
                            <a href="/news/124" class="title">공지 2</a>
                            <span class="date">01월 14일</span>
                        </div>
                        <div class="news-item">
                            <a href="/news/125" class="title">공지 3</a>
                            <span class="date">2024/01/13</span>
                        </div>
                    </div>
                </body>
            </html>
        """.trimIndent()
        
        val document = Jsoup.parse(mockHtml)
        val game = Game(
            id = 1,
            name = "원신",
            baseUrl = "https://genshin.hoyoverse.com",
            scraperClass = "GenshinScraper"
        )
        
        val notices = parseNotices(document, game)
        
        assertEquals(3, notices.size)
        
        // 각 공지사항의 날짜가 올바르게 파싱되었는지 확인
        assertEquals(15, notices[0].publishedDate.dayOfMonth)
        assertEquals(14, notices[1].publishedDate.dayOfMonth)
        assertEquals(13, notices[2].publishedDate.dayOfMonth)
    }
    
    @Test
    fun `should handle alternative selectors`() {
        val mockHtml = """
            <html>
                <body>
                    <div class="content-item">
                        <h4><a href="/info/789">서버 점검 안내</a></h4>
                        <span class="time">2024.01.17</span>
                        <div class="description">정기 점검이 예정되어 있습니다.</div>
                    </div>
                </body>
            </html>
        """.trimIndent()
        
        val document = Jsoup.parse(mockHtml)
        val game = Game(
            id = 1,
            name = "원신",
            baseUrl = "https://genshin.hoyoverse.com",
            scraperClass = "GenshinScraper"
        )
        
        val notices = parseNotices(document, game)
        
        assertEquals(1, notices.size)
        
        val notice = notices[0]
        assertEquals("서버 점검 안내", notice.title)
        assertEquals("https://genshin.hoyoverse.com/info/789", notice.url)
        assertEquals(17, notice.publishedDate.dayOfMonth)
        assertEquals("정기 점검이 예정되어 있습니다.", notice.summary)
    }
    
    @Test
    fun `should handle empty notice list gracefully`() {
        val mockHtml = """
            <html>
                <body>
                    <div class="content">
                        <p>공지사항이 없습니다.</p>
                    </div>
                </body>
            </html>
        """.trimIndent()
        
        val document = Jsoup.parse(mockHtml)
        val game = Game(
            id = 1,
            name = "원신",
            baseUrl = "https://genshin.hoyoverse.com",
            scraperClass = "GenshinScraper"
        )
        
        val notices = parseNotices(document, game)
        
        assertEquals(0, notices.size)
    }
    
    @Test
    fun `should skip notices with empty title or URL`() {
        val mockHtml = """
            <html>
                <body>
                    <div class="news-list">
                        <div class="news-item">
                            <a href="/news/123" class="title">정상 공지사항</a>
                            <span class="date">2024.01.15</span>
                        </div>
                        <div class="news-item">
                            <a href="" class="title">URL이 없는 공지</a>
                            <span class="date">2024.01.14</span>
                        </div>
                        <div class="news-item">
                            <a href="/news/125" class="title"></a>
                            <span class="date">2024.01.13</span>
                        </div>
                    </div>
                </body>
            </html>
        """.trimIndent()
        
        val document = Jsoup.parse(mockHtml)
        val game = Game(
            id = 1,
            name = "원신",
            baseUrl = "https://genshin.hoyoverse.com",
            scraperClass = "GenshinScraper"
        )
        
        val notices = parseNotices(document, game)
        
        // 정상적인 공지사항 1개만 파싱되어야 함
        assertEquals(1, notices.size)
        assertEquals("정상 공지사항", notices[0].title)
    }
    
    @Test
    fun `should use current time when date parsing fails`() {
        val mockHtml = """
            <html>
                <body>
                    <div class="news-list">
                        <div class="news-item">
                            <a href="/news/123" class="title">날짜 없는 공지</a>
                            <span class="date">invalid date format</span>
                        </div>
                    </div>
                </body>
            </html>
        """.trimIndent()
        
        val document = Jsoup.parse(mockHtml)
        val game = Game(
            id = 1,
            name = "원신",
            baseUrl = "https://genshin.hoyoverse.com",
            scraperClass = "GenshinScraper"
        )
        
        val before = LocalDateTime.now()
        val notices = parseNotices(document, game)
        val after = LocalDateTime.now()
        
        assertEquals(1, notices.size)
        val notice = notices[0]
        assertTrue(notice.publishedDate.isAfter(before.minusSeconds(1)))
        assertTrue(notice.publishedDate.isBefore(after.plusSeconds(1)))
    }
}
