package com.datecount.widget

import android.app.Application
import com.datecount.widget.data.EventRepository
import com.datecount.widget.data.WidgetBindingStore
import com.datecount.widget.widget.DateCountWidgetReceiver
import com.datecount.widget.widget.WidgetRefreshWorker

class DateCountApplication : Application() {
    lateinit var events: EventRepository
        private set
    lateinit var widgetBindings: WidgetBindingStore
        private set

    override fun onCreate() {
        super.onCreate()
        events = EventRepository(this)
        widgetBindings = WidgetBindingStore(this)
        WidgetRefreshWorker.cancelLegacyPeriodicWork(this)
        if (DateCountWidgetReceiver.widgetIds(this).isNotEmpty()) WidgetRefreshWorker.schedule(this)
    }
}

fun android.content.Context.asDateCountApp(): DateCountApplication =
    applicationContext as DateCountApplication
