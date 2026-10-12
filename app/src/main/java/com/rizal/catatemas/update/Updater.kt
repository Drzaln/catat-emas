package com.rizal.catatemas.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.rizal.catatemas.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

sealed interface UpdateState {
    data object Idle : UpdateState
    data class Available(val version: String) : UpdateState
    data class Downloading(val version: String, val progress: Int) : UpdateState
    data class Ready(val version: String, val file: File) : UpdateState
    data class Failed(val message: String) : UpdateState
}

/**
 * Cek pembaruan dari GitHub Releases **tanpa REST API** (biar tidak kena rate limit):
 * - `/releases/latest` di-redirect ke `/releases/tag/v<versi>` -> versi terbaru dibaca dari URL akhir.
 * - `/releases/latest/download/<asset>` selalu menunjuk aset di release terbaru.
 *
 * Repo diambil dari BuildConfig.GITHUB_REPO (set lewat gradle.properties -> `github.repo`).
 */
class Updater(private val context: Context) {

    private val prefs = context.getSharedPreferences("update", Context.MODE_PRIVATE)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    val currentVersion: String
        get() = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: BuildConfig.VERSION_NAME

    val repo: String get() = BuildConfig.GITHUB_REPO

    private val baseUrl: String get() = "https://github.com/$repo"

    suspend fun latestVersion(): String? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("$baseUrl/releases/latest")
                .header("User-Agent", "CatatEmas/${currentVersion}")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val finalUrl = response.request.url.toString()
                Regex("/releases/tag/v?(.+)$").find(finalUrl)?.groupValues?.get(1)?.trim()
            }
        }.getOrNull()
    }

    fun isNewer(remote: String, current: String): Boolean {
        fun parts(value: String) = value.trim().removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
        val remoteParts = parts(remote)
        val currentParts = parts(current)
        if (remoteParts.isEmpty()) return false
        for (index in 0 until maxOf(remoteParts.size, currentParts.size)) {
            val a = remoteParts.getOrElse(index) { 0 }
            val b = currentParts.getOrElse(index) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    suspend fun download(onProgress: (Int) -> Unit): File = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/releases/latest/download/$APK_NAME")
            .header("User-Agent", "CatatEmas/${currentVersion}")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Unduhan gagal: HTTP ${response.code}")
            val body = response.body ?: error("Unduhan gagal: respons kosong")
            val total = body.contentLength()
            val dir = File(context.cacheDir, "update").apply { mkdirs() }
            val target = File(dir, APK_NAME)
            body.byteStream().use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var read = 0L
                    var lastPercent = -1
                    while (true) {
                        val count = input.read(buffer)
                        if (count <= 0) break
                        output.write(buffer, 0, count)
                        read += count
                        if (total > 0) {
                            val percent = ((read * 100) / total).toInt()
                            if (percent != lastPercent) {
                                lastPercent = percent
                                onProgress(percent)
                            }
                        }
                    }
                }
            }
            target
        }
    }

    // ------------------------------------------------------------- versi yang di-skip

    fun skippedVersion(): String? = prefs.getString(KEY_SKIP, null)

    fun skipVersion(version: String) {
        prefs.edit().putString(KEY_SKIP, version).apply()
    }

    // ------------------------------------------------------------- pemasangan

    fun canRequestInstalls(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    fun openInstallSettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    fun install(file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    companion object {
        private const val KEY_SKIP = "skipped_version"
        private const val APK_NAME = "app-release.apk"
    }
}
