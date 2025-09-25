package com.kiki.scraper

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*

/**
 * HTML 파싱을 위한 유틸리티 클래스
 * 공통적으로 사용되는 파싱 로직을 제공합니다.
 */
@Component
class ParsingUtil {
    
    private val logger = LoggerFactory.getLogger(ParsingUtil::class.java)
    
    /**
     * 텍스트를 정리합니다 (공백 제거, 특수문자 정리 등)
     * @param text 정리할 텍스트
     * @return 정리된 텍스트
     */
    fun cleanText(text: String?): String {
        return text?.trim()
            ?.replace(Regex("\\s+"), " ")
            ?.replace(Regex("[\\r\\n\\t]+"), " ")
            ?: ""
    }
    
    /**
     * 상대 URL을 절대 URL로 변환합니다.
     * @param baseUrl 기본 URL
     * @param relativeUrl 상대 URL
     * @return 절대 URL
     */
    fun resolveUrl(baseUrl: String, relativeUrl: String): String {
        return when {
            relativeUrl.startsWith("http://") || relativeUrl.startsWith("https://") -> relativeUrl
            relativeUrl.startsWith("//") -> "https:$relativeUrl"
            relativeUrl.startsWith("/") -> {
                val base = if (baseUrl.endsWith("/")) baseUrl.dropLast(1) else baseUrl
                "$base$relativeUrl"
            }
            else -> {
                val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
                "$base$relativeUrl"
            }
        }
    }
    
    /**
     * 다양한 날짜 형식을 파싱하여 LocalDateTime으로 변환합니다.
     * @param dateText 날짜 텍스트
     * @param patterns 시도할 날짜 패턴 목록
     * @return 파싱된 LocalDateTime, 실패 시 현재 시간
     */
    fun parseDate(dateText: String, patterns: List<String>): LocalDateTime {
        val cleanedText = cleanText(dateText)
        
        for (pattern in patterns) {
            try {
                val formatter = DateTimeFormatter.ofPattern(pattern, Locale.KOREAN)
                
                // LocalDate 패턴인 경우 시간을 00:00:00으로 설정
                return if (pattern.contains("H") || pattern.contains("m") || pattern.contains("s")) {
                    LocalDateTime.parse(cleanedText, formatter)
                } else {
                    // 년도가 없는 패턴인 경우 현재 년도를 사용
                    if (!pattern.contains("y")) {
                        val currentYear = LocalDateTime.now().year
                        
                        // 한국어 날짜 형식 처리
                        if (pattern.contains("월") && pattern.contains("일")) {
                            val fullDateText = "${currentYear}년 $cleanedText"
                            val fullPattern = "yyyy년 $pattern"
                            val fullFormatter = DateTimeFormatter.ofPattern(fullPattern, Locale.KOREAN)
                            try {
                                val localDate = java.time.LocalDate.parse(fullDateText, fullFormatter)
                                return localDate.atStartOfDay()
                            } catch (e: DateTimeParseException) {
                                // 원래 패턴으로 다시 시도
                            }
                        } else {
                            // 일반적인 숫자 형식
                            val fullDateText = "$currentYear.$cleanedText"
                            val fullPattern = "yyyy.$pattern"
                            val fullFormatter = DateTimeFormatter.ofPattern(fullPattern, Locale.KOREAN)
                            try {
                                val localDate = java.time.LocalDate.parse(fullDateText, fullFormatter)
                                return localDate.atStartOfDay()
                            } catch (e: DateTimeParseException) {
                                // 원래 패턴으로 다시 시도
                            }
                        }
                    }
                    
                    val localDate = java.time.LocalDate.parse(cleanedText, formatter)
                    localDate.atStartOfDay()
                }
            } catch (e: DateTimeParseException) {
                logger.debug("Failed to parse date '{}' with pattern '{}'", cleanedText, pattern)
            }
        }
        
        logger.warn("Failed to parse date: '{}', using current time", cleanedText)
        return LocalDateTime.now()
    }
    
    /**
     * 요소에서 텍스트를 안전하게 추출합니다.
     * @param element 추출할 요소
     * @return 추출된 텍스트, 요소가 null이면 빈 문자열
     */
    fun safeText(element: Element?): String {
        return element?.text()?.let { cleanText(it) } ?: ""
    }
    
    /**
     * 요소에서 속성값을 안전하게 추출합니다.
     * @param element 추출할 요소
     * @param attribute 속성 이름
     * @return 추출된 속성값, 요소가 null이거나 속성이 없으면 빈 문자열
     */
    fun safeAttr(element: Element?, attribute: String): String {
        return element?.attr(attribute)?.let { cleanText(it) } ?: ""
    }
    
    /**
     * 텍스트를 요약합니다 (최대 길이 제한)
     * @param text 요약할 텍스트
     * @param maxLength 최대 길이 (기본값: 200)
     * @return 요약된 텍스트
     */
    fun summarizeText(text: String, maxLength: Int = 200): String {
        val cleaned = cleanText(text)
        return if (cleaned.length <= maxLength) {
            cleaned
        } else {
            cleaned.substring(0, maxLength - 3) + "..."
        }
    }
}