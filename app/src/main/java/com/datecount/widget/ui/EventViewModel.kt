package com.datecount.widget.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.datecount.widget.asDateCountApp
import com.datecount.widget.data.DisplayMode
import com.datecount.widget.data.calculateCount
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class EventViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application.asDateCountApp()

    val events = app.events.events
        .map { list -> list.map { calculateCount(it) }.sortedBy { it.event.date } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(
        id: String?,
        title: String,
        date: LocalDate,
        displayMode: DisplayMode
    ) {
        viewModelScope.launch {
            app.events.save(id, title, date, displayMode)
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            app.events.delete(id)
            app.widgetBindings.removeEvent(id)
        }
    }
}
