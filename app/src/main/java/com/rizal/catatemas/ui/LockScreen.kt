package com.rizal.catatemas.ui

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rizal.catatemas.security.authenticateWithBiometric
import com.rizal.catatemas.ui.components.PinDots
import com.rizal.catatemas.ui.components.PinKeypad
import kotlinx.coroutines.delay

/**
 * Layar kunci. Dibuat sebagai overlay (bukan mengganti konten) supaya
 * isian form yang sedang dibuka tidak hilang saat aplikasi terkunci.
 */
@Composable
fun LockScreen(
    pinLength: Int,
    biometricUsable: Boolean,
    verifyPin: (String) -> Boolean,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var autoTried by remember { mutableStateOf(false) }

    fun submit(pin: String) {
        if (verifyPin(pin)) {
            input = ""
            error = false
            onUnlock()
        } else {
            error = true
            input = ""
        }
    }

    fun promptBiometric() {
        val activity = context as? Activity ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        authenticateWithBiometric(
            activity = activity,
            onSuccess = { onUnlock() },
            onCancel = { /* tetap pakai PIN */ },
        )
    }

    LaunchedEffect(error) {
        if (error) {
            delay(1_400L)
            error = false
        }
    }

    LaunchedEffect(Unit) {
        if (biometricUsable && !autoTried) {
            autoTried = true
            delay(250L)
            promptBiometric()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
        ) {
            Text("🔒", style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Catat Emas terkunci",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Masukkan PIN untuk membuka",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(28.dp))

            PinDots(count = pinLength, filled = input.length, error = error)

            Spacer(Modifier.height(10.dp))

            Text(
                text = if (error) "PIN salah, coba lagi" else " ",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.error,
            )

            Spacer(Modifier.height(18.dp))

            PinKeypad(
                onDigit = { digit ->
                    if (input.length < pinLength) {
                        val next = input + digit
                        input = next
                        if (next.length == pinLength) submit(next)
                    }
                },
                onBackspace = { input = input.dropLast(1) },
                modifier = Modifier.widthIn(max = 340.dp),
            )

            if (biometricUsable) {
                Spacer(Modifier.height(14.dp))
                TextButton(onClick = { promptBiometric() }) {
                    Text("Buka dengan sidik jari / wajah")
                }
            }
        }
    }
}
