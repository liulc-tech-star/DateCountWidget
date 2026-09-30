package com.datecount.widget.data

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

enum class DisplayMode {
    WEEKS_AND_DAYS,
    TOTAL_DAYS,
    BOTH
}

@Serializable
data class DateEvent(
    val id: String,
    val title: String,
    val dateIso: String,
    val displayMode: DisplayMode = DisplayMode.BOTH,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    val date: LocalDate
        get() = LocalDate.parse(dateIso)
}

data class DateCount(
    val event: DateEvent,
    val totalDays: Long,
    val weeks: Long,
    val leftoverDays: Long,
    val isPast: Boolean,
    val isToday: Boolean
) {
    val directionLabel: String
        get() = when {
            isToday -> "就是今天"
            isPast -> "已经过去"
            else -> "还剩"
        }

    val weeksAndDaysLabel: String
        get() = when {
            isToday -> "0 天"
            leftoverDays == 0L -> "${weeks} 周"
            weeks == 0L -> "${leftoverDays} 天"
            else -> "${weeks} 周 ${leftoverDays} 天"
        }

    val totalDaysLabel: String
        get() = "${kotlin.math.abs(totalDays)} 天"

    val formattedDate: String
        get() = event.date.format(DATE_FORMATTER)
}

private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日")

fun calculateCount(event: DateEvent, today: LocalDate = LocalDate.now()): DateCount {
    val totalDays = ChronoUnit.DAYS.between(event.date, today)
    val absDays = kotlin.math.abs(totalDays)
    return DateCount(
        event = event,
        totalDays = totalDays,
        weeks = absDays / 7,
        leftoverDays = absDays % 7,
        isPast = totalDays > 0,
        isToday = totalDays == 0L
    )
}
