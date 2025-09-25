package com.kiki.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.kiki.dto.SubscribeRequest
import com.kiki.dto.SubscriptionResult
import com.kiki.entity.Subscriber
import com.kiki.service.SubscriptionService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDateTime

@WebMvcTest(SubscriptionController::class)
class SubscriptionControllerTest {
    
    @Autowired
    private lateinit var mockMvc: MockMvc
    
    @Autowired
    private lateinit var objectMapper: ObjectMapper
    
    @Autowired
    private lateinit var subscriptionService: SubscriptionService
    
    @TestConfiguration
    class TestConfig {
        @Bean
        @Primary
        fun subscriptionService(): SubscriptionService = mockk()
    }
    
    @Test
    fun `POST subscribe - should return success for valid email`() {
        // Given
        val email = "test@example.com"
        val request = SubscribeRequest(email = email)
        val subscriber = Subscriber(
            id = 1L,
            email = email,
            unsubscribeToken = "token123",
            isActive = true,
            subscribedAt = LocalDateTime.now()
        )
        val subscriptionResult = SubscriptionResult(
            success = true,
            message = "구독이 성공적으로 등록되었습니다",
            subscriber = subscriber
        )
        
        every { subscriptionService.subscribe(email) } returns subscriptionResult
        
        // When & Then
        mockMvc.perform(
            post("/subscribe")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("구독이 성공적으로 등록되었습니다"))
        
        verify { subscriptionService.subscribe(email) }
    }
    
    @Test
    fun `POST subscribe - should return bad request for invalid email format`() {
        // Given
        val invalidEmail = "invalid-email"
        val request = SubscribeRequest(email = invalidEmail)
        
        // When & Then
        mockMvc.perform(
            post("/subscribe")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }
    
    @Test
    fun `POST subscribe - should return bad request for empty email`() {
        // Given
        val emptyEmail = ""
        val request = SubscribeRequest(email = emptyEmail)
        
        // When & Then
        mockMvc.perform(
            post("/subscribe")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }
    
    @Test
    fun `POST subscribe - should return bad request for already subscribed email`() {
        // Given
        val email = "test@example.com"
        val request = SubscribeRequest(email = email)
        val subscriptionResult = SubscriptionResult(
            success = false,
            message = "이미 구독된 이메일입니다"
        )
        
        every { subscriptionService.subscribe(email) } returns subscriptionResult
        
        // When & Then
        mockMvc.perform(
            post("/subscribe")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("이미 구독된 이메일입니다"))
        
        verify { subscriptionService.subscribe(email) }
    }
    
    @Test
    fun `POST subscribe - should return internal server error when service throws exception`() {
        // Given
        val email = "test@example.com"
        val request = SubscribeRequest(email = email)
        
        every { subscriptionService.subscribe(email) } throws RuntimeException("Database error")
        
        // When & Then
        mockMvc.perform(
            post("/subscribe")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요."))
        
        verify { subscriptionService.subscribe(email) }
    }
    
    @Test
    fun `GET unsubscribe - should return success for valid token`() {
        // Given
        val token = "validtoken123"
        
        every { subscriptionService.unsubscribe(token) } returns true
        
        // When & Then
        mockMvc.perform(get("/unsubscribe/{token}", token))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("구독이 성공적으로 취소되었습니다."))
        
        verify { subscriptionService.unsubscribe(token) }
    }
    
    @Test
    fun `GET unsubscribe - should return bad request for invalid token`() {
        // Given
        val invalidToken = "invalidtoken"
        
        every { subscriptionService.unsubscribe(invalidToken) } returns false
        
        // When & Then
        mockMvc.perform(get("/unsubscribe/{token}", invalidToken))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("유효하지 않은 구독 취소 링크입니다."))
        
        verify { subscriptionService.unsubscribe(invalidToken) }
    }
    
    @Test
    fun `GET unsubscribe - should return bad request for blank token`() {
        // Given
        val blankToken = "   "
        
        every { subscriptionService.unsubscribe(blankToken) } returns false
        
        // When & Then
        mockMvc.perform(get("/unsubscribe/{token}", blankToken))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("유효하지 않은 구독 취소 링크입니다."))
    }
    
    @Test
    fun `GET unsubscribe - should return internal server error when service throws exception`() {
        // Given
        val token = "validtoken123"
        
        every { subscriptionService.unsubscribe(token) } throws RuntimeException("Database error")
        
        // When & Then
        mockMvc.perform(get("/unsubscribe/{token}", token))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요."))
        
        verify { subscriptionService.unsubscribe(token) }
    }
    
    @Test
    fun `GET health - should return success`() {
        // When & Then
        mockMvc.perform(get("/health"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Service is healthy"))
    }
}
