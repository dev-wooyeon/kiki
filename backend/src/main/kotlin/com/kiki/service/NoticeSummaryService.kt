package com.kiki.service

import com.kiki.entity.GameNotice
import com.kiki.integration.OpenAiClient
import com.kiki.repository.GameNoticeRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Service
class NoticeSummaryService(
    private val openAiClient: OpenAiClient,
    private val noticeContentService: NoticeContentService,
    private val gameNoticeRepository: GameNoticeRepository
) {

    private val logger = LoggerFactory.getLogger(NoticeSummaryService::class.java)
    private val publishedFormatter = DateTimeFormatter.ofPattern("MM/dd HH:mm", Locale.KOREAN)
    private val promptFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.KOREAN)
    private val newlineRegex = Regex("\\s+")

    fun generateDigestSummary(
        notices: List<GameNotice>,
        highlightsPerGame: Int = DEFAULT_HIGHLIGHT_LIMIT,
        summaryLength: Int = DEFAULT_SUMMARY_LENGTH
    ): DigestSummary {
        if (notices.isEmpty()) {
            return DigestSummary(
                totalCount = 0,
                generatedAt = LocalDateTime.now(),
                gameSummaries = emptyList()
            )
        }

        val grouped = notices.groupBy { it.game.name }
        val gameSummaries = grouped.map { (gameName, gameNotices) ->
            val sorted = gameNotices.sortedByDescending { it.publishedDate }
            val highlights = sorted.take(highlightsPerGame).map { notice ->
                SummaryHighlight(
                    title = notice.title,
                    publishedAt = notice.publishedDate,
                    snippet = notice.summary?.let { cleanSummary(it, summaryLength) },
                    url = notice.url
                )
            }
            val aiSummary = generateAiSummary(gameName, sorted)

            GameSummary(
                gameName = gameName,
                totalCount = gameNotices.size,
                latestPublishedAt = sorted.firstOrNull()?.publishedDate,
                highlights = highlights,
                aiSummary = aiSummary
            )
        }.sortedByDescending { it.latestPublishedAt }

        return DigestSummary(
            totalCount = notices.size,
            generatedAt = LocalDateTime.now(),
            gameSummaries = gameSummaries
        )
    }

    fun formatAsPlainText(digestSummary: DigestSummary): String {
        if (digestSummary.gameSummaries.isEmpty()) {
            return "새로운 공지사항 요약이 없습니다."
        }

        return buildString {
            appendLine("총 ${digestSummary.totalCount}건의 공지사항")
            digestSummary.gameSummaries.forEach { summary ->
                val latest = summary.latestPublishedAt?.format(publishedFormatter) ?: "-"
                appendLine("- ${summary.gameName} (${summary.totalCount}건, 최신: $latest)")
                summary.aiSummary?.let {
                    appendLine("    요약: ${it.replace(newlineRegex, " ")}")
                }
                summary.highlights.forEach { highlight ->
                    val published = highlight.publishedAt.format(publishedFormatter)
                    append("    • [$published] ${highlight.title}")
                    highlight.snippet?.let { append(" – $it") }
                    appendLine()
                }
            }
        }
    }

    private fun cleanSummary(summary: String, maxLength: Int): String {
        val normalized = summary.replace(newlineRegex, " ").trim()
        if (normalized.isBlank()) return ""
        return if (normalized.length <= maxLength) normalized else normalized.take(maxLength).trimEnd() + "..."
    }

    data class DigestSummary(
        val totalCount: Int,
        val generatedAt: LocalDateTime,
        val gameSummaries: List<GameSummary>
    )

    data class GameSummary(
        val gameName: String,
        val totalCount: Int,
        val latestPublishedAt: LocalDateTime?,
        val highlights: List<SummaryHighlight>,
        val aiSummary: String?
    )

    data class SummaryHighlight(
        val title: String,
        val publishedAt: LocalDateTime,
        val snippet: String?,
        val url: String
    )

    companion object {
        private const val DEFAULT_HIGHLIGHT_LIMIT = 3
        private const val DEFAULT_SUMMARY_LENGTH = 140
        private const val MAX_ITEMS_FOR_AI = 5
        private const val MAX_SUMMARY_CHARS = 180
    }

    private fun generateAiSummary(gameName: String, notices: List<GameNotice>): String? {
        if (notices.isEmpty()) return null

        val highlightsForPrompt = notices
            .sortedByDescending { it.publishedDate }
            .take(MAX_ITEMS_FOR_AI)
            .joinToString(separator = "\n") { notice ->
                val published = notice.publishedDate.format(promptFormatter)
                val builder = StringBuilder("- [$published] ${notice.title}")
                notice.summary?.takeIf { it.isNotBlank() }?.let { summary ->
                    val clipped = summary.replace(newlineRegex, " ").take(MAX_SUMMARY_CHARS)
                    builder.append(" – $clipped")
                }
                builder.toString()
            }

        val userPrompt = """
            게임명: $gameName
            업데이트 목록:
$highlightsForPrompt

            위 정보를 바탕으로 플레이어가 알아두면 좋은 핵심 변경사항을 2~3문장의 간결한 한국어 요약으로 작성해 주세요.
            요약은 존댓말 대신 간결한 서술형으로 작성하고 과장된 표현은 피하세요.
        """.trimIndent()

        val systemPrompt = "당신은 게임 업데이트 내용을 간결하게 정리하는 한국어 전문 에디터입니다."

        return try {
            openAiClient.generateSummary(systemPrompt, userPrompt)
        } catch (ex: Exception) {
            logger.warn("Failed to generate AI summary for {}: {}", gameName, ex.message)
            null
        }
    }
}
