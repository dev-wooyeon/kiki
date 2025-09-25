package com.kiki

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class KikiApplicationTests {

    @Test
    fun contextLoads() {
        // This test verifies that the Spring application context loads successfully
    }
}