package com.rizal.catatemas.backup

import android.content.Context
import android.net.Uri
import com.rizal.catatemas.data.ItemRepository
import com.rizal.catatemas.data.ItemStore
import com.rizal.catatemas.data.PhotoStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Backup terenkripsi (.ceb) berisi semua catatan + foto.
 *
 * Format file:
 *   "CEB1" | versi(1B) | panjang salt(1B) | salt | iterasi(4B) | panjang iv(1B) | iv | AES-256-GCM(zip)
 *
 * Isi zip: backup.json (ItemStore) + photos/<nama file>.
 * Kunci diturunkan dari passphrase user (PBKDF2-HMAC-SHA256 + salt acak),
 * GCM memberi autentikasi -> passphrase salah / file diubah akan gagal dibuka.
 */
class BackupManager(
    private val context: Context,
    private val items: ItemRepository,
    private val photos: PhotoStore,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun export(uri: Uri, passphrase: String): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val all = items.items.value
            val salt = randomBytes(SALT_LEN)
            val iv = randomBytes(IV_LEN)

            val resolver = context.contentResolver
                ?: throw IllegalStateException("Content resolver tidak tersedia")

            resolver.openOutputStream(uri, "wt")?.use { raw ->
                DataOutputStream(BufferedOutputStream(raw)).use { header ->
                    header.write(MAGIC)
                    header.writeByte(FORMAT_VERSION)
                    header.writeByte(SALT_LEN)
                    header.write(salt)
                    header.writeInt(ITERATIONS)
                    header.writeByte(IV_LEN)
                    header.write(iv)
                    header.flush()

                    val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                        init(
                            Cipher.ENCRYPT_MODE,
                            SecretKeySpec(deriveKey(passphrase, salt, ITERATIONS), "AES"),
                            GCMParameterSpec(TAG_BITS, iv),
                        )
                    }

                    CipherOutputStream(header, cipher).use { encrypted ->
                        ZipOutputStream(BufferedOutputStream(encrypted)).use { zip ->
                            // photoPath di dalam backup diubah jadi nama entry zip
                            val payload = all.map { item ->
                                val name = item.photoPath
                                    ?.let { path -> File(path).takeIf { file -> file.exists() }?.name }
                                item.copy(photoPath = name?.let { "photos/$it" })
                            }

                            zip.putNextEntry(ZipEntry(ENTRY_JSON))
                            zip.write(json.encodeToString(ItemStore.serializer(), ItemStore(payload)).toByteArray())
                            zip.closeEntry()

                            all.forEach { item ->
                                val file = item.photoPath?.let { File(it) } ?: return@forEach
                                if (!file.exists()) return@forEach
                                zip.putNextEntry(ZipEntry("photos/${file.name}"))
                                file.inputStream().use { input -> input.copyTo(zip, BUFFER) }
                                zip.closeEntry()
                            }
                        }
                    }
                }
            } ?: throw IOException("Tidak bisa menulis file backup")

            all.size
        }.mapError()
    }

    suspend fun import(uri: Uri, passphrase: String): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
                ?: throw IllegalStateException("Content resolver tidak tersedia")

            val restored = resolver.openInputStream(uri)?.use { raw ->
                DataInputStream(BufferedInputStream(raw)).use { input ->
                    val magic = ByteArray(MAGIC.size).also { input.readFully(it) }
                    if (!magic.contentEquals(MAGIC)) {
                        throw IllegalArgumentException("File ini bukan backup Catat Emas")
                    }
                    val version = input.readByte().toInt()
                    if (version != FORMAT_VERSION) {
                        throw IllegalArgumentException("Versi backup tidak didukung ($version)")
                    }
                    val salt = ByteArray(input.readByte().toInt()).also { input.readFully(it) }
                    val iterations = input.readInt()
                    val iv = ByteArray(input.readByte().toInt()).also { input.readFully(it) }

                    val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                        init(
                            Cipher.DECRYPT_MODE,
                            SecretKeySpec(deriveKey(passphrase, salt, iterations), "AES"),
                            GCMParameterSpec(TAG_BITS, iv),
                        )
                    }

                    CipherInputStream(input, cipher).use { decrypted ->
                        ZipInputStream(BufferedInputStream(decrypted)).use { zip ->
                            var payload: ItemStore? = null
                            val extracted = mutableMapOf<String, File>()
                            var entry = zip.nextEntry
                            var index = 0

                            while (entry != null) {
                                val name = entry.name
                                when {
                                    name == ENTRY_JSON -> {
                                        val text = zip.readBytes().decodeToString()
                                        payload = json.decodeFromString(ItemStore.serializer(), text)
                                    }

                                    name.startsWith("photos/") && !entry.isDirectory -> {
                                        // nama unik supaya tidak bentrok dengan file lama
                                        val target = File(
                                            photos.directory,
                                            "restore_${System.currentTimeMillis()}_${index}_${File(name).name}",
                                        )
                                        target.outputStream().use { output -> zip.copyTo(output, BUFFER) }
                                        extracted[name] = target
                                        index++
                                    }
                                }
                                zip.closeEntry()
                                entry = zip.nextEntry
                            }

                            val store = payload ?: throw IllegalArgumentException("Isi backup tidak lengkap")
                            ItemStore(
                                store.items.map { item ->
                                    val file = item.photoPath?.let { extracted[it] }
                                    item.copy(photoPath = file?.absolutePath)
                                },
                            )
                        }
                    }
                }
            } ?: throw IOException("Tidak bisa membaca file backup")

            // ganti seluruh data: hapus foto lama, lalu tulis ulang
            items.items.value.mapNotNull { it.photoPath }.forEach { photos.delete(it) }
            items.replaceAll(restored.items)

            restored.items.size
        }.mapError()
    }

    private fun randomBytes(size: Int): ByteArray = ByteArray(size).also { SecureRandom().nextBytes(it) }

    private fun deriveKey(passphrase: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, iterations, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    /** pesan error yang enak dibaca: GCM gagal = passphrase salah / file diubah */
    private fun <T> Result<T>.mapError(): Result<T> = recoverCatching { error ->
        var cause: Throwable? = error
        while (cause != null) {
            if (cause is AEADBadTagException || cause is BadPaddingException) {
                throw IllegalArgumentException("Passphrase salah atau file backup rusak / diubah")
            }
            cause = cause.cause
        }
        throw error
    }

    companion object {
        val MAGIC = byteArrayOf('C'.code.toByte(), 'E'.code.toByte(), 'B'.code.toByte(), '1'.code.toByte())
        const val FORMAT_VERSION: Int = 1
        const val EXTENSION: String = "ceb"

        private const val ENTRY_JSON = "backup.json"
        private const val SALT_LEN = 16
        private const val IV_LEN = 12
        private const val TAG_BITS = 128
        private const val KEY_BITS = 256
        private const val ITERATIONS = 150_000
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val BUFFER = 64 * 1024
    }
}
