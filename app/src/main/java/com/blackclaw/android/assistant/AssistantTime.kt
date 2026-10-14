package com.blackclaw.android.assistant

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Lenient natural-ish datetime parsing shared by the assistant tools and UI.
 * Accepts what the LLM tends to emit: "2026-12-31 23:59", "tomorrow 09:00",
 * "today 14:30", "in 30m", "in 2h", or a bare "07:30" (next occurrence).
 *
 * English-only natural-language support keeps parsing deterministic for the
 * English app interface and assistant prompts.
 */
object AssistantTime {

    private val WEEKDAY_MAP = mapOf(
        "monday" to Calendar.MONDAY, "tuesday" to Calendar.TUESDAY,
        "wednesday" to Calendar.WEDNESDAY, "thursday" to Calendar.THURSDAY,
        "friday" to Calendar.FRIDAY, "saturday" to Calendar.SATURDAY,
        "sunday" to Calendar.SUNDAY,
    )

    /** Returns epoch ms, or 0 if unparseable. */
    fun parse(input: String?): Long {
        if (input.isNullOrBlank()) return 0L
        val s = input.trim().lowercase()
        val now = System.currentTimeMillis()

        // Relative: "in 30m", "in 3 days", "in 3 weeks", "in 2 months"
        Regex("""in\s+(\d+)\s*(weeks?|w|months?|seconds?|s|minutes?|min|m|hours?|hrs?|h|days?|d)\b""")
            .find(s)?.let { m ->
                val n = m.groupValues[1].toInt()
                val unit = m.groupValues[2]
                val cal = Calendar.getInstance()
                val isDateUnit = when {
                    unit == "w" || unit.startsWith("week") -> { cal.add(Calendar.WEEK_OF_YEAR, n); true }
                    unit == "month" || unit == "months" -> { cal.add(Calendar.MONTH, n); true }
                    unit.startsWith("second") || unit == "s" -> { cal.add(Calendar.SECOND, n); false }
                    unit.startsWith("minute") || unit == "min" || unit == "m" -> { cal.add(Calendar.MINUTE, n); false }
                    unit.startsWith("h") -> { cal.add(Calendar.HOUR_OF_DAY, n); false }
                    unit.startsWith("d") -> { cal.add(Calendar.DAY_OF_YEAR, n); true }
                    else -> { return@let }
                }
                // For day/week/month offsets, honor an explicit clock time if the
                // user gave one ("in 3 weeks at 5"), else default to 09:00.
                if (isDateUnit) {
                    val hm = Regex("""(\d{1,2}):(\d{2})""").find(s)
                    val spoken = if (hm == null) parseSpokenHour(
                        s.substringAfter(m.value).ifBlank { s }) else null
                    when {
                        hm != null -> {
                            cal.set(Calendar.HOUR_OF_DAY, hm.groupValues[1].toInt().coerceIn(0, 23))
                            cal.set(Calendar.MINUTE, hm.groupValues[2].toInt().coerceIn(0, 59))
                        }
                        spoken != null -> {
                            cal.set(Calendar.HOUR_OF_DAY, spoken.first); cal.set(Calendar.MINUTE, spoken.second)
                        }
                        else -> { cal.set(Calendar.HOUR_OF_DAY, 9); cal.set(Calendar.MINUTE, 0) }
                    }
                    cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                }
                return cal.timeInMillis
            }

        // Absolute formats.
        val patterns = listOf("yyyy-MM-dd HH:mm", "yyyy-MM-dd'T'HH:mm", "yyyy/MM/dd HH:mm", "dd/MM/yyyy HH:mm")
        for (p in patterns) {
            runCatching {
                SimpleDateFormat(p, Locale.getDefault()).parse(input.trim())?.let { return it.time }
            }
        }

        // Extract time component (HH:mm) — used in combination with day resolution below
        val timeMatch = Regex("""(\d{1,2}):(\d{2})""").find(s)
        var h: Int
        var min: Int
        var haveTime = true
        if (timeMatch != null) {
            h = timeMatch.groupValues[1].toInt().coerceIn(0, 23)
            min = timeMatch.groupValues[2].toInt().coerceIn(0, 59)
        } else {
            val spoken = parseSpokenHour(s)
            if (spoken != null) {
                h = spoken.first; min = spoken.second
            } else {
                // No time found — if we can identify a day, default to 09:00
                h = 9; min = 0; haveTime = false
            }
        }

        // Resolve the day
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, min)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }

        // "day after tomorrow"
        if (s.contains("day after tomorrow")) {
            cal.add(Calendar.DAY_OF_YEAR, 2)
            return cal.timeInMillis
        }

        // "tomorrow"
        if (s.contains("tomorrow")) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            return cal.timeInMillis
        }

        // "today"
        if (s.contains("today")) {
            // Keep today even if the time has passed because the user explicitly said "today".
            return cal.timeInMillis
        }

        // Weekday: "on Monday", "next Friday"
        for ((name, dow) in WEEKDAY_MAP) {
            if (s.contains(name)) {
                val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
                var daysAhead = (dow - today + 7) % 7
                if (daysAhead == 0) daysAhead = 7  // If today is Monday, resolve Monday to next week.
                // But if time hasn't passed and they just say the day name, could mean today
                // Only advance if time already passed today for same-day references
                cal.add(Calendar.DAY_OF_YEAR, daysAhead)
                return cal.timeInMillis
            }
        }

        // Bare "HH:mm" — next occurrence (today if future, tomorrow if past)
        if (haveTime) {
            if (cal.timeInMillis <= now) cal.add(Calendar.DAY_OF_YEAR, 1)
            return cal.timeInMillis
        }

        // date only "yyyy-MM-dd" (add default time 09:00 if no time component)
        runCatching {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(input.trim())?.let {
                val dateCal = Calendar.getInstance().apply {
                    time = it
                    set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                return dateCal.timeInMillis
            }
        }
        return 0L
    }

    /**
     * Parse a spoken-style hour without a colon, common in voice/chat:
     * "at 7", "half past 7", "7 in the evening", "7pm", "quarter past 7",
     * "quarter to 7", "noon", "midnight". Returns (hour24, minute) or
     * null if no hour is found. Applies English AM/PM heuristics.
     *  - bare small hours (1..7) with no qualifier are left as stated; callers
     *    treat them as the next occurrence.
     */
    private fun parseSpokenHour(s: String): Pair<Int, Int>? {
        if (s.contains("noon")) return 12 to 0
        if (s.contains("midnight")) return 0 to 0

        // Find an hour number, ideally near "at" or a qualifier.
        val m = Regex("""(?:at\s+)?(\d{1,2})\b""").find(s) ?: return null
        var hour = m.groupValues[1].toIntOrNull() ?: return null
        if (hour !in 0..23) return null

        var minute = 0
        when {
            s.contains("half past") -> minute = 30
            s.contains("quarter past") -> minute = 15
            s.contains("quarter to") -> { minute = 45; hour = (hour + 23) % 24 }
            else -> Regex("""(?:past|and)\s+(\d{1,2})""").find(s)?.let {
                val mm = it.groupValues[1].toIntOrNull()
                if (mm != null && mm in 1..59) minute = mm
            }
        }

        // AM/PM heuristics.
        val pm = s.contains("evening") || s.contains("night") ||
            Regex("""\d\s*pm""").containsMatchIn(s) || s.contains(" pm")
        val am = s.contains("morning") ||
            Regex("""\d\s*am""").containsMatchIn(s) || s.contains(" am")
        if (pm && hour < 12) hour += 12
        if (am && hour == 12) hour = 0
        return hour.coerceIn(0, 23) to minute.coerceIn(0, 59)
    }

    fun format(ms: Long): String {        if (ms <= 0) return "—"
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = ms }
        val df = if (now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)) {
            SimpleDateFormat("'today' HH:mm", Locale.ENGLISH)
        } else if (now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) + 1 == target.get(Calendar.DAY_OF_YEAR)) {
            SimpleDateFormat("'tomorrow' HH:mm", Locale.ENGLISH)
        } else {
            SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())
        }
        return df.format(Date(ms))
    }
}
