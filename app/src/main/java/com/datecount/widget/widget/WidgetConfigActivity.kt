package com.datecount.widget.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.datecount.widget.asDateCountApp
import com.datecount.widget.data.DateCount
import com.datecount.widget.data.DisplayMode
import com.datecount.widget.ui.EventEditorDialog
import com.datecount.widget.ui.EventEditorState
import com.datecount.widget.ui.EventViewModel
import com.datecount.widget.ui.theme.DateCountTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalDate

class WidgetConfigActivity : ComponentActivity() {
    private val appWidgetId: Int by lazy {
        intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val alreadyBound = application.asDateCountApp().widgetBindings.eventIdFor(appWidgetId)
        val resultIntent = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        if (alreadyBound != null) {
            setResult(RESULT_OK, resultIntent)
        } else {
            setResult(RESULT_CANCELED)
        }

        enableEdgeToEdge()
        setContent {
            DateCountTheme {
                WidgetConfigScreen(
                    selectedEventId = alreadyBound,
                    onCancel = { finish() },
                    onPick = { eventId -> bind(eventId) },
                    onCreate = { title, date, mode -> createAndBind(title, date, mode) }
                )
            }
        }
    }

    // With singleTop the launcher can route a second widget's click into this instance.
    // Recreating from the new intent stops appWidgetId from staying on the previous widget.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val incomingId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        if (incomingId == appWidgetId) return
        setIntent(intent)
        recreate()
    }

    private fun bind(eventId: String) {
        lifecycleScope.launch {
            try {
                DateCountWidgetReceiver.bindAndRefresh(applicationContext, appWidgetId, eventId)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e(TAG, "Unable to bind widget $appWidgetId", error)
            } finally {
                finishWithWidget()
            }
        }
    }

    private fun createAndBind(title: String, date: LocalDate, mode: DisplayMode) {
        lifecycleScope.launch {
            try {
                val saved = application.asDateCountApp().events.save(null, title, date, mode)
                DateCountWidgetReceiver.bindAndRefresh(applicationContext, appWidgetId, saved.id)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e(TAG, "Unable to create and bind widget $appWidgetId", error)
            } finally {
                finishWithWidget()
            }
        }
    }

    private fun finishWithWidget() {
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        finish()
    }

    companion object {
        private const val TAG = "WidgetConfigActivity"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetConfigScreen(
    selectedEventId: String?,
    onCancel: () -> Unit,
    onPick: (String) -> Unit,
    onCreate: (String, LocalDate, DisplayMode) -> Unit,
    viewModel: EventViewModel = viewModel()
) {
    val events by viewModel.events.collectAsStateWithLifecycle()
    var editor by remember { mutableStateOf<EventEditorState?>(null) }
    var applying by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("选择要显示的日期") },
                navigationIcon = {
                    TextButton(onClick = onCancel, enabled = !applying) { Text("取消") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { if (!applying) editor = EventEditorState() }
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "添加日期")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (events.isEmpty()) {
                Text(
                    "还没有日期。点右下角加号先添加一个，添加后会直接显示到这个插件上。",
                    modifier = Modifier.padding(24.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(events, key = { it.event.id }) { count ->
                        EventPickRow(
                            count = count,
                            selected = count.event.id == selectedEventId,
                            enabled = !applying,
                            onClick = {
                                applying = true
                                onPick(count.event.id)
                            }
                        )
                    }
                }
            }

            if (applying) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }

    editor?.let { state ->
        EventEditorDialog(
            state = state,
            onDismiss = { if (!applying) editor = null },
            onSave = { title, date, mode ->
                applying = true
                editor = null
                onCreate(title, date, mode)
            }
        )
    }
}

@Composable
private fun EventPickRow(
    count: DateCount,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        ListItem(
            headlineContent = { Text(count.event.title) },
            supportingContent = {
                Text("${count.formattedDate} · ${count.directionLabel} ${count.weeksAndDaysLabel}")
            },
            trailingContent = {
                if (selected) {
                    Text("当前", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }
}
