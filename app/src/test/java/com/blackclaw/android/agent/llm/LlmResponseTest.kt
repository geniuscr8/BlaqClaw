package com.blackclaw.android.agent.llm

import dev.langchain4j.agent.tool.ToolExecutionRequest
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test

class LlmResponseTest {

    @Test
    fun emptyResponseIsRejected() {
        assertThrowsIllegalState {
            LlmResponse(text = "", toolExecutionRequests = emptyList()).requireUsable()
        }
    }

    @Test
    fun textResponseIsAccepted() {
        val response = LlmResponse(text = "Ready", toolExecutionRequests = emptyList())
        assertSame(response, response.requireUsable())
    }

    @Test
    fun toolOnlyResponseIsAccepted() {
        val request = ToolExecutionRequest.builder()
            .id("test")
            .name("finish")
            .arguments("{}")
            .build()
        val response = LlmResponse(text = null, toolExecutionRequests = listOf(request))
        assertSame(response, response.requireUsable())
    }

    private fun assertThrowsIllegalState(block: () -> Unit) {
        try {
            block()
            fail("Expected IllegalStateException")
        } catch (_: IllegalStateException) {
            // Expected.
        }
    }
}
