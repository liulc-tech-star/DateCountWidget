package com.datecount.widget.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Serializable(with = DisplayModeSerializer::class)
enum class DisplayMode(val wireName: String) {
    WEEKS_AND_DAYS("WEEKS_AND_DAYS"),
    TOTAL_DAYS("TOTAL_DAYS"),
    BOTH("BOTH");

    companion object {
        private val byWireName by lazy { entries.associateBy { it.wireName } }

        // Unknown values — written by a newer version, or left behind by a constant
        // that was renamed — fall back to the default instead of failing the record.
        fun fromWireName(name: String): DisplayMode = byWireName[name] ?: BOTH
    }
}

// The stored wire name is frozen in place, so the Kotlin constant can be renamed
// without invalidating data that is already on users' devices.
object DisplayModeSerializer : KSerializer<DisplayMode> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        "com.datecount.widget.data.DisplayMode",
        PrimitiveKind.STRING
    )

    override fun serialize(encoder: Encoder, value: DisplayMode) {
        encoder.encodeString(value.wireName)
    }

    override fun deserialize(decoder: Decoder): DisplayMode =
        DisplayMode.fromWireName(decoder.decodeString())
}

/** 桌面插件只能单行显示标题，保存时统一收口长度。 */
const val MAX_EVENT_TITLE_LENGTH = 24

fun normalizeEventTitle(raw: String): String =
    raw.trim().take(MAX_EVENT_TITLE_LENGTH).ifBlank { "未命名日期" }

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

    // 主显示行由事件的显示方式决定，插件、列表和编辑预览共用这一处判断。
    val primaryLabel: String
        get() = when (event.displayMode) {
            DisplayMode.TOTAL_DAYS -> totalDaysLabel
            DisplayMode.WEEKS_AND_DAYS, DisplayMode.BOTH -> weeksAndDaysLabel
        }

    // 只有「都显示」才有第二行。
    val secondaryLabel: String?
        get() = if (event.displayMode == DisplayMode.BOTH) totalDaysLabel else null

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
