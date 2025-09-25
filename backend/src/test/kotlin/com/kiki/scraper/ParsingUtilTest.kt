package com.kiki.scraper

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles("test")
class ParsingUtilTest {
    
    private val parsingUtil = ParsingUtil()
    
    @Test
    fun `should clean text properly`() {
        val input = "  Hello   World  \n\t  "
        val expected = "Hello World"
        assertEquals(expected, parsingUtil.cleanText(input))
    }
    
    @Test
    fun `should handle null text`() {
        assertEquals("", parsingUtil.cleanText(null))
    }
    
    @Test
    fun `should resolve relative URLs correctly`() {
        val baseUrl = "https://example.com"
        
        // Absolute URL should remain unchanged
        assertEquals("https://other.com/page", parsingUtil.resolveUrl(baseUrl, "https://other.com/page"))
        
        // Relative URL starting with /
        assertEquals("https://example.com/page", parsingUtil.resolveUrl(baseUrl, "/page"))
        
        // Protocol-relative URL
        assertEquals("https://other.com/page", parsingUtil.resolveUrl(baseUrl, "//other.com/page"))
        
        // Relative URL without /
        assertEquals("https://example.com/page", parsingUtil.resolveUrl(baseUrl, "page"))
    }
    
    @Test
    fun `should parse Korean date formats`() {
        val patterns = listOf("yyyy.MM.dd", "yyyy-MM-dd HH:mm")
        
        val result1 = parsingUtil.parseDate("2024.01.15", patterns)
        assertEquals(2024, result1.year)
        assertEquals(1, result1.monthValue)
        assertEquals(15, result1.dayOfMonth)
        
        val result2 = parsingUtil.parseDate("2024-01-15 14:30", patterns)
        assertEquals(2024, result2.year)
        assertEquals(1, result2.monthValue)
        assertEquals(15, result2.dayOfMonth)
        assertEquals(14, result2.hour)
        assertEquals(30, result2.minute)
    }
    
    @Test
    fun `should return current time for invalid date`() {
        val patterns = listOf("yyyy.MM.dd")
        val before = LocalDateTime.now()
        val result = parsingUtil.parseDate("invalid-date", patterns)
        val after = LocalDateTime.now()
        
        assertTrue(result.isAfter(before.minusSeconds(1)))
        assertTrue(result.isBefore(after.plusSeconds(1)))
    }
    
    @Test
    fun `should summarize text correctly`() {
        val longText = "A".repeat(300)
        val summary = parsingUtil.summarizeText(longText, 100)
        
        assertEquals(100, summary.length)
        assertTrue(summary.endsWith("..."))
    }
    
    @Test
    fun `should not summarize short text`() {
        val shortText = "Short text"
        val summary = parsingUtil.summarizeText(shortText, 100)
        
        assertEquals(shortText, summary)
    }
}