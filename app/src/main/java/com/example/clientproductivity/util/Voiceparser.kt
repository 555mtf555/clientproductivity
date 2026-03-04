package com.example.clientproductivity.util

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.Instant
import java.util.Locale

data class ParsedTaskInput(
    val title: String = "",
    val notes: String = "",
    val dueInstant: Instant? = null
)

object VoiceParser {

    fun parse(transcript: String): ParsedTaskInput {
        val lower = transcript.lowercase(Locale.getDefault()).trim()

        // Split on keyword boundaries: "notes", "due", "at"
        // e.g. "Call John about proposal notes follow up on budget due friday at 3pm"
        val titleEnd = listOf("notes ", "due ", " at ").minOfOrNull { kw ->
            val idx = lower.indexOf(kw)
            if (idx > 0) idx else Int.MAX_VALUE
        } ?: Int.MAX_VALUE

        val rawTitle = if (titleEnd == Int.MAX_VALUE) transcript.trim()
        else transcript.substring(0, titleEnd).trim()

        // Extract notes — between "notes" and "due" (or end)
        val notesStart = lower.indexOf("notes ")
        val dueStart = lower.indexOf("due ")
        val rawNotes = when {
            notesStart >= 0 -> {
                val end = if (dueStart > notesStart) dueStart else lower.length
                transcript.substring(notesStart + 6, end).trim()
            }
            else -> ""
        }

        // Extract due date/time
        val dueInstant = parseDue(lower, dueStart)

        return ParsedTaskInput(
            title = rawTitle.replaceFirstChar { it.uppercase() },
            notes = rawNotes.replaceFirstChar { it.uppercase() },
            dueInstant = dueInstant
        )
    }

    private fun parseDue(lower: String, dueStart: Int): Instant? {
        if (dueStart < 0) return null
        val dueChunk = lower.substring(dueStart + 4).trim()

        val today = LocalDate.now()
        var time: LocalTime? = null

        // Day keywords
        val date: LocalDate? = when {
            dueChunk.startsWith("today")      -> today
            dueChunk.startsWith("tomorrow")   -> today.plusDays(1)
            dueChunk.contains("monday")       -> nextWeekday(today, 1)
            dueChunk.contains("tuesday")      -> nextWeekday(today, 2)
            dueChunk.contains("wednesday")    -> nextWeekday(today, 3)
            dueChunk.contains("thursday")     -> nextWeekday(today, 4)
            dueChunk.contains("friday")       -> nextWeekday(today, 5)
            dueChunk.contains("saturday")     -> nextWeekday(today, 6)
            dueChunk.contains("sunday")       -> nextWeekday(today, 7)
            dueChunk.contains("next week")    -> today.plusWeeks(1)
            dueChunk.contains("in two weeks") -> today.plusWeeks(2)
            dueChunk.contains("in a week")    -> today.plusWeeks(1)
            dueChunk.contains("end of month") -> today.withDayOfMonth(today.lengthOfMonth())
            else -> null
        }

        // Time — look for "at X pm/am" or "at X:XX"
        val timeRegex = Regex("""at (\d{1,2})(?::(\d{2}))?\s*(am|pm)?""")
        val timeMatch = timeRegex.find(dueChunk)
        if (timeMatch != null) {
            var hour = timeMatch.groupValues[1].toIntOrNull() ?: 9
            val minute = timeMatch.groupValues[2].toIntOrNull() ?: 0
            val ampm = timeMatch.groupValues[3]
            if (ampm == "pm" && hour < 12) hour += 12
            if (ampm == "am" && hour == 12) hour = 0
            time = LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
        }

        val resolvedDate = date ?: return null
        val resolvedTime = time ?: LocalTime.of(9, 0) // default 9am if no time given
        return resolvedDate.atTime(resolvedTime).atZone(ZoneId.systemDefault()).toInstant()
    }

    private fun nextWeekday(from: LocalDate, targetDow: Int): LocalDate {
        // targetDow: 1=Mon … 7=Sun (ISO)
        val currentDow = from.dayOfWeek.value
        val daysUntil = ((targetDow - currentDow + 7) % 7).let { if (it == 0) 7 else it }
        return from.plusDays(daysUntil.toLong())
    }
}