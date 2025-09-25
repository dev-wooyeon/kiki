package com.kiki.service

import com.kiki.entity.*
import com.kiki.repository.EmailLogRepository
import com.kiki.repository.SubscriberRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.util.HtmlUtils
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
@Transactional
class EmailService(
    private val mailSender: JavaMailSender,
    private val subscriberRepository: SubscriberRepository,
    private val emailLogRepository: EmailLogRepository,
    private val noticeSummaryService: NoticeSummaryService,
    @Value("\${kiki.email.from}") private val fromEmail: String,
    @Value("\${server.port:8080}") private val serverPort: String,
    @Value("\${kiki.frontend.url:http://localhost:3000}") private val frontendUrl: String
) {
    
    private val logger = LoggerFactory.getLogger(EmailService::class.java)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH:mm")
    
    /**
     * 새로운 공지사항이 있을 때 모든 활성 구독자에게 이메일 발송
     */
    fun sendDailyDigest(notices: List<GameNotice>) {
        if (notices.isEmpty()) {
            logger.info("새로운 공지사항이 없어 이메일을 발송하지 않습니다.")
            return
        }
        val activeSubscribers = subscriberRepository.findByIsActiveTrue()
        if (activeSubscribers.isEmpty()) {
            logger.info("활성 구독자가 없어 이메일을 발송하지 않습니다.")
            return
        }

        logger.info("${notices.size}개의 공지사항을 기준으로 ${activeSubscribers.size}명의 구독자에게 맞춤 발송합니다.")

        // 구독 시점을 시간 단위로 버킷팅하여 캐싱 효율화
        val cohortMap = activeSubscribers.groupBy { it.subscribedAt.withMinute(0).withSecond(0).withNano(0) }
        val contentCache = mutableMapOf<String, Pair<String, String>>() // key -> (subject, html)

        cohortMap.forEach { (bucketStart, subscribers) ->
            val filteredNotices = notices.filter { it.publishedDate.isAfter(bucketStart) || it.publishedDate.isEqual(bucketStart) }

            if (filteredNotices.isEmpty()) {
                logger.info("버킷 {} 이후의 공지가 없어 {}명에게 발송 생략", bucketStart, subscribers.size)
                return@forEach
            }

            val key = buildCacheKey(bucketStart, filteredNotices)
            val (subject, html) = contentCache.getOrPut(key) {
                val digest = try {
                    // 본문 추출 + LLM 요약을 포함한 다이제스트 생성
                    // 기존 요약만으로도 동작하도록, 내부에서 fallback 처리
                    noticeSummaryService.generateDigestSummary(filteredNotices)
                } catch (ex: Exception) {
                    logger.warn("다이제스트 생성 실패, 기본 모드로 전송: {}", ex.message)
                    noticeSummaryService.generateDigestSummary(filteredNotices)
                }
                val subjectBuilt = generateEmailSubject(filteredNotices)
                val htmlBuilt = generateEmailContent(filteredNotices, digest)
                subjectBuilt to htmlBuilt
            }

            subscribers.forEach { sub ->
                sendEmailToSubscriber(sub, subject, html, filteredNotices.size)
            }
        }
    }
    
    /**
     * 구독 확인 이메일 발송
     */
    fun sendSubscriptionConfirmation(email: String) {
        try {
            val mimeMessage = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(mimeMessage, true, "UTF-8")
            
            helper.setFrom(fromEmail)
            helper.setTo(email)
            helper.setSubject("KIKI 구독이 완료되었습니다!")
            helper.setText(generateSubscriptionConfirmationContent(), true)
            
            mailSender.send(mimeMessage)
            logger.info("구독 확인 이메일을 성공적으로 발송했습니다: $email")
            
        } catch (e: Exception) {
            logger.error("구독 확인 이메일 발송 실패: $email", e)
        }
    }
    
    /**
     * 개별 구독자에게 이메일 발송 및 로그 기록
     */
    private fun sendEmailToSubscriber(subscriber: Subscriber, subject: String, content: String, noticeCount: Int) {
        val personalizedContent = content.replace("{{unsubscribe_url}}", generateUnsubscribeUrl(subscriber.unsubscribeToken))
        try {
            val mimeMessage = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(mimeMessage, true, "UTF-8")
            
            helper.setFrom(fromEmail)
            helper.setTo(subscriber.email)
            helper.setSubject(subject)
            helper.setText(personalizedContent, true)
            
            mailSender.send(mimeMessage)
            
            // 성공 로그 기록
            emailLogRepository.save(
                EmailLog(
                    subscriber = subscriber,
                    noticeCount = noticeCount,
                    status = EmailStatus.SUCCESS
                )
            )
            
            logger.debug("이메일 발송 성공: ${subscriber.email}")
            
        } catch (e: Exception) {
            // 실패 로그 기록
            emailLogRepository.save(
                EmailLog(
                    subscriber = subscriber,
                    noticeCount = noticeCount,
                    status = EmailStatus.FAILED,
                    errorMessage = e.message
                )
            )
            
            logger.error("이메일 발송 실패: ${subscriber.email}", e)
        }
    }
    
    /**
     * 이메일 제목 생성
     */
    private fun generateEmailSubject(notices: List<GameNotice>): String {
        val gameNames = notices.groupBy { it.game.name }.keys
        val today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM/dd"))
        
        return when {
            gameNames.size == 1 -> "[$today] ${gameNames.first()} 업데이트 ${notices.size}건"
            gameNames.size == 2 -> "[$today] ${gameNames.joinToString(", ")} 업데이트 ${notices.size}건"
            else -> "[$today] 게임 업데이트 ${notices.size}건 (${gameNames.size}개 게임)"
        }
    }

    private fun buildCacheKey(bucketStart: LocalDateTime, notices: List<GameNotice>): String {
        val latest = notices.maxByOrNull { it.publishedDate }?.publishedDate?.toString() ?: "-"
        return "${bucketStart}|${notices.size}|$latest"
    }
    
    /**
     * HTML 이메일 내용 생성 (게임별 그룹화)
     */
    private fun generateEmailContent(
        notices: List<GameNotice>,
        digestSummary: NoticeSummaryService.DigestSummary
    ): String {
        val noticesByGame = notices.groupBy { it.game.name }
        val currentDate = LocalDateTime.now().format(dateFormatter)
        val digestGameSummaries = digestSummary.gameSummaries
        
        return buildString {
            append("""
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>KIKI 게임 업데이트 알림</title>
                    <style>
                        body {
                            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                            line-height: 1.6;
                            color: #333;
                            max-width: 600px;
                            margin: 0 auto;
                            padding: 20px;
                            background-color: #f8f9fa;
                        }
                        .container {
                            background-color: white;
                            border-radius: 12px;
                            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
                            overflow: hidden;
                        }
                        .header {
                            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                            color: white;
                            padding: 30px 20px;
                            text-align: center;
                        }
                        .header h1 {
                            margin: 0;
                            font-size: 28px;
                            font-weight: 700;
                        }
                        .header p {
                            margin: 10px 0 0 0;
                            opacity: 0.9;
                            font-size: 16px;
                        }
                        .content {
                            padding: 30px 20px;
                        }
                        .game-section {
                            margin-bottom: 40px;
                            border-left: 4px solid #667eea;
                            padding-left: 20px;
                        }
                        .game-title {
                            font-size: 22px;
                            font-weight: 600;
                            color: #2c3e50;
                            margin-bottom: 20px;
                            display: flex;
                            align-items: center;
                        }
                        .game-icon {
                            width: 24px;
                            height: 24px;
                            background: #667eea;
                            border-radius: 50%;
                            margin-right: 10px;
                        }
                        .notice-item {
                            background-color: #f8f9fa;
                            border-radius: 8px;
                            padding: 20px;
                            margin-bottom: 15px;
                            border: 1px solid #e9ecef;
                            transition: all 0.3s ease;
                        }
                        .notice-item:hover {
                            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
                        }
                        .notice-title {
                            font-size: 18px;
                            font-weight: 600;
                            color: #2c3e50;
                            margin-bottom: 8px;
                        }
                        .notice-title a {
                            color: #2c3e50;
                            text-decoration: none;
                        }
                        .notice-title a:hover {
                            color: #667eea;
                        }
                        .notice-meta {
                            font-size: 14px;
                            color: #6c757d;
                            margin-bottom: 10px;
                        }
                        .notice-summary {
                            font-size: 15px;
                            color: #495057;
                            line-height: 1.5;
                        }
                        .footer {
                            background-color: #f8f9fa;
                            padding: 20px;
                            text-align: center;
                            border-top: 1px solid #e9ecef;
                        }
                        .footer p {
                            margin: 5px 0;
                            font-size: 14px;
                            color: #6c757d;
                        }
                        .unsubscribe-link {
                            color: #dc3545;
                            text-decoration: none;
                            font-weight: 500;
                        }
                        .unsubscribe-link:hover {
                            text-decoration: underline;
                        }
                        .stats {
                            background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
                            color: white;
                            padding: 15px;
                            border-radius: 8px;
                            margin-bottom: 30px;
                            text-align: center;
                        }
                        .stats-number {
                            font-size: 24px;
                            font-weight: 700;
                        }
                        .quick-summary {
                            margin-bottom: 30px;
                            padding: 20px;
                            background-color: #eef2ff;
                            border-radius: 8px;
                            border: 1px solid #d6dcff;
                        }
                        .summary-title {
                            font-size: 18px;
                            font-weight: 600;
                            color: #3b4cca;
                            margin-bottom: 12px;
                        }
                        .summary-game {
                            margin-bottom: 16px;
                        }
                        .summary-game:last-child {
                            margin-bottom: 0;
                        }
                        .summary-game-title {
                            font-weight: 600;
                            margin-bottom: 6px;
                            color: #273469;
                        }
                        .summary-ai {
                            background-color: #fff;
                            border: 1px solid #d6dcff;
                            border-radius: 6px;
                            padding: 12px;
                            margin-bottom: 10px;
                            font-size: 15px;
                            color: #2c3e50;
                        }
                        .summary-list {
                            margin: 0;
                            padding-left: 20px;
                            color: #333;
                        }
                        .summary-list li {
                            margin-bottom: 4px;
                        }
                        .link-section {
                            margin: 40px 0 20px;
                            padding: 20px;
                            background-color: #f1f5ff;
                            border-radius: 8px;
                            border: 1px solid #d0dcff;
                        }
                        .link-section h3 {
                            margin-top: 0;
                            color: #1f3c88;
                        }
                        .link-game {
                            margin-bottom: 18px;
                        }
                        .link-game-title {
                            font-weight: 600;
                            margin-bottom: 6px;
                            color: #2c3e50;
                        }
                        .link-list {
                            margin: 0;
                            padding-left: 18px;
                        }
                        .link-list li {
                            margin-bottom: 4px;
                            font-size: 15px;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>🎮 KIKI</h1>
                            <p>게임 업데이트 알림 서비스</p>
                        </div>
                        
                        <div class="content">
                            <div class="stats">
                                <div class="stats-number">${notices.size}개</div>
                                <div>새로운 공지사항이 있습니다!</div>
                            </div>
                """.trimIndent())

            if (digestGameSummaries.isNotEmpty()) {
                append("""
                        <div class="quick-summary">
                            <div class="summary-title">빠른 요약</div>
                """.trimIndent())

                digestGameSummaries.forEach { gameSummary ->
                    append("""
                                <div class="summary-game">
                                    <div class="summary-game-title">${HtmlUtils.htmlEscape(gameSummary.gameName)} (${gameSummary.totalCount}건)</div>
                    """.trimIndent())

                    gameSummary.aiSummary?.let {
                        append("""
                                    <div class="summary-ai">${HtmlUtils.htmlEscape(it)}</div>
                        """.trimIndent())
                    }

                    if (gameSummary.highlights.isNotEmpty()) {
                        append("<ul class=\"summary-list\">")
                        gameSummary.highlights.forEach { highlight ->
                            val published = highlight.publishedAt.format(dateFormatter)
                            val escapedTitle = HtmlUtils.htmlEscape(highlight.title)
                            val escapedSnippet = highlight.snippet?.let { HtmlUtils.htmlEscape(it) }
                            append("<li><strong>$published</strong> $escapedTitle")
                            if (!escapedSnippet.isNullOrBlank()) {
                                append(" – $escapedSnippet")
                            }
                            append("</li>")
                        }
                        append("</ul>")
                    }

                    append("""
                                </div>
                    """.trimIndent())
                }

                append("""
                        </div>
                """.trimIndent())
            }

            // 게임별 공지사항 섹션 생성
            noticesByGame.forEach { (gameName, gameNotices) ->
                append("""
                    <div class="game-section">
                        <div class="game-title">
                            <div class="game-icon"></div>
                            ${HtmlUtils.htmlEscape(gameName)} (${gameNotices.size}건)
                        </div>
                """.trimIndent())

                gameNotices.forEach { notice ->
                    val publishedDate = notice.publishedDate.format(dateFormatter)
                    val title = HtmlUtils.htmlEscape(notice.title)
                    val summary = notice.summary?.let {
                        val cleaned = if (it.length > 150) "${it.take(150)}..." else it
                        HtmlUtils.htmlEscape(cleaned)
                    } ?: "공지사항 내용을 확인해보세요."
                    val url = HtmlUtils.htmlEscape(notice.url)

                    append("""
                        <div class="notice-item">
                            <div class="notice-title">
                                <a href="$url" target="_blank">$title</a>
                            </div>
                            <div class="notice-meta">📅 $publishedDate</div>
                            <div class="notice-summary">$summary</div>
                        </div>
                    """.trimIndent())
                }

                append("</div>")
            }

            append("""
                        <div class="link-section">
                            <h3>📎 상세 공지 바로가기</h3>
            """.trimIndent())

            noticesByGame.forEach { (gameName, gameNotices) ->
                append("""
                            <div class="link-game">
                                <div class="link-game-title">${HtmlUtils.htmlEscape(gameName)}</div>
                                <ul class="link-list">
                """.trimIndent())

                gameNotices.forEach { notice ->
                    val title = HtmlUtils.htmlEscape(notice.title)
                    val url = HtmlUtils.htmlEscape(notice.url)
                    append("<li><a href=\"$url\" target=\"_blank\">$title</a></li>")
                }

                append("""
                                </ul>
                            </div>
                """.trimIndent())
            }

            append("""
                        </div>
                        
                        <div class="footer">
                            <p>📧 $currentDate 에 발송된 이메일입니다.</p>
                            <p>더 이상 이메일을 받고 싶지 않으시다면 
                                <a href="{{unsubscribe_url}}" class="unsubscribe-link">구독 취소</a>를 클릭해주세요.
                            </p>
                            <p>© 2025 KIKI. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
            """.trimIndent())
        }
    }
    
    /**
     * 구독 확인 이메일 내용 생성
     */
    private fun generateSubscriptionConfirmationContent(): String {
        return """
            <!DOCTYPE html>
            <html lang="ko">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>KIKI 구독 완료</title>
                <style>
                    body {
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                        line-height: 1.6;
                        color: #333;
                        max-width: 600px;
                        margin: 0 auto;
                        padding: 20px;
                        background-color: #f8f9fa;
                    }
                    .container {
                        background-color: white;
                        border-radius: 12px;
                        box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
                        overflow: hidden;
                    }
                    .header {
                        background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
                        color: white;
                        padding: 40px 20px;
                        text-align: center;
                    }
                    .header h1 {
                        margin: 0;
                        font-size: 32px;
                        font-weight: 700;
                    }
                    .content {
                        padding: 40px 20px;
                        text-align: center;
                    }
                    .success-icon {
                        font-size: 64px;
                        margin-bottom: 20px;
                    }
                    .message {
                        font-size: 18px;
                        color: #2c3e50;
                        margin-bottom: 30px;
                    }
                    .features {
                        background-color: #f8f9fa;
                        border-radius: 8px;
                        padding: 20px;
                        margin: 20px 0;
                        text-align: left;
                    }
                    .feature-item {
                        margin: 10px 0;
                        font-size: 16px;
                    }
                    .footer {
                        background-color: #f8f9fa;
                        padding: 20px;
                        text-align: center;
                        border-top: 1px solid #e9ecef;
                        font-size: 14px;
                        color: #6c757d;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🎉 구독 완료!</h1>
                    </div>
                    
                    <div class="content">
                        <div class="success-icon">✅</div>
                        <div class="message">
                            KIKI 게임 업데이트 알림 서비스 구독이 완료되었습니다!
                        </div>
                        
                        <div class="features">
                            <h3>🎮 앞으로 받아보실 내용:</h3>
                            <div class="feature-item">📱 승리의 여신 니케 업데이트 정보</div>
                            <div class="feature-item">🏰 마비노기 모바일 업데이트 정보</div>
                            <div class="feature-item">🌌 원신 업데이트 정보</div>
                            <div class="feature-item">⚔️ 명조 업데이트 정보</div>
                            <div class="feature-item">⏰ 30분마다 자동 확인하여 새로운 소식만 전달</div>
                            <div class="feature-item">📧 깔끔한 이메일 형태로 정리된 정보 제공</div>
                        </div>
                        
                        <p style="font-size: 16px; color: #495057;">
                            새로운 공지사항이 있을 때마다 이메일로 알려드리겠습니다.<br>
                            게임의 최신 소식을 놓치지 마세요! 🚀
                        </p>
                    </div>
                    
                    <div class="footer">
                        <p>© 2025 KIKI. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
    
    /**
     * 구독 취소 URL 생성
     */
    private fun generateUnsubscribeUrl(token: String): String {
        return "$frontendUrl/unsubscribe/$token"
    }
}
