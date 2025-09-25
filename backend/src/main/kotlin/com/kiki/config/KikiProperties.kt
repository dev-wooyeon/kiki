package com.kiki.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "kiki")
data class KikiProperties(
    val games: Map<String, GameProperties> = emptyMap()
) {
    data class GameProperties(
        val name: String,
        val baseUrl: String,
        val noticePath: String?
    )
}
