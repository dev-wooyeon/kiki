package com.kiki.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.kiki.dto.SubscribeRequest
import com.kiki.entity.Subscriber
import com.kiki.repository.SubscriberRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class SubscriptionControllerIntegrationTest {
    
    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext
    
    @Autowired
    private lateinit var subscriberRepository: SubscriberRepository
    
    @Autowired
    private lateinit var objectMapper: ObjectMapper
    
    private lateinit var mockMvc: MockMvc
    
    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        subscriberRepository.deleteAll()
    }
    
    @Test
    fun `full subscription flow - subscribe and unsubscribe`() {
        val email = "integration-test@example.com"
        val subscribeRequest = SubscribeRequest(email = email)
        
        // Step 1: Subscribe
        mockMvc.perform(
            post("/subscribe")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subscribeRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("구독이 성공적으로 등록되었습니다"))
        
        // Verify subscriber was created
        val subscriber = subscriberRepository.findByEmail(email)
        assert(subscriber != null)
        assert(subscriber!!.isActive)
        assert(subscriber.unsubscribeToken.isNotBlank())
        
        // Step 2: Try to subscribe again (should fail)
        mockMvc.perform(
            post("/subscribe")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subscribeRequest))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("이미 구독된 이메일입니다"))
        
        // Step 3: Unsubscribe using token
        val unsubscribeToken = subscriber.unsubscribeToken
        mockMvc.perform(get("/unsubscribe/{token}", unsubscribeToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("구독이 성공적으로 취소되었습니다."))
        
        // Verify subscriber was deactivated
        val unsubscribedSubscriber = subscriberRepository.findByEmail(email)
        assert(unsubscribedSubscriber != null)
        assert(!unsubscribedSubscriber!!.isActive)
        
        // Step 4: Try to unsubscribe again (should still succeed)
        mockMvc.perform(get("/unsubscribe/{token}", unsubscribeToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("구독이 성공적으로 취소되었습니다."))
        
        // Step 5: Subscribe again after unsubscribing (should reactivate)
        mockMvc.perform(
            post("/subscribe")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subscribeRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("구독이 성공적으로 등록되었습니다"))
        
        // Verify subscriber was reactivated with new token
        val reactivatedSubscriber = subscriberRepository.findByEmail(email)
        assert(reactivatedSubscriber != null)
        assert(reactivatedSubscriber!!.isActive)
        assert(reactivatedSubscriber.unsubscribeToken != unsubscribeToken) // New token should be generated
    }
    
    @Test
    fun `subscribe with invalid email format should return validation error`() {
        val invalidRequest = SubscribeRequest(email = "invalid-email")
        
        mockMvc.perform(
            post("/subscribe")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
        )
            .andExpect(status().isBadRequest)
        
        // Verify no subscriber was created
        val subscribers = subscriberRepository.findAll()
        assert(subscribers.isEmpty())
    }
    
    @Test
    fun `unsubscribe with invalid token should return error`() {
        val invalidToken = "invalid-token-123"
        
        mockMvc.perform(get("/unsubscribe/{token}", invalidToken))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("유효하지 않은 구독 취소 링크입니다."))
    }
    
    @Test
    fun `health endpoint should return success`() {
        mockMvc.perform(get("/health"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Service is healthy"))
    }
}
