package com.blackclaw.android.tool.impl

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleTaskToolTest {
    private val now = Calendar.getInstance().apply {
        set(2026, Calendar.JULY, 18, 16, 0, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    @Test fun `parses English timer`() {
        assertEquals(now + 30 * 60_000L, ScheduleTaskTool().parseWhen("in 30 minutes", now))
    }

    @Test fun `parses seconds for short reminders`() {
        assertEquals(now + 45 * 1_000L, ScheduleTaskTool().parseWhen("in 45 seconds", now))
    }

    @Test fun `parses English today and tomorrow clock`() {
        val today = Calendar.getInstance().apply { timeInMillis = now; set(Calendar.HOUR_OF_DAY, 17) }.timeInMillis
        val tomorrow = Calendar.getInstance().apply { timeInMillis = today; add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis
        assertEquals(today, ScheduleTaskTool().parseWhen("today at 17:00", now))
        assertEquals(tomorrow, ScheduleTaskTool().parseWhen("tomorrow at 17:00", now))
    }
}
