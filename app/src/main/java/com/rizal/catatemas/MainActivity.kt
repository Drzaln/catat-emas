package com.rizal.catatemas

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.rizal.catatemas.ui.CatatEmasRoot
import com.rizal.catatemas.ui.theme.CatatEmasTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val security by lazy { (application as CatatEmasApp).container.securityManager }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // proteksi screenshot / rekam layar mengikuti pengaturan
        applySecureScreen(security.state.value.secureScreen)
        lifecycleScope.launch {
            security.state.collect { state -> applySecureScreen(state.secureScreen) }
        }

        val container = (application as CatatEmasApp).container
        setContent {
            CatatEmasTheme {
                CatatEmasRoot(container)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        security.onAppStarted()
    }

    override fun onStop() {
        security.onAppStopped()
        super.onStop()
    }

    private fun applySecureScreen(enabled: Boolean) {
        if (enabled) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE,
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
