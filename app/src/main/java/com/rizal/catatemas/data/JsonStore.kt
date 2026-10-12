package com.rizal.catatemas.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Penyimpanan JSON sederhana + tulis atomic (tmp file lalu rename).
 * Dipakai supaya tidak perlu database/annotation processor -> ukuran apk & build lebih ringan.
 */
class JsonStore<T>(
    private val file: File,
    private val serializer: KSerializer<T>,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    @Volatile
    private var cached: T? = null

    @Synchronized
    fun read(): T? {
        cached?.let { return it }
        val value = runCatching {
            if (!file.exists() || file.length() == 0L) null
            else json.decodeFromString(serializer, file.readText())
        }.getOrNull()
        cached = value
        return value
    }

    @Synchronized
    fun write(value: T) {
        cached = value
        runCatching {
            file.parentFile?.mkdirs()
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(json.encodeToString(serializer, value))
            if (!tmp.renameTo(file)) {
                file.delete()
                if (!tmp.renameTo(file)) tmp.copyTo(file, overwrite = true)
            }
        }
    }
}
