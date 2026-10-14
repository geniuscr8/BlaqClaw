package com.blackclaw.android.agent

import org.junit.Assert.assertEquals
import org.junit.Test

class TokenMonitorTest {

    @Test
    fun `tracks latest and average model latency`() {
        val monitor = TokenMonitor("test-model")
        monitor.record(1, inputTokens = 10, outputTokens = 5, totalTokenCount = 15, latencyMs = 120)
        monitor.record(2, inputTokens = 8, outputTokens = 4, totalTokenCount = 12, latencyMs = 280)

        val status = monitor.getStatus()
        assertEquals(280L, status.lastLatencyMs)
        assertEquals(200L, status.averageLatencyMs)
    }
}
