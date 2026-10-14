package com.blackclaw.android.agent.llm

import com.blackclaw.android.agent.AgentConfig
import com.blackclaw.android.agent.LlmProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomaticModelManagerTest {

    @Test
    fun `health score prefers fast recent successes over failures`() {
        val now = 1_000_000L
        val fast = AutomaticModelManager.BenchmarkResult(
            key = "fast", displayName = "Fast", kind = AutomaticModelManager.Kind.CLOUD,
            success = true, latencyMs = 400, testedAt = now,
        )
        val failed = AutomaticModelManager.BenchmarkResult(
            key = "failed", displayName = "Failed", kind = AutomaticModelManager.Kind.CLOUD,
            success = false, latencyMs = 1_000, testedAt = now,
        )

        assertTrue(
            AutomaticModelManager.healthScore(fast, now) >
                AutomaticModelManager.healthScore(failed, now)
        )
    }

    @Test
    fun `unknown candidates receive a neutral health score`() {
        assertEquals(50, AutomaticModelManager.healthScore(null, 1_000_000L))
    }

    @Test
    fun `custom endpoint remains in AUTO candidate list`() {
        val candidate = AutomaticModelManager.candidateForConfig(
            AgentConfig(
                apiKey = "key",
                baseUrl = "https://gateway.example/v1/",
                modelName = "my-model",
                provider = LlmProvider.OPENAI,
            )
        )

        assertNotNull(candidate)
        assertEquals("CLOUD:CUSTOM:my-model", candidate!!.key)
        assertEquals("https://gateway.example/v1/", candidate.baseUrl)
    }

    @Test
    fun `local candidate uses a stable canonical path`() {
        val candidate = AutomaticModelManager.candidateForConfig(
            AgentConfig(
                apiKey = "",
                baseUrl = "/tmp/../tmp/blaqclaw-model.litertlm",
                modelName = "gemma4-e2b",
                provider = LlmProvider.LOCAL,
            )
        )

        assertNotNull(candidate)
        assertEquals(AutomaticModelManager.Kind.LOCAL, candidate!!.kind)
        assertEquals("LOCAL:/tmp/blaqclaw-model.litertlm", candidate.key)
    }
}
