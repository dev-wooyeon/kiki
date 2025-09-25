package com.kiki.dto

/**
 * Response DTO for API responses
 */
data class ApiResponse(
    val success: Boolean,
    val message: String,
    val data: Any? = null
)