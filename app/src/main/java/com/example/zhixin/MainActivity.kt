package com.example.zhixin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.zhixin.data.Prefs
import com.example.zhixin.data.Store
import com.example.zhixin.notify.ReminderReceiver
import com.example.zhixin.ui.AppRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Store.load(applicationContext)
        Prefs.load(applicationContext)

        val settings = Prefs.settings.value
        if (settings.remindEnabled) {
            ReminderReceiver.schedule(applicationContext, settings.remindHour, settings.remindMinute)
        }

        setContent {
            AppRoot()
        }
    }
}
