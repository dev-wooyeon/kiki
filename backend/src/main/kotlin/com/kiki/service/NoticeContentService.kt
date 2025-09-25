package com.kiki.service

import com.kiki.scraper.HttpClientUtil
import com.kiki.scraper.ParsingUtil
import org.jsoup.nodes.Document
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class NoticeContentService(
    private val httpClientUtil: HttpClientUtil,
    private val parsingUtil: ParsingUtil
) {

    private val logger = LoggerFactory.getLogger(NoticeContentService::class.java)

    fun extractPrimaryText(url: String): String? {
        return try {
            val document = httpClientUtil.fetchDocument(url)
            val raw = extractTextFromDocument(document)
            parsingUtil.cleanText(raw).takeIf { it.isNotBlank() }
        } catch (ex: Exception) {
            logger.warn("Failed to fetch notice content from {}: {}", url, ex.message)
            null
        }
    }

    private fun extractTextFromDocument(document: Document): String {
        val selectors = listOf(
            "main",
            "article",
            "[role=main]",
            "section.content",
            "div.article-body",
            "div#content",
            "div.post-content",
            "div.news-detail",
            "div.detail",
            "div#article"
        )

        selectors.forEach { selector ->
            val element = document.selectFirst(selector)
            if (element != null) {
                val text = element.text().trim()
                if (text.isNotBlank()) {
                    return text
                }
            }
        }

        return document.body()?.text()?.trim().orEmpty()
    }
}
