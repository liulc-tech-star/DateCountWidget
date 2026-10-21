package com.datecount.widget.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.datecount.widget.data.DateEvent
import com.datecount.widget.data.DisplayMode
import com.datecount.widget.data.MAX_EVENT_TITLE_LENGTH
import com.datecount.widget.data.calculateCount
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

data class EventEditorState(
    val id: String? = null,
    val title: String = "",
    val date: LocalDate = LocalDate.now(),
    val displayMode: DisplayMode = DisplayMode.BOTH
) {
    companion object {
        fun from(event: DateEvent) = EventEditorState(
            id = event.id,
            title = event.title,
            date = event.date,
            displayMode = event.displayMode
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditorDialog(
    state: EventEditorState,
    onDismiss: () -> Unit,
    onSave: (String, LocalDate, DisplayMode) -> Unit
) {
    var title by remember { mutableStateOf(state.title.take(MAX_EVENT_TITLE_LENGTH)) }
    var date by remember { mutableStateOf(state.date) }
    var mode by remember { mutableStateOf(state.displayMode) }
    var showPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (state.id == null) "添加日期" else "编辑日期") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(MAX_EVENT_TITLE_LENGTH) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("名称") },
                    placeholder = { Text("例如：毕业、生日、考试") },
                    singleLine = true
                )
                FilledTonalButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.DateRange, contentDescription = null)
                    Spacer(Modifier.padding(horizontal = 6.dp))
                    Text(date.toString())
                }
                Text("桌面显示方式", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = mode == DisplayMode.WEEKS_AND_DAYS,
                        onClick = { mode = DisplayMode.WEEKS_AND_DAYS },
                        label = { Text("周+天") }
                    )
                    FilterChip(
                        selected = mode == DisplayMode.TOTAL_DAYS,
                        onClick = { mode = DisplayMode.TOTAL_DAYS },
                        label = { Text("总天数") }
                    )
                    FilterChip(
                        selected = mode == DisplayMode.BOTH,
                        onClick = { mode = DisplayMode.BOTH },
                        label = { Text("都显示") }
                    )
                }
                PreviewLine(title = title, date = date, mode = mode)
            }
        },
        confirmButton = {
            Button(onClick = { onSave(title, date, mode) }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )

    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showPicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun PreviewLine(title: String, date: LocalDate, mode: DisplayMode) {
    val preview = calculateCount(
        DateEvent(
            id = "preview",
            title = title.ifBlank { "未命名日期" },
            dateIso = date.toString(),
            displayMode = mode
        )
    )
    Text(
        buildString {
            append(preview.directionLabel)
            append(" ")
            append(preview.primaryLabel)
            preview.secondaryLabel?.let { append(" · ").append(it) }
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
