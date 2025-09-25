package com.kiki.service

import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class SystemHealthService(
    private val jdbcTemplate: JdbcTemplate,
    private val schedulingService: SchedulingService
) {

    private val logger = LoggerFactory.getLogger(SystemHealthService::class.java)

    fun getHealthStatus(): HealthStatus {
        val timestamp = LocalDateTime.now()
        val databaseStatus = checkDatabase()
        val schedulingStatus = schedulingService.getSchedulingStatus()
        val schedulerComponent = ComponentStatus(
            status = when (schedulingStatus.metrics.lastRunSuccess) {
                true -> HealthState.UP
                false -> HealthState.DEGRADED
                null -> HealthState.DEGRADED
            },
            message = when (schedulingStatus.metrics.lastRunSuccess) {
                true -> "Last run succeeded"
                false -> schedulingStatus.metrics.lastRunErrorMessage ?: "Last run failed"
                null -> "No run has completed yet"
            },
            metadata = schedulingStatus
        )

        val components = mapOf(
            "database" to databaseStatus,
            "scheduler" to schedulerComponent
        )

        val overall = when {
            components.values.any { it.status == HealthState.DOWN } -> HealthState.DOWN
            components.values.any { it.status == HealthState.DEGRADED } -> HealthState.DEGRADED
            else -> HealthState.UP
        }

        return HealthStatus(
            status = overall,
            timestamp = timestamp,
            components = components
        )
    }

    private fun checkDatabase(): ComponentStatus {
        return try {
            jdbcTemplate.queryForObject("SELECT 1", Int::class.java)
            ComponentStatus(status = HealthState.UP, message = "OK")
        } catch (ex: Exception) {
            logger.error("Database health check failed", ex)
            ComponentStatus(status = HealthState.DOWN, message = ex.message ?: "Database check failed")
        }
    }
}

data class HealthStatus(
    val status: HealthState,
    val timestamp: LocalDateTime,
    val components: Map<String, ComponentStatus>
)

data class ComponentStatus(
    val status: HealthState,
    val message: String,
    val metadata: Any? = null
)

enum class HealthState {
    UP,
    DEGRADED,
    DOWN
}
