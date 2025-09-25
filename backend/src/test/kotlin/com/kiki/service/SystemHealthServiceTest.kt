package com.kiki.service

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDateTime
import kotlin.test.assertEquals

class SystemHealthServiceTest {

    private lateinit var jdbcTemplate: JdbcTemplate
    private lateinit var schedulingService: SchedulingService
    private lateinit var systemHealthService: SystemHealthService

    @BeforeEach
    fun setUp() {
        jdbcTemplate = mockk()
        schedulingService = mockk()
        systemHealthService = SystemHealthService(jdbcTemplate, schedulingService)
    }

    @Test
    fun `health status is UP when all components succeed`() {
        every { jdbcTemplate.queryForObject("SELECT 1", Int::class.java) } returns 1
        val info = SchedulingInfo(
            intervalMs = 1800000L,
            intervalMinutes = 30L,
            nextExecutionEstimate = LocalDateTime.now().plusMinutes(30),
            isEnabled = true
        )
        val metrics = SchedulingMetrics(lastRunSuccess = true)
        every { schedulingService.getSchedulingStatus() } returns SchedulingStatus(info, metrics)

        val status = systemHealthService.getHealthStatus()

        assertEquals(HealthState.UP, status.status)
        assertEquals(HealthState.UP, status.components["database"]?.status)
        assertEquals(HealthState.UP, status.components["scheduler"]?.status)
    }

    @Test
    fun `health status is DOWN when database check fails`() {
        every { jdbcTemplate.queryForObject("SELECT 1", Int::class.java) } throws RuntimeException("db down")
        val info = SchedulingInfo(
            intervalMs = 1800000L,
            intervalMinutes = 30L,
            nextExecutionEstimate = LocalDateTime.now().plusMinutes(30),
            isEnabled = true
        )
        val metrics = SchedulingMetrics(lastRunSuccess = true)
        every { schedulingService.getSchedulingStatus() } returns SchedulingStatus(info, metrics)

        val status = systemHealthService.getHealthStatus()

        assertEquals(HealthState.DOWN, status.status)
        assertEquals(HealthState.DOWN, status.components["database"]?.status)
    }
}
