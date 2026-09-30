package com.datecount.widget.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.datecount.widget.data.DateCount
import com.datecount.widget.data.DateEvent
import com.datecount.widget.data.DisplayMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateCountApp(viewModel: EventViewModel = viewModel()) {
    val events by viewModel.events.collectAsStateWithLifecycle()
    var editor by remember { mutableStateOf<EventEditorState?>(null) }
    var pendingDelete by remember { mutableStateOf<DateEvent?>(null) }
    var showHelp by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("日期计数") },
                actions = {
                    IconButton(onClick = { showHelp = true }) {
                        Icon(Icons.Outlined.Widgets, contentDescription = "插件说明")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editor = EventEditorState() }) {
                Icon(Icons.Outlined.Add, contentDescription = "添加日期")
            }
        }
    ) { padding ->
        if (events.isEmpty()) {
            EmptyState(Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(events, key = { it.event.id }) { count ->
                    EventCard(
                        count = count,
                        onEdit = { editor = EventEditorState.from(count.event) },
                        onDelete = { pendingDelete = count.event }
                    )
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }

    editor?.let { state ->
        EventEditorDialog(
            state = state,
            onDismiss = { editor = null },
            onSave = { title, date, mode ->
                viewModel.save(state.id, title, date, mode)
                editor = null
            }
        )
    }

    pendingDelete?.let { event ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除这个日期？") },
            text = { Text("「${event.title}」会从应用和桌面插件里一起移除。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(event.id)
                    pendingDelete = null
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            }
        )
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text("放到桌面") },
            text = {
                Text("在桌面空白处长按，选择「微件 / 插件」，找到「日期计数插件」。放到桌面后会弹出日期列表；之后点插件也能重新选择日期。")
            },
            confirmButton = {
                TextButton(onClick = { showHelp = false }) { Text("知道了") }
            }
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))
            Text("还没有日期", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "先添加一个纪念日或倒计日，再把它放到桌面插件上。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EventCard(
    count: DateCount,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(count.event.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        count.formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Outlined.Edit, contentDescription = "编辑")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = "删除")
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(count.directionLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    when (count.event.displayMode) {
                        DisplayMode.WEEKS_AND_DAYS -> {
                            Text(count.weeksAndDaysLabel, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        }
                        DisplayMode.TOTAL_DAYS -> {
                            Text(count.totalDaysLabel, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        }
                        DisplayMode.BOTH -> {
                            Text(count.weeksAndDaysLabel, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text(count.totalDaysLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}


