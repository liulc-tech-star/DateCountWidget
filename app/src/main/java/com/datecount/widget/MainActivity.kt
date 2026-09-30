package com.datecount.widget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.datecount.widget.ui.DateCountApp
import com.datecount.widget.ui.theme.DateCountTheme
import com.datecount.widget.widget.DateCountWidgetReceiver
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            DateCountWidgetReceiver.refreshAllNow(applicationContext, force = true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DateCountTheme {
                DateCountApp()
            }
        }
    }
}
