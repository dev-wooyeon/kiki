package com.kiki.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

/**
 * Request DTO for subscription
 */
data class SubscribeRequest(
    @field:NotBlank(message = "이메일 주소는 필수입니다")
    @field:Email(message = "올바른 이메일 형식이 아닙니다")
    val email: String
)