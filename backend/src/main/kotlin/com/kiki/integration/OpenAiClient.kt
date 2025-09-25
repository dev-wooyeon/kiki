package com.kiki.integration

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException
import java.time.Duration

@Component
class OpenAiClient(
    restTemplateBuilder: RestTemplateBuilder,
    @Value("\${kiki.openai.api-key:}") private val apiKey: String,
    @Value("\${kiki.openai.model:gpt-4o-mini}") private val model: String
) {

    private val logger = LoggerFactory.getLogger(OpenAiClient::class.java)
    private val restTemplate = restTemplateBuilder
        .setConnectTimeout(Duration.ofSeconds(10))
        .setReadTimeout(Duration.ofSeconds(30))
        .build()

    fun generateSummary(systemPrompt: String, userPrompt: String, maxTokens: Int = 400): String? {
        if (apiKey.isBlank()) {
            logger.debug("OpenAI API key is not configured. Skipping AI summary generation.")
            return null
        }

        return try {
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                setBearerAuth(apiKey)
            }

            val requestBody = ChatCompletionRequest(
                model = model,
                messages = listOf(
                    Message(role = "system", content = systemPrompt),
                    Message(role = "user", content = userPrompt)
                ),
                temperature = 0.2,
                maxTokens = maxTokens
            )

            val entity = HttpEntity(requestBody, headers)
            val response: ResponseEntity<ChatCompletionResponse> = restTemplate.postForEntity(
                OPENAI_CHAT_COMPLETIONS_URL,
                entity,
                ChatCompletionResponse::class.java
            )

            response.body?.choices?.firstOrNull()?.message?.content?.trim()?.ifBlank { null }
        } catch (ex: RestClientException) {
            logger.warn("Failed to call OpenAI API: {}", ex.message)
            null
        } catch (ex: Exception) {
            logger.warn("Unexpected error while calling OpenAI API", ex)
            null
        }
    }

    companion object {
        private const val OPENAI_CHAT_COMPLETIONS_URL = "https://api.openai.com/v1/chat/completions"
    }

    data class ChatCompletionRequest(
        val model: String,
        val messages: List<Message>,
        val temperature: Double,
        val maxTokens: Int
    )

    data class Message(
        val role: String,
        val content: String
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class ChatCompletionResponse(
        val choices: List<Choice> = emptyList()
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        data class Choice(
            val message: MessageResponse? = null
        )

        @JsonIgnoreProperties(ignoreUnknown = true)
        data class MessageResponse(
            val role: String? = null,
            val content: String? = null
        )
    }
}
