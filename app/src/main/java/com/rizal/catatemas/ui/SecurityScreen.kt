@file:OptIn(ExperimentalMaterial3Api::class)

package com.rizal.catatemas.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rizal.catatemas.security.SecurityManager
import com.rizal.catatemas.security.SecurityState
import com.rizal.catatemas.ui.components.PinDots
import com.rizal.catatemas.ui.components.PinKeypad
import kotlinx.coroutines.delay

private enum class PinDialogMode { ENABLE, CHANGE, DISABLE }

private enum class PinStep { VERIFY, ENTER, CONFIRM }

@Composable
fun SecurityScreen(
    state: SecurityState,
    pinLength: Int,
    onClose: () -> Unit,
    onVerifyPin: (String) -> Boolean,
    onSetPin: (String) -> Unit,
    onClearPin: () -> Unit,
    onToggleBiometric: (Boolean) -> Unit,
    onToggleSecureScreen: (Boolean) -> Unit,
    onLockNow: () -> Unit,
) {
    BackHandler { onClose() }

    var dialogMode by remember { mutableStateOf<PinDialogMode?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Keamanan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SettingSwitch(
                        title = "Kunci aplikasi (PIN)",
                        description = if (state.lockEnabled) {
                            "Aktif • PIN $pinLength angka. Aplikasi minta PIN setiap dibuka."
                        } else {
                            "Wajibkan PIN setiap aplikasi dibuka."
                        },
                        checked = state.lockEnabled,
                        onCheckedChange = { checked ->
                            dialogMode = if (checked) PinDialogMode.ENABLE else PinDialogMode.DISABLE
                        },
                    )

                    HorizontalDivider(Modifier.padding(vertical = 8.dp))

                    SettingSwitch(
                        title = "Buka dengan biometrik",
                        description = when {
                            !state.lockEnabled -> "Aktifkan kunci PIN dulu."
                            !state.biometricAvailable ->
                                "Belum ada sidik jari / wajah yang terdaftar di pengaturan perangkat."
                            else -> "Bisa dibuka pakai sidik jari / wajah, PIN tetap jadi cadangan."
                        },
                        checked = state.lockEnabled && state.biometricEnabled,
                        enabled = state.lockEnabled && state.biometricAvailable,
                        onCheckedChange = onToggleBiometric,
                    )

                    if (state.lockEnabled) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = { dialogMode = PinDialogMode.CHANGE },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Ubah PIN", maxLines = 1)
                            }
                            FilledTonalButton(
                                onClick = onLockNow,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Kunci sekarang", maxLines = 1)
                            }
                        }
                    }
                }
            }

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    SettingSwitch(
                        title = "Cegah screenshot & rekam layar",
                        description = "Layar aplikasi jadi hitam kalau di-screenshot atau direkam, " +
                            "dan tidak muncul di pratinjau aplikasi terbaru.",
                        checked = state.secureScreen,
                        onCheckedChange = onToggleSecureScreen,
                    )
                }
            }

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Catatan keamanan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Note("PIN disimpan sebagai hash PBKDF2-HMAC-SHA256 + salt acak di penyimpanan privat aplikasi, bukan teks biasa.")
                    Note("PIN tidak pernah dikirim ke server. Tetap jaga PIN dan kunci layar perangkat.")
                    Note("PIN lupa = satu-satunya jalan hapus data aplikasi dari pengaturan Android. Catatan emas ikut hilang.")
                    Note("Aplikasi otomatis terkunci lagi setelah 30 detik di background.")
                }
            }
        }
    }

    val mode = dialogMode
    if (mode != null) {
        PinDialog(
            mode = mode,
            pinLength = pinLength,
            onVerify = onVerifyPin,
            onDone = { newPin ->
                when (mode) {
                    PinDialogMode.DISABLE -> onClearPin()
                    else -> newPin?.let { onSetPin(it) }
                }
                dialogMode = null
            },
            onDismiss = { dialogMode = null },
        )
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun Note(text: String) {
    Text(
        text = "•  $text",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun PinDialog(
    mode: PinDialogMode,
    pinLength: Int,
    onVerify: (String) -> Boolean,
    onDone: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var step by remember {
        mutableStateOf(
            if (mode == PinDialogMode.ENABLE) PinStep.ENTER else PinStep.VERIFY,
        )
    }
    var input by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    fun fail(text: String) {
        error = true
        message = text
        input = ""
    }

    fun handleFilled(value: String) {
        when (step) {
            PinStep.VERIFY -> {
                if (onVerify(value)) {
                    input = ""
                    error = false
                    message = ""
                    when (mode) {
                        PinDialogMode.DISABLE -> onDone(null)
                        else -> step = PinStep.ENTER
                    }
                } else {
                    fail("PIN salah")
                }
            }

            PinStep.ENTER -> Unit

            PinStep.CONFIRM -> {
                if (value == newPin) {
                    onDone(newPin)
                } else {
                    fail("PIN tidak sama, ulangi")
                }
            }
        }
    }

    LaunchedEffect(error) {
        if (error) {
            delay(1_500L)
            error = false
        }
    }

    val expectedLength = when (step) {
        PinStep.VERIFY -> pinLength
        PinStep.ENTER -> SecurityManager.MAX_PIN
        PinStep.CONFIRM -> newPin.length
    }
    val shownLength = when (step) {
        PinStep.ENTER -> SecurityManager.MAX_PIN
        else -> expectedLength
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (step) {
                    PinStep.VERIFY -> "Masukkan PIN saat ini"
                    PinStep.ENTER -> "Buat PIN baru"
                    PinStep.CONFIRM -> "Ulangi PIN baru"
                },
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = when (step) {
                        PinStep.VERIFY -> "Masukkan $pinLength angka PIN kamu."
                        PinStep.ENTER -> "PIN ${SecurityManager.MIN_PIN}-${SecurityManager.MAX_PIN} angka."
                        PinStep.CONFIRM -> "Ulangi PIN barunya."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                PinDots(
                    count = shownLength,
                    filled = input.length,
                    error = error,
                )

                Text(
                    text = if (message.isNotBlank()) message else " ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )

                PinKeypad(
                    onDigit = { digit ->
                        if (input.length < expectedLength) {
                            val next = input + digit
                            input = next
                            if (next.length == expectedLength && step != PinStep.ENTER) handleFilled(next)
                        }
                    },
                    onBackspace = { input = input.dropLast(1) },
                    modifier = Modifier.fillMaxWidth(),
                )

                if (step == PinStep.ENTER) {
                    TextButton(
                        onClick = {
                            newPin = input
                            input = ""
                            message = ""
                            error = false
                            step = PinStep.CONFIRM
                        },
                        enabled = input.length >= SecurityManager.MIN_PIN,
                    ) {
                        Text("Lanjut")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        },
    )
}
