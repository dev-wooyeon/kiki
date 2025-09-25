package com.kiki.service

import com.kiki.scraper.service.ScrapingResult
import com.kiki.scraper.service.ScrapingService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 스케줄링 서비스
 * 30분마다 게임 공지사항 스크래핑 작업을 실행합니다.
 */
@Service
class SchedulingService(
    private val scrapingService: ScrapingService,
    private val emailService: EmailService,
    private val gameNoticeRepository: com.kiki.repository.GameNoticeRepository
) {
    
    companion object {
        private val logger = LoggerFactory.getLogger(SchedulingService::class.java)
        private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        
        // 30분 = 30 * 60 * 1000 = 1,800,000 밀리초
        private const val SCRAPING_INTERVAL_MS = 1800000L
    }

    @Volatile
    private var lastMetrics: SchedulingMetrics = SchedulingMetrics()
    
    /**
     * 30분마다 실행되는 스크래핑 작업
     * fixedRate를 사용하여 이전 작업 완료와 관계없이 정확한 간격으로 실행
     */
    @Scheduled(fixedRate = SCRAPING_INTERVAL_MS)
    fun executeScrapingJob() {
        val startTime = LocalDateTime.now()
        val formattedStartTime = startTime.format(dateTimeFormatter)
        
        logger.info("=== Scheduled scraping job started at {} ===", formattedStartTime)
        updateMetricsOnStart(RunTrigger.SCHEDULED, startTime)

        try {
            // 스크래핑 실행
            val result = scrapingService.scrapeAllGames()
            
            val endTime = LocalDateTime.now()
            val formattedEndTime = endTime.format(dateTimeFormatter)
            val durationMs = result.duration
            updateMetricsFromResult(result)

            if (result.success) {
                logger.info("=== Scheduled scraping job completed successfully at {} ===", formattedEndTime)
                logger.info("Scraping summary: {} games processed, {} new notices found in {}ms", 
                    result.totalGames, result.totalNewNotices, durationMs)
                
                // 게임별 결과 로깅
                result.gameResults.forEach { (gameName, gameResult) ->
                    if (gameResult.success) {
                        logger.info("  - {}: {} new notices (scraped: {}, duration: {}ms)", 
                            gameName, gameResult.newNoticesCount, gameResult.scrapedNoticesCount, gameResult.duration)
                    } else {
                        logger.warn("  - {}: FAILED - {}", gameName, gameResult.errorMessage)
                    }
                }
                
                // 새로운 공지사항이 있으면 이메일 발송 트리거
                if (result.totalNewNotices > 0) {
                    logger.info("Found {} new notices, triggering email notification", result.totalNewNotices)
                    triggerEmailNotification()
                }
                
            } else {
                logger.error("=== Scheduled scraping job failed at {} ===", formattedEndTime)
                logger.error("Error: {}", result.errorMessage)
            }
            
        } catch (e: Exception) {
            val endTime = LocalDateTime.now()
            val formattedEndTime = endTime.format(dateTimeFormatter)

            logger.error("=== Scheduled scraping job encountered critical error at {} ===", formattedEndTime, e)
            updateMetricsOnFailure(startTime, endTime, e.message)
            
            // 중요한 오류는 별도 알림 처리 (향후 구현)
            // TODO: 관리자 알림 시스템 연동
        }
    }
    
    /**
     * 수동으로 스크래핑 작업을 실행합니다.
     * 테스트나 관리 목적으로 사용됩니다.
     * @return 스크래핑 결과
     */
    fun executeManualScraping(): ScrapingResult {
        logger.info("Manual scraping job triggered")
        
        val startTime = LocalDateTime.now()
        updateMetricsOnStart(RunTrigger.MANUAL, startTime)
        
        try {
            val result = scrapingService.scrapeAllGames()
            updateMetricsFromResult(result)
            
            if (result.success) {
                logger.info("Manual scraping completed successfully: {} games, {} new notices", 
                    result.totalGames, result.totalNewNotices)
            } else {
                logger.error("Manual scraping failed: {}", result.errorMessage)
            }
            
            return result
            
        } catch (e: Exception) {
            logger.error("Manual scraping encountered error", e)
            val endTime = LocalDateTime.now()
            updateMetricsOnFailure(startTime, endTime, e.message)
            throw e
        }
    }
    
    /**
     * 스케줄링 상태 정보를 반환합니다.
     * @return 스케줄링 상태 정보
     */
    fun getSchedulingInfo(): SchedulingInfo {
        return SchedulingInfo(
            intervalMs = SCRAPING_INTERVAL_MS,
            intervalMinutes = SCRAPING_INTERVAL_MS / 60000,
            nextExecutionEstimate = getNextExecutionEstimate(),
            isEnabled = true // Spring의 @Scheduled는 기본적으로 활성화됨
        )
    }
    
    /**
     * 이메일 알림을 트리거합니다.
     * 발송되지 않은 새로운 공지사항을 조회하여 이메일로 발송합니다.
     */
    private fun triggerEmailNotification() {
        try {
            logger.info("Starting email notification process")
            
            // 발송되지 않은 공지사항 조회
            val unsentNotices = scrapingService.getUnsentNotices()
            
            if (unsentNotices.isEmpty()) {
                logger.info("No unsent notices found for email notification")
                return
            }
            
            logger.info("Found {} unsent notices, sending email digest", unsentNotices.size)
            
            // 이메일 발송
            emailService.sendDailyDigest(unsentNotices)
            
            // 발송 완료 표시
            scrapingService.markNoticesAsSent(unsentNotices)
            
            logger.info("Email notification process completed successfully for {} notices", unsentNotices.size)
            
        } catch (e: Exception) {
            logger.error("Failed to send email notification", e)
            // 이메일 발송 실패는 전체 스크래핑 프로세스를 중단시키지 않음
        }
    }
    
    /**
     * 수동으로 이메일 알림을 실행합니다.
     * 테스트나 관리 목적으로 사용됩니다.
     * @return 발송된 공지사항 수
     */
    fun executeManualEmailNotification(forceAll: Boolean = false, hoursLookback: Long = 24): Int {
        try {
            val unsentNotices = if (forceAll) {
                val from = LocalDateTime.now().minusHours(hoursLookback)
                logger.info("Force send enabled: collecting notices since {} ({}h)", from, hoursLookback)
                gameNoticeRepository.findByPublishedDateAfterOrderByPublishedDateDesc(from)
            } else {
                scrapingService.getUnsentNotices()
            }
            
            if (unsentNotices.isEmpty()) {
                logger.info("No unsent notices found for manual email notification")
                return 0
            }
            
            logger.info("Sending manual email notification for {} notices", unsentNotices.size)
            
            emailService.sendDailyDigest(unsentNotices)
            scrapingService.markNoticesAsSent(unsentNotices)
            
            logger.info("Manual email notification completed successfully for {} notices", unsentNotices.size)
            return unsentNotices.size
            
        } catch (e: Exception) {
            logger.error("Manual email notification failed", e)
            throw e
        }
    }
    
    /**
     * 다음 실행 예상 시간을 계산합니다.
     * 정확한 시간은 Spring 스케줄러에 의해 결정되므로 추정값입니다.
     */
    private fun getNextExecutionEstimate(): LocalDateTime {
        // fixedRate이므로 현재 시간 + 간격으로 추정
        return LocalDateTime.now().plusSeconds(SCRAPING_INTERVAL_MS / 1000)
    }

    fun getSchedulingStatus(): SchedulingStatus {
        return SchedulingStatus(
            info = getSchedulingInfo(),
            metrics = lastMetrics
        )
    }

    private fun updateMetricsOnStart(trigger: RunTrigger, startTime: LocalDateTime) {
        lastMetrics = SchedulingMetrics(
            lastRunStartedAt = startTime,
            lastRunTriggeredBy = trigger
        )
    }

    private fun updateMetricsFromResult(result: ScrapingResult) {
        lastMetrics = lastMetrics.copy(
            lastRunCompletedAt = result.endTime,
            lastRunDurationMs = result.duration,
            lastRunSuccess = result.success,
            lastRunErrorMessage = result.errorMessage,
            lastRunTotalGames = result.totalGames,
            lastRunTotalNewNotices = result.totalNewNotices
        )
    }

    private fun updateMetricsOnFailure(startTime: LocalDateTime, endTime: LocalDateTime, errorMessage: String?) {
        lastMetrics = lastMetrics.copy(
            lastRunCompletedAt = endTime,
            lastRunDurationMs = Duration.between(startTime, endTime).toMillis(),
            lastRunSuccess = false,
            lastRunErrorMessage = errorMessage,
            lastRunTotalGames = null,
            lastRunTotalNewNotices = null
        )
    }
}

/**
 * 스케줄링 정보 데이터 클래스
 */
data class SchedulingInfo(
    val intervalMs: Long,
    val intervalMinutes: Long,
    val nextExecutionEstimate: LocalDateTime,
    val isEnabled: Boolean
)


data class SchedulingStatus(
    val info: SchedulingInfo,
    val metrics: SchedulingMetrics
)

data class SchedulingMetrics(
    val lastRunStartedAt: LocalDateTime? = null,
    val lastRunCompletedAt: LocalDateTime? = null,
    val lastRunDurationMs: Long? = null,
    val lastRunSuccess: Boolean? = null,
    val lastRunErrorMessage: String? = null,
    val lastRunTotalGames: Int? = null,
    val lastRunTotalNewNotices: Int? = null,
    val lastRunTriggeredBy: RunTrigger? = null
)

enum class RunTrigger {
    SCHEDULED,
    MANUAL
}
