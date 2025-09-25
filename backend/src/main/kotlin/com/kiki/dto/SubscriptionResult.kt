package com.kiki.dto

import com.kiki.entity.Subscriber

/**
 * Result of subscription operation
 */
data class SubscriptionResult(
    val success: Boolean,
    val message: String,
    val subscriber: Subscriber? = null
)