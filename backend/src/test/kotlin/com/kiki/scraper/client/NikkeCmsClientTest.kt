package com.kiki.scraper.client

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.kiki.scraper.HttpClientUtil
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class NikkeCmsClientTest {

    private val httpClientUtil: HttpClientUtil = mockk()
    private val objectMapper = jacksonObjectMapper()

    private lateinit var client: NikkeCmsClient

    @BeforeEach
    fun setUp() {
        clearMocks(httpClientUtil)
        client = NikkeCmsClient(
            httpClientUtil = httpClientUtil,
            objectMapper = objectMapper,
            baseUrl = "https://na-community.playerinfinite.com",
            gameId = "16",
            areaId = "na",
            source = "pc_web",
            language = "ko",
            pageSize = 10,
            maxAgeHours = 72,
            detailBaseUrl = "https://nikke-kr.com/newsdetail.html"
        )
    }

    @Test
    fun `fetchLatestNotices should parse label and content responses`() {
        val labelResponse = """
            {
              "code":0,
              "msg":"succ",
              "data":{
                "primary_label_list":[
                  {
                    "label_id":309,
                    "label_name":"news",
                    "raw_label_name":"official_news",
                    "secondary_label_list":[
                      {
                        "label_id":892,
                        "label_name":"공지사항",
                        "raw_label_name":"NOTICE"
                      }
                    ]
                  }
                ]
              }
            }
        """.trimIndent()

        val publishedEpoch = Instant.now().epochSecond
        val contentResponse = """
            {
              "code":0,
              "msg":"succ",
              "data":{
                "info_content":[
                  {
                    "content_id":"abc123",
                    "title":"테스트 공지",
                    "pub_timestamp":"$publishedEpoch",
                    "content_desc":"상세 설명"
                  }
                ]
              }
            }
        """.trimIndent()

        every { httpClientUtil.postJson(any(), any(), any()) } returnsMany listOf(labelResponse, contentResponse)

        val notices = client.fetchLatestNotices()

        assertEquals(1, notices.size)
        val notice = notices.first()
        assertEquals("abc123", notice.contentId)
        assertEquals("테스트 공지", notice.title)
        assertEquals("상세 설명", notice.summary)
        assertTrue(notice.url.contains("content_id=abc123"))
    }

    @Test
    fun `fetchLatestNotices should filter notices older than maxAgeHours`() {
        val labelResponse = """
            {
              "code":0,
              "msg":"succ",
              "data":{
                "primary_label_list":[
                  {
                    "label_id":309,
                    "label_name":"news",
                    "raw_label_name":"official_news",
                    "secondary_label_list":[
                      {
                        "label_id":892,
                        "label_name":"공지사항",
                        "raw_label_name":"NOTICE"
                      }
                    ]
                  }
                ]
              }
            }
        """.trimIndent()

        val oldEpoch = LocalDateTime.now()
            .minusDays(5)
            .atZone(ZoneId.of("Asia/Seoul"))
            .toEpochSecond()
        val contentResponse = """
            {
              "code":0,
              "msg":"succ",
              "data":{
                "info_content":[
                  {
                    "content_id":"old001",
                    "title":"오래된 공지",
                    "pub_timestamp":"$oldEpoch",
                    "content_desc":"과거 공지"
                  }
                ]
              }
            }
        """.trimIndent()

        every { httpClientUtil.postJson(any(), any(), any()) } returnsMany listOf(labelResponse, contentResponse)

        val strictClient = NikkeCmsClient(
            httpClientUtil = httpClientUtil,
            objectMapper = objectMapper,
            baseUrl = "https://na-community.playerinfinite.com",
            gameId = "16",
            areaId = "na",
            source = "pc_web",
            language = "ko",
            pageSize = 10,
            maxAgeHours = 24,
            detailBaseUrl = "https://nikke-kr.com/newsdetail.html"
        )

        val notices = strictClient.fetchLatestNotices()

        assertTrue(notices.isEmpty())
    }
}
