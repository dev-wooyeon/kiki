package com.kiki.scraper.client

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.kiki.scraper.HttpClientUtil
import com.kiki.scraper.ScrapingException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class WutheringWavesClient(
    private val httpClientUtil: HttpClientUtil,
    private val objectMapper: ObjectMapper,
    @Value("\${kiki.scraping.wuthering.json-base-url}") private val jsonBaseUrl: String,
    @Value("\${kiki.scraping.wuthering.locale:kr}") private val locale: String
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun fetchArticleTypes(): List<ArticleTypeDto> {
        val url = buildUrl("MainMenu.json")
        val json = httpClientUtil.fetchJson(url)
        return try {
            objectMapper.readValue(json, MainMenuResponse::class.java).articleType.filter { it.contentId != null && !it.contentLabel.isNullOrBlank() }
        } catch (ex: Exception) {
            logger.error("Failed to parse Wuthering Waves MainMenu JSON", ex)
            throw ScrapingException("Failed to parse Wuthering Waves article types", ex)
        }
    }

    fun fetchArticles(): List<ArticleMenuItemDto> {
        val url = buildUrl("ArticleMenu.json")
        val json = httpClientUtil.fetchJson(url)
        return try {
            objectMapper.readValue(json, ARTICLE_MENU_TYPE_REF)
        } catch (ex: Exception) {
            logger.error("Failed to parse Wuthering Waves ArticleMenu JSON", ex)
            throw ScrapingException("Failed to parse Wuthering Waves articles", ex)
        }
    }

    private fun buildUrl(fileName: String): String {
        val normalizedBase = jsonBaseUrl.trimEnd('/')
        val normalizedLocale = locale.trim('/').ifEmpty { "kr" }
        return "$normalizedBase/$normalizedLocale/$fileName"
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class MainMenuResponse(
        @JsonProperty("articleType")
        val articleType: List<ArticleTypeDto> = emptyList()
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class ArticleTypeDto(
        @JsonProperty("contentId")
        val contentId: Long?,
        @JsonProperty("contentLabel")
        val contentLabel: String?,
        @JsonProperty("sorting")
        val sorting: Int? = null
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class ArticleMenuItemDto(
        @JsonProperty("articleId")
        val articleId: Long?,
        @JsonProperty("articleTitle")
        val articleTitle: String?,
        @JsonProperty("articleType")
        val articleType: Long?,
        @JsonProperty("startTime")
        val startTime: String?,
        @JsonProperty("articleDesc")
        val articleDesc: String? = null,
        @JsonProperty("articleContent")
        val articleContent: String? = null,
        @JsonProperty("top")
        val top: Int? = null,
        @JsonProperty("sortingMark")
        val sortingMark: Long? = null
    )

    companion object {
        private val ARTICLE_MENU_TYPE_REF = object : TypeReference<List<ArticleMenuItemDto>>() {}
    }
}
