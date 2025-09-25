package com.kiki.scraper

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class HttpClientUtilTest {
    
    private val httpClientUtil = HttpClientUtil()
    
    @Test
    fun `should throw ScrapingException for invalid URL`() {
        assertThrows<ScrapingException> {
            httpClientUtil.fetchDocument("invalid-url")
        }
    }
    
    @Test
    fun `should throw ScrapingException for non-existent domain`() {
        assertThrows<ScrapingException> {
            httpClientUtil.fetchDocument("https://non-existent-domain-12345.com")
        }
    }
}