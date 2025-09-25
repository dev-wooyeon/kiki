package com.kiki.scraper

/**
 * 스크래핑 과정에서 발생하는 예외를 나타내는 클래스
 */
class ScrapingException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)