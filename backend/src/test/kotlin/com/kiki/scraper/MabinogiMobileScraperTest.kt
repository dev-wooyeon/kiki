package com.kiki.scraper

import com.kiki.entity.Game
import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class MabinogiMobileScraperTest : MabinogiMobileScraper() {

    @BeforeEach
    fun setUp() {
        httpClientUtil = HttpClientUtil()
        parsingUtil = ParsingUtil()
    }

    private fun mockGame() = Game(
        id = 1,
        name = "마비노기 모바일",
        baseUrl = "https://mabinogimobile.nexon.com",
        scraperClass = "MabinogiMobileScraper"
    )

    @Test
    fun `should parse notices from modern board structure`() {
        val mockHtml = """
            <html>
              <body>
                <div class="list_area" data-mm-boardlist>
                  <ul class="list">
                    <li class="item" data-threadid="3150274">
                      <div class="order_1">
                        <div class="type"><span>안내</span></div>
                        <a class="title"><span>빛의 신화! 팔라딘 업데이트 안내</span></a>
                      </div>
                      <div class="order_2">
                        <div class="sub_info">
                          <div class="date"><span>2025.09.25</span></div>
                        </div>
                      </div>
                    </li>
                    <li class="item" data-threadid="3150261">
                      <div class="order_1">
                        <div class="type"><span>점검</span></div>
                        <a class="title"><span>Google Play Pass 프로모션 안내</span></a>
                      </div>
                      <div class="order_2">
                        <div class="sub_info">
                          <div class="date"><span>2025-09-24</span></div>
                        </div>
                      </div>
                    </li>
                  </ul>
                </div>
              </body>
            </html>
        """.trimIndent()

        val notices = parseNotices(Jsoup.parse(mockHtml), mockGame())

        assertEquals(2, notices.size)
        val first = notices[0]
        assertEquals("빛의 신화! 팔라딘 업데이트 안내", first.title)
        assertEquals("https://mabinogimobile.nexon.com/News/Notice/3150274", first.url)
        assertTrue(first.summary?.contains("안내") == true)
        assertEquals(25, first.publishedDate.dayOfMonth)

        val second = notices[1]
        assertEquals("Google Play Pass 프로모션 안내", second.title)
        assertEquals(24, second.publishedDate.dayOfMonth)
    }

    @Test
    fun `should fall back to legacy selectors`() {
        val mockHtml = """
            <html>
              <body>
                <div class="board-list">
                  <div class="board-item">
                    <a href="/notice/123" class="board-title">신규 캐릭터 업데이트</a>
                    <span class="board-date">2024/01/14</span>
                    <div class="board-content">새로운 캐릭터가 추가되었습니다.</div>
                  </div>
                </div>
              </body>
            </html>
        """.trimIndent()

        val notices = parseNotices(Jsoup.parse(mockHtml), mockGame())
        assertEquals(1, notices.size)
        assertEquals("https://mabinogimobile.nexon.com/notice/123", notices.first().url)
    }

    @Test
    fun `should handle empty list gracefully`() {
        val notices = parseNotices(Jsoup.parse("<html><body></body></html>"), mockGame())
        assertTrue(notices.isEmpty())
    }
}
