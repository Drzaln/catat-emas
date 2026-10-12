package com.rizal.catatemas.security

import android.app.Activity
import android.content.Context
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.os.SystemClock
import android.util.Base64
import androidx.annotation.RequiresApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class SecurityState(
    val lockEnabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val biometricAvailable: Boolean = false,
    /** cegah screenshot / rekam layar (FLAG_SECURE) */
    val secureScreen: Boolean = true,
) {
    val biometricUsable: Boolean get() = lockEnabled && biometricEnabled && biometricAvailable
}

/**
 * Kunci aplikasi (PIN) + biometrik + proteksi screenshot.
 *
 * PIN tidak disimpan mentah: disimpan hash PBKDF2-HMAC-SHA256 + salt acak.
 * Semua data ada di SharedPreferences privat aplikasi, tidak dikirim ke mana pun.
 */
class SecurityManager(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(readState())
    val state: StateFlow<SecurityState> = _state.asStateFlow()

    /** true = tampilkan layar kunci */
    private val _locked = MutableStateFlow(readState().lockEnabled)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    private var backgroundAt = 0L

    // ---------------------------------------------------------------- PIN

    val pinLength: Int get() = prefs.getInt(KEY_PIN_LENGTH, 0)

    val isLockEnabled: Boolean get() = prefs.getString(KEY_PIN_HASH, null) != null

    fun setPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString(KEY_PIN_SALT, encode(salt))
            .putString(KEY_PIN_HASH, encode(hashPin(pin, salt)))
            .putInt(KEY_PIN_LENGTH, pin.length)
            .apply()
        refresh()
    }

    fun verifyPin(pin: String): Boolean {
        val salt = prefs.getString(KEY_PIN_SALT, null)?.let { decode(it) } ?: return false
        val expected = prefs.getString(KEY_PIN_HASH, null)?.let { decode(it) } ?: return false
        return hashPin(pin, salt).contentEquals(expected)
    }

    fun clearPin() {
        prefs.edit()
            .remove(KEY_PIN_HASH)
            .remove(KEY_PIN_SALT)
            .remove(KEY_PIN_LENGTH)
            .putBoolean(KEY_BIOMETRIC, false)
            .apply()
        refresh()
        _locked.value = false
    }

    // ---------------------------------------------------------------- pengaturan lain

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC, enabled).apply()
        refresh()
    }

    fun setSecureScreen(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SECURE_SCREEN, enabled).apply()
        refresh()
    }

    // ---------------------------------------------------------------- sesi

    fun lockNow() {
        if (_state.value.lockEnabled) _locked.value = true
    }

    fun unlock() {
        _locked.value = false
    }

    fun onAppStopped() {
        backgroundAt = SystemClock.elapsedRealtime()
    }

    /** kunci lagi kalau aplikasi lama di background (biar picker foto tidak memicu kunci) */
    fun onAppStarted() {
        val since = backgroundAt
        backgroundAt = 0L
        if (_state.value.lockEnabled && since > 0L &&
            SystemClock.elapsedRealtime() - since > GRACE_MS
        ) {
            _locked.value = true
        }
    }

    fun refresh() {
        _state.value = readState()
    }

    private fun readState(): SecurityState = SecurityState(
        lockEnabled = isLockEnabled,
        biometricEnabled = prefs.getBoolean(KEY_BIOMETRIC, false),
        biometricAvailable = hasBiometricEnrolled(context),
        secureScreen = prefs.getBoolean(KEY_SECURE_SCREEN, true),
    )

    private fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    companion object {
        private const val PREFS = "security"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_PIN_LENGTH = "pin_length"
        private const val KEY_BIOMETRIC = "biometric"
        private const val KEY_SECURE_SCREEN = "secure_screen"

        private const val ITERATIONS = 100_000
        private const val KEY_BITS = 256

        /** toleransi sebelum dikunci ulang otomatis */
        private const val GRACE_MS = 30_000L

        const val MIN_PIN = 4
        const val MAX_PIN = 6

        private fun encode(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)

        private fun decode(value: String): ByteArray = Base64.decode(value, Base64.NO_WRAP)
    }
}

/** cek biometrik tersedia & sudah terdaftar (API 28+) */
fun hasBiometricEnrolled(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
    return runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val manager = context.getSystemService(android.hardware.biometrics.BiometricManager::class.java)
            manager?.canAuthenticate(android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
                android.hardware.biometrics.BiometricManager.BIOMETRIC_SUCCESS
        } else {
            @Suppress("DEPRECATION")
            val fingerprint = context.getSystemService(android.hardware.fingerprint.FingerprintManager::class.java)
            @Suppress("DEPRECATION")
            fingerprint != null && fingerprint.isHardwareDetected && fingerprint.hasEnrolledFingerprints()
        }
    }.getOrDefault(false)
}

/** Prompt biometrik bawaan platform (tanpa dependency tambahan). */
@RequiresApi(Build.VERSION_CODES.P)
fun authenticateWithBiometric(
    activity: Activity,
    onSuccess: () -> Unit,
    onCancel: () -> Unit,
) {
    runCatching {
        val executor = activity.mainExecutor
        val prompt = BiometricPrompt.Builder(activity)
            .setTitle("Buka Catat Emas")
            .setSubtitle("Verifikasi sidik jari / wajah")
            .setNegativeButton("Pakai PIN", executor) { _, _ -> onCancel() }
            .build()

        prompt.authenticate(
            CancellationSignal(),
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                    onCancel()
                }
            },
        )
    }.onFailure { onCancel() }
}
