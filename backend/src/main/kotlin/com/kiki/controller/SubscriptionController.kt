package com.kiki.controller

import com.kiki.dto.ApiResponse
import com.kiki.dto.SubscribeRequest
import com.kiki.service.SubscriptionService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

/**
 * REST Controller for subscription management
 */
@RestController
@Validated
class SubscriptionController(
    private val subscriptionService: SubscriptionService
) {
    
    private val logger = LoggerFactory.getLogger(SubscriptionController::class.java)
    
    /**
     * Subscribe to email notifications
     * 
     * POST /subscribe
     * 
     * @param request SubscribeRequest containing email address
     * @return ResponseEntity with subscription result
     */
    @PostMapping("/subscribe")
    fun subscribe(@Valid @RequestBody request: SubscribeRequest): ResponseEntity<ApiResponse> {
        logger.info("Received subscription request for email: {}", request.email)

        try {
            val result = subscriptionService.subscribe(request.email)
            
            return if (result.success) {
                logger.info("Successfully subscribed email: {}", request.email)
                ResponseEntity.ok(
                    ApiResponse(
                        success = true,
                        message = result.message
                    )
                )
            } else {
                logger.warn("Failed to subscribe email: {} - {}", request.email, result.message)
                ResponseEntity.badRequest().body(
                    ApiResponse(
                        success = false,
                        message = result.message
                    )
                )
            }
        } catch (e: Exception) {
            logger.error("Unexpected error during subscription for email: {}", request.email, e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
                )
            )
        }
    }

    /**
     * 구독 현황 조회
     *
     * GET /api/subscribe/stats
     */
    @GetMapping("/subscribe/stats")
    fun getSubscriptionStats(): ResponseEntity<ApiResponse> {
        val activeCount = subscriptionService.getActiveSubscriberCount()
        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "구독자 현황",
                data = mapOf("activeCount" to activeCount)
            )
        )
    }

    /**
     * Unsubscribe from email notifications using token
     * 
     * GET /api/unsubscribe/{token}
     * 
     * @param token Unsubscribe token
     * @return ResponseEntity with unsubscribe result
     */
    @GetMapping("/unsubscribe/{token}")
    fun unsubscribe(@PathVariable token: String): ResponseEntity<ApiResponse> {
        logger.info("Received unsubscribe request with token: {}", token)
        
        if (token.isBlank()) {
            logger.warn("Blank unsubscribe token provided")
            return ResponseEntity.badRequest().body(
                ApiResponse(
                    success = false,
                    message = "유효하지 않은 구독 취소 링크입니다."
                )
            )
        }
        
        try {
            val result = subscriptionService.unsubscribe(token)
            
            return if (result) {
                logger.info("Successfully unsubscribed with token: {}", token)
                ResponseEntity.ok(
                    ApiResponse(
                        success = true,
                        message = "구독이 성공적으로 취소되었습니다."
                    )
                )
            } else {
                logger.warn("Failed to unsubscribe with token: {}", token)
                ResponseEntity.badRequest().body(
                    ApiResponse(
                        success = false,
                        message = "유효하지 않은 구독 취소 링크입니다."
                    )
                )
            }
        } catch (e: Exception) {
            logger.error("Unexpected error during unsubscribe with token: {}", token, e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
                )
            )
        }
    }
    
    /**
     * Health check endpoint
     * 
     * GET /api/health
     * 
     * @return ResponseEntity with health status
     */
    @GetMapping("/health")
    fun health(): ResponseEntity<ApiResponse> {
        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Service is healthy"
            )
        )
    }
}
