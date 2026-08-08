package com.brk.booklogger

import android.Manifest
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.brk.booklogger.data.notifications.ReadingNotificationHelper
import com.brk.booklogger.data.notifications.ReadingNotificationScheduler
import com.brk.booklogger.ui.navigation.BookLogNavHost
import com.brk.booklogger.ui.theme.BookLogTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        val app = application as BookLogApplication
        ReadingNotificationScheduler.schedule(this)

        setContent {
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { /* granted or denied â€” worker still runs, posts only if allowed */ }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !ReadingNotificationHelper.canPostNotifications(this@MainActivity)
                ) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            BookLogTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BookLogNavHost(app = app)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        (application as BookLogApplication).audioManager.pauseBackgroundMusic()
    }
}