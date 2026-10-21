package com.datecount.widget.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.datecount.widget.asDateCountApp
import com.datecount.widget.data.DateCount
import com.datecount.widget.data.DisplayMode
import com.datecount.widget.data.calculateCount
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

internal val APP_WIDGET_ID_KEY = ActionParameters.Key<Int>(AppWidgetManager.EXTRA_APPWIDGET_ID)

class DateCountWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact
    override val stateDefinition = null

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext.asDateCountApp()
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        suspend fun loadCount(): DateCount? = app.widgetBindings.eventIdFor(appWidgetId)
            ?.let { app.events.getEvent(it) }
            ?.let { calculateCount(it) }
        val initialCount = loadCount()
        val counts = app.widgetBindings.refreshRequests(appWidgetId).map { loadCount() }

        provideContent {
            val count by counts.collectAsState(initialCount)
            WidgetContent(
                count = count,
                appWidgetId = appWidgetId
            )
        }
    }
}

private data class WidgetMetrics(
    val compact: Boolean,
    val padding: Dp,
    val radius: Dp,
    val titleSize: TextUnit,
    val directionSize: TextUnit,
    val countSize: TextUnit,
    val secondarySize: TextUnit,
    val dateSize: TextUnit,
    val gapSmall: Dp,
    val gapMedium: Dp
)

private fun widgetMetrics(size: DpSize, extraLine: Boolean): WidgetMetrics {
    val height = size.height.value
    val compact = height < 88f
    val padding = when {
        height < 70f -> 6f
        height < 120f -> 10f
        else -> (10f + (height - 120f) * 0.04f).coerceAtMost(18f)
    }
    val innerHeight = (height - padding * 2).coerceAtLeast(24f)
    val radius = (12f + innerHeight * 0.04f).coerceIn(12f, 24f)

    if (compact) {
        return WidgetMetrics(
            compact = true,
            padding = padding.dp,
            radius = radius.dp,
            titleSize = (innerHeight * 0.22f).coerceIn(10f, 13f).sp,
            directionSize = 11.sp,
            countSize = (innerHeight * 0.42f).coerceIn(13f, 22f).sp,
            secondarySize = 12.sp,
            dateSize = 10.sp,
            gapSmall = 2.dp,
            gapMedium = 2.dp
        )
    }

    val heightShare = if (extraLine) 0.24f else 0.32f
    val countSp = (innerHeight * heightShare).coerceIn(18f, 52f)
    val titleSp = (countSp * 0.42f).coerceIn(12f, 22f)
    val directionSp = (countSp * 0.38f).coerceIn(11f, 20f)
    val dateSp = (countSp * 0.34f).coerceIn(10f, 18f)
    val secondarySp = (countSp * 0.42f).coerceIn(12f, 22f)
    val gap = (innerHeight * 0.03f).coerceIn(2f, 8f)

    return WidgetMetrics(
        compact = false,
        padding = padding.dp,
        radius = radius.dp,
        titleSize = titleSp.sp,
        directionSize = directionSp.sp,
        countSize = countSp.sp,
        secondarySize = secondarySp.sp,
        dateSize = dateSp.sp,
        gapSmall = (gap * 0.6f).dp,
        gapMedium = gap.dp
    )
}

@Composable
private fun WidgetContent(count: DateCount?, appWidgetId: Int?) {
    val metrics = widgetMetrics(
        size = LocalSize.current,
        extraLine = count?.event?.displayMode == DisplayMode.BOTH
    )
    val bg = GlanceTheme.colors.widgetBackground
    val onBg = GlanceTheme.colors.onSurface
    val muted = GlanceTheme.colors.onSurfaceVariant
    val accent = GlanceTheme.colors.primary
    val clickAction = if (appWidgetId != null && appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
        actionStartActivity<WidgetConfigActivity>(
            actionParametersOf(APP_WIDGET_ID_KEY to appWidgetId)
        )
    } else {
        actionStartActivity<WidgetConfigActivity>()
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(metrics.radius)
            .background(bg)
            .padding(metrics.padding)
            .clickable(clickAction),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (count == null) {
            WidgetText("点这里选择日期", onBg, metrics.titleSize, FontWeight.Medium)
            if (!metrics.compact) {
                Spacer(GlanceModifier.height(metrics.gapMedium))
                WidgetText("选好后会显示在桌面上。", muted, metrics.dateSize)
            }
        } else {
            WidgetText(count.event.title, muted, metrics.titleSize)
            if (metrics.compact) {
                Spacer(GlanceModifier.height(metrics.gapSmall))
                WidgetText(compactCountLine(count), onBg, metrics.countSize, FontWeight.Bold)
            } else {
                Spacer(GlanceModifier.height(metrics.gapMedium))
                WidgetText(count.directionLabel, accent, metrics.directionSize, FontWeight.Medium)
                Spacer(GlanceModifier.height(metrics.gapSmall))
                WidgetText(count.primaryLabel, onBg, metrics.countSize, FontWeight.Bold)
                count.secondaryLabel?.let { label ->
                    Spacer(GlanceModifier.height(metrics.gapSmall))
                    WidgetText(label, muted, metrics.secondarySize)
                }
                Spacer(GlanceModifier.height(metrics.gapMedium))
                WidgetText(count.formattedDate, muted, metrics.dateSize)
            }
        }
    }
}

