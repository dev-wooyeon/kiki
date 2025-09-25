package com.kiki.controller

import com.kiki.dto.ApiResponse
import com.kiki.service.SchedulingService
import com.kiki.service.SystemHealthService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/monitoring")
@CrossOrigin(origins = ["*"])
class MonitoringController(
    private val schedulingService: SchedulingService,
    private val systemHealthService: SystemHealthService
) {

    private val logger = LoggerFactory.getLogger(MonitoringController::class.java)

    @GetMapping("/health")
    fun health(): ResponseEntity<Any> {
        return ResponseEntity.ok(systemHealthService.getHealthStatus())
    }

    @GetMapping("/schedule")
    fun schedule(): ResponseEntity<Any> {
        return ResponseEntity.ok(schedulingService.getSchedulingStatus())
    }

    @PostMapping("/scrape")
    fun triggerScrape(): ResponseEntity<ApiResponse> {
        return try {
            val result = schedulingService.executeManualScraping()
            ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Manual scraping job triggered",
                    data = result
                )
            )
        } catch (ex: Exception) {
            logger.error("Failed to trigger manual scraping", ex)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Manual scraping failed: ${ex.message}"
                )
            )
        }
    }

    @PostMapping("/notifications")
    fun triggerEmailDigest(
        @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "false") force: Boolean,
        @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "24") hours: Long
    ): ResponseEntity<ApiResponse> {
        return try {
            val sentCount = schedulingService.executeManualEmailNotification(forceAll = force, hoursLookback = hours)
            ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = if (force) "Manual email digest triggered (force)" else "Manual email digest triggered",
                    data = mapOf("sentNotices" to sentCount)
                )
            )
        } catch (ex: Exception) {
            logger.error("Failed to trigger manual email digest", ex)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Manual email digest failed: ${ex.message}"
                )
            )
        }
    }
}
