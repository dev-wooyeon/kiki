package com.kiki.scraper.client

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.kiki.scraper.HttpClientUtil
import com.kiki.scraper.ScrapingException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicReference

@Component
class NikkeCmsClient(
    private val httpClientUtil: HttpClientUtil,
    private val objectMapper: ObjectMapper,
    @Value("\${kiki.scraping.nikke.base-url:https://na-community.playerinfinite.com}") private val baseUrl: String,
    @Value("\${kiki.scraping.nikke.game-id:16}") private val gameId: String,
    @Value("\${kiki.scraping.nikke.area-id:na}") private val areaId: String,
    @Value("\${kiki.scraping.nikke.source:pc_web}") private val source: String,
    @Value("\${kiki.scraping.nikke.language:ko}") private val language: String,
    @Value("\${kiki.scraping.nikke.page-size:20}") private val pageSize: Int,
    @Value("\${kiki.scraping.nikke.max-age-hours:72}") private val maxAgeHours: Long,
    @Value("\${kiki.scraping.nikke.detail-base-url:https://nikke-kr.com/newsdetail.html}") private val detailBaseUrl: String
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val zoneId = ZoneId.of("Asia/Seoul")
    private val labelCache = AtomicReference<LabelSelection?>()

    fun fetchLatestNotices(): List<NikkeNotice> {
        val selection = labelCache.get() ?: fetchAndCacheLabelSelection()
        val payload = mutableMapOf<String, Any>(
            "gameid" to gameId,
            "language" to listOf(language),
            "primary_label_id" to selection.primaryLabelId,
            "secondary_label_id" to selection.secondaryLabelId,
            "get_num" to pageSize,
            "offset" to 0,
            "ext_info_type_list" to listOf(0, 1, 2),
            "content_class" to 0
        )

        val responseJson = postCms(CONTENT_PATH, payload)
        val response = parseResponse(responseJson, CONTENT_RESPONSE_TYPE)

        if (response.code != 0) {
            throw ScrapingException("NIKKE content API returned error code ${response.code}: ${response.msg}")
        }

        val items = response.data?.infoContent.orEmpty()
        val cutoff = if (maxAgeHours > 0) LocalDateTime.now(zoneId).minusHours(maxAgeHours) else null

        return items.mapNotNull { item ->
            val contentId = item.contentId?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val title = item.title?.trim().orEmpty()
            if (title.isBlank()) {
                return@mapNotNull null
            }

            val publishedAt = parseTimestamp(item.pubTimestamp)
            if (cutoff != null && publishedAt.isBefore(cutoff)) {
                logger.debug("Skipping old NIKKE notice {} published at {}", contentId, publishedAt)
                return@mapNotNull null
            }

            NikkeNotice(
                contentId = contentId,
                title = title,
                summary = buildSummary(item),
                publishedAt = publishedAt,
                url = buildDetailUrl(contentId, selection.secondaryLabelId, selection.secondaryLabelName)
            )
        }.distinctBy { it.url }
    }

    private fun fetchAndCacheLabelSelection(): LabelSelection {
        val payload = mapOf(
            "gameid" to gameId,
            "language" to listOf(language),
            "tag_id" to "0",
            "with_content_count" to true
        )

        val responseJson = postCms(LABEL_PATH, payload)
        val response = parseResponse(responseJson, LABEL_RESPONSE_TYPE)

        if (response.code != 0) {
            throw ScrapingException("NIKKE label API returned error code ${response.code}: ${response.msg}")
        }

        val primary = response.data?.primaryLabelList.orEmpty().firstOrNull {
            it.rawLabelName.equals("official_news", ignoreCase = true) ||
                it.labelName.equals("news", ignoreCase = true)
        } ?: throw ScrapingException("Failed to locate NIKKE primary news label")

        val secondary = primary.secondaryLabelList.orEmpty().firstOrNull {
            it.rawLabelName.equals("NOTICE", ignoreCase = true) ||
                (it.labelName?.contains("공지") == true)
        } ?: throw ScrapingException("Failed to locate NIKKE notice secondary label")

        val selection = LabelSelection(
            primaryLabelId = primary.labelId ?: throw ScrapingException("Primary label id missing"),
            primaryLabelName = primary.rawLabelName ?: primary.labelName ?: "official_news",
            secondaryLabelId = secondary.labelId ?: throw ScrapingException("Secondary label id missing"),
            secondaryLabelName = secondary.rawLabelName ?: secondary.labelName ?: "NOTICE"
        )

        labelCache.compareAndSet(null, selection)
        return selection
    }

    private fun buildDetailUrl(contentId: String, secondaryLabelId: Long, colName: String): String {
        val encodedColName = URLEncoder.encode(colName, StandardCharsets.UTF_8)
        val base = detailBaseUrl.removeSuffix("/")
        return "$base?content_id=$contentId&sid=$secondaryLabelId&from=list&col_name=$encodedColName"
    }

    private fun buildSummary(item: NoticeItem): String? {
        val raw = item.contentDesc?.takeIf { it.isNotBlank() }
            ?: item.contentPart?.replace("\n", " ")?.trim()

        return raw?.takeIf { it.isNotBlank() }?.let { text ->
            if (text.length <= SUMMARY_LIMIT) text else text.take(SUMMARY_LIMIT - 3) + "..."
        }
    }

    private fun parseTimestamp(timestamp: String?): LocalDateTime {
        val epoch = timestamp?.toLongOrNull() ?: return LocalDateTime.now(zoneId)
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epoch), zoneId)
    }

    private fun postCms(path: String, payload: Any): String {
        val url = buildPath(path)
        val headers = buildHeaders()
        val body = objectMapper.writeValueAsString(payload)
        return httpClientUtil.postJson(url, headers, body)
    }

    private fun buildHeaders(): Map<String, String> = mapOf(
        "X-GameId" to gameId,
        "X-AreaId" to areaId,
        "X-Source" to source,
        "X-Language" to language
    )

    private fun buildPath(path: String): String {
        val trimmedBase = baseUrl.removeSuffix("/")
        val normalizedPath = if (path.startsWith("/")) path else "/$path"
        return "$trimmedBase$normalizedPath"
    }

    private fun <T> parseResponse(json: String, type: TypeReference<CmsResponse<T>>): CmsResponse<T> {
        return try {
            objectMapper.readValue(json, type)
        } catch (ex: Exception) {
            logger.error("Failed to parse NIKKE CMS response", ex)
            throw ScrapingException("Failed to parse NIKKE CMS response", ex)
        }
    }

    data class NikkeNotice(
        val contentId: String,
        val title: String,
        val summary: String?,
        val publishedAt: LocalDateTime,
        val url: String
    )

    private data class LabelSelection(
        val primaryLabelId: Long,
        val primaryLabelName: String,
        val secondaryLabelId: Long,
        val secondaryLabelName: String
    )

    private data class CmsResponse<T>(
        val code: Int?,
        val msg: String?,
        val data: T?
    )

    private data class LabelListData(
        @JsonProperty("primary_label_list")
        val primaryLabelList: List<LabelInfo>?
    )

    private data class LabelInfo(
        @JsonProperty("label_id")
        val labelId: Long?,
        @JsonProperty("label_name")
        val labelName: String?,
        @JsonProperty("raw_label_name")
        val rawLabelName: String?,
        @JsonProperty("secondary_label_list")
        val secondaryLabelList: List<LabelInfo> = emptyList()
    )

    private data class ContentListData(
        @JsonProperty("info_content")
        val infoContent: List<NoticeItem> = emptyList()
    )

    private data class NoticeItem(
        @JsonProperty("content_id")
        val contentId: String?,
        val title: String?,
        @JsonProperty("pub_timestamp")
        val pubTimestamp: String?,
        @JsonProperty("content_desc")
        val contentDesc: String?,
        @JsonProperty("content_part")
        val contentPart: String?
    )

    companion object {
        private const val LABEL_PATH = "/api/gpts.information_feeds_svr.InformationFeedsSvr/GetLabelList"
        private const val CONTENT_PATH = "/api/gpts.information_feeds_svr.InformationFeedsSvr/GetContentByLabel"
        private const val SUMMARY_LIMIT = 280

        private val LABEL_RESPONSE_TYPE = object : TypeReference<CmsResponse<LabelListData>>() {}
        private val CONTENT_RESPONSE_TYPE = object : TypeReference<CmsResponse<ContentListData>>() {}
    }
}