// Glance 1.1 exposes no TextOverflow API, so every line is pinned to a single line.
// That keeps a long title from wrapping and pushing the count out of the fixed box.
@Composable
private fun WidgetText(
    text: String,
    color: ColorProvider,
    size: TextUnit,
    weight: FontWeight = FontWeight.Normal
) {
    Text(
        text = text,
        style = TextStyle(color = color, fontSize = size, fontWeight = weight),
        maxLines = 1
    )
}

private fun compactCountLine(count: DateCount): String =
    if (count.isToday) count.primaryLabel else "${count.directionLabel} ${count.primaryLabel}"

class DateCountWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DateCountWidget()

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val bindings = context.asDateCountApp().widgetBindings
        val today = LocalDate.now()
        val needsRefresh = appWidgetIds.any {
            (bindings.eventIdFor(it) != null || bindings.lastRefreshDayFor(it) != null) &&
                shouldRefreshWidget(bindings.lastRefreshDayFor(it), today)
        }
        // Register Glance's provider without bypassing the shared date/creation refresh gate.
        super.onUpdate(context, manager, intArrayOf())
        if (needsRefresh) {
            WidgetRefreshWorker.rescheduleAfterClockChange(context)
        } else {
            WidgetRefreshWorker.schedule(context)
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetRefreshWorker.schedule(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetRefreshWorker.cancel(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val bindings = context.applicationContext.asDateCountApp().widgetBindings
        appWidgetIds.forEach { bindings.removeWidget(it) }
    }

    companion object {
        private const val TAG = "DateCountWidget"
        private val refreshMutex = Mutex()

        fun widgetIds(context: Context): IntArray = AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, DateCountWidgetReceiver::class.java))

        suspend fun refreshAllNow(context: Context, force: Boolean = false): Boolean =
            refreshEach(context, widgetIds(context).toList(), force)

        // Editing or deleting an event must repaint the widgets bound to it right away,
        // even when they were already refreshed today.
        suspend fun refreshWidgets(context: Context, appWidgetIds: Collection<Int>) {
            refreshEach(context, appWidgetIds, force = true)
        }

        private suspend fun refreshEach(
            context: Context,
            appWidgetIds: Collection<Int>,
            force: Boolean
        ): Boolean {
            var success = true
            val appContext = context.applicationContext
            appWidgetIds.forEach { appWidgetId ->
                try {
                    refreshWidget(appContext, appWidgetId, force)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    success = false
                    Log.e(TAG, "Unable to refresh widget $appWidgetId", error)
                }
            }
            return success
        }

        suspend fun bindAndRefresh(context: Context, appWidgetId: Int, eventId: String) {
            val appContext = context.applicationContext
            appContext.asDateCountApp().widgetBindings.bind(appWidgetId, eventId)
            refreshWidget(appContext, appWidgetId, force = true)
            WidgetRefreshWorker.schedule(appContext)
        }

        private suspend fun refreshWidget(
            context: Context,
            appWidgetId: Int,
            force: Boolean = false
        ) = refreshMutex.withLock {
            val bindings = context.asDateCountApp().widgetBindings
            val today = LocalDate.now()
            val boundEventId = bindings.eventIdFor(appWidgetId)
            val lastRefreshDay = bindings.lastRefreshDayFor(appWidgetId)
            if (isUnconfiguredWidget(boundEventId, lastRefreshDay, force)) return@withLock
            if (!shouldRefreshWidget(lastRefreshDay, today, force)) return@withLock
            val manager = GlanceAppWidgetManager(context)
            val glanceId = awaitGlanceId(manager, appWidgetId)
                ?: error("Widget $appWidgetId is not available")
            bindings.requestRefresh(appWidgetId)
            DateCountWidget().update(context, glanceId)
            bindings.markRefreshed(appWidgetId, today.toEpochDay())
        }

        private suspend fun awaitGlanceId(
            manager: GlanceAppWidgetManager,
            appWidgetId: Int
        ): GlanceId? {
            repeat(25) { attempt ->
                val glanceId = try {
                    manager.getGlanceIdBy(appWidgetId)
                } catch (_: IllegalArgumentException) {
                    null
                }
                if (glanceId != null) return glanceId
                delay(80L * (attempt + 1))
            }
            return null
        }
    }
}
