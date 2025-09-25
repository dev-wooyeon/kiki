package com.kiki.scraper

import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.slf4j.LoggerFactory
import java.io.IOException
import kotlin.math.pow

/**
 * HTTP 클라이언트 유틸리티 클래스
 * 재시도 로직과 지수 백오프를 포함한 웹 페이지 요청 기능을 제공합니다.
 */
import org.springframework.stereotype.Component

@Component
class HttpClientUtil {
    
    private val logger = LoggerFactory.getLogger(HttpClientUtil::class.java)
    
    companion object {
        private const val MAX_RETRIES = 3
        private const val BASE_DELAY_MS = 1000L
        private const val TIMEOUT_MS = 30000
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
    }
    
    /**
     * 지수 백오프를 사용하여 웹 페이지를 가져옵니다.
     * @param url 요청할 URL
     * @return Jsoup Document 객체
     * @throws ScrapingException 모든 재시도가 실패한 경우
     */
    fun fetchDocument(url: String): Document {
        var lastException: Exception? = null
        
        repeat(MAX_RETRIES) { attempt ->
            try {
                logger.debug("Fetching URL: {} (attempt: {})", url, attempt + 1)
                
                val document = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .method(Connection.Method.GET)
                    .followRedirects(true)
                    .get()
                
                logger.debug("Successfully fetched URL: {}", url)
                return document
                
            } catch (e: Exception) {
                lastException = e
                logger.warn("Failed to fetch URL: {} (attempt: {}), error: {}", url, attempt + 1, e.message)
                
                if (attempt < MAX_RETRIES - 1) {
                    val delay = calculateDelay(attempt)
                    logger.debug("Retrying in {} ms", delay)
                    Thread.sleep(delay)
                }
            }
        }
        
        throw ScrapingException(
            "Failed to fetch URL after $MAX_RETRIES attempts: $url",
            lastException
        )
    }

    /**
     * JSON 응답을 문자열로 가져옵니다.
     * @param url 요청할 URL
     * @return 응답 본문 문자열
     */
    fun fetchJson(url: String): String {
        var lastException: Exception? = null

        repeat(MAX_RETRIES) { attempt ->
            try {
                logger.debug("Fetching JSON URL: {} (attempt: {})", url, attempt + 1)

                val response = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .ignoreContentType(true)
                    .method(Connection.Method.GET)
                    .followRedirects(true)
                    .execute()

                if (response.statusCode() in 200..299) {
                    logger.debug("Successfully fetched JSON URL: {}", url)
                    return response.body()
                }

                throw IOException("Unexpected status code ${response.statusCode()} for URL: $url")

            } catch (e: Exception) {
                lastException = e
                logger.warn(
                    "Failed to fetch JSON URL: {} (attempt: {}), error: {}",
                    url,
                    attempt + 1,
                    e.message
                )

                if (attempt < MAX_RETRIES - 1) {
                    val delay = calculateDelay(attempt)
                    logger.debug("Retrying in {} ms", delay)
                    Thread.sleep(delay)
                }
            }
        }

        throw ScrapingException(
            "Failed to fetch JSON after $MAX_RETRIES attempts: $url",
            lastException
        )
    }

    /**
     * JSON POST 요청을 실행하여 응답 본문을 문자열로 반환합니다.
     * @param url 요청할 URL
     * @param headers 요청 헤더
     * @param body JSON 문자열 본문
     * @return 응답 본문 문자열
     */
    fun postJson(url: String, headers: Map<String, String>, body: String): String {
        var lastException: Exception? = null

        repeat(MAX_RETRIES) { attempt ->
            try {
                logger.debug(
                    "Posting JSON to URL: {} (attempt: {}), body length: {}",
                    url,
                    attempt + 1,
                    body.length
                )

                val connection = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .ignoreContentType(true)
                    .method(Connection.Method.POST)
                    .header("Content-Type", "application/json;charset=utf-8")
                    .requestBody(body)

                headers.forEach { (key, value) ->
                    if (!key.equals("Content-Type", ignoreCase = true)) {
                        connection.header(key, value)
                    }
                }

                val response = connection.execute()

                if (response.statusCode() in 200..299) {
                    logger.debug("Successfully posted JSON to URL: {}", url)
                    return response.body()
                }

                throw IOException("Unexpected status code ${response.statusCode()} for URL: $url")

            } catch (e: Exception) {
                lastException = e
                logger.warn(
                    "Failed to post JSON to URL: {} (attempt: {}), error: {}",
                    url,
                    attempt + 1,
                    e.message
                )

                if (attempt < MAX_RETRIES - 1) {
                    val delay = calculateDelay(attempt)
                    logger.debug("Retrying in {} ms", delay)
                    Thread.sleep(delay)
                }
            }
        }

        throw ScrapingException(
            "Failed to POST JSON after $MAX_RETRIES attempts: $url",
            lastException
        )
    }
    
    /**
     * 지수 백오프 지연 시간을 계산합니다.
     * @param attempt 현재 시도 횟수 (0부터 시작)
     * @return 지연 시간 (밀리초)
     */
    private fun calculateDelay(attempt: Int): Long {
        return (BASE_DELAY_MS * 2.0.pow(attempt)).toLong()
    }
}
