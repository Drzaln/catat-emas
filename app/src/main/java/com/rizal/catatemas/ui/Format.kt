package com.rizal.catatemas.ui

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale

object Fmt {

    val locale: Locale = Locale.forLanguageTag("id-ID")

    private val numberFormat: NumberFormat = NumberFormat.getIntegerInstance(locale)

    fun rupiah(value: Long): String = "Rp" + numberFormat.format(value)

    fun rupiahSigned(value: Long): String {
        val sign = when {
            value > 0L -> "+"
            value < 0L -> "-"
            else -> ""
        }
        return sign + "Rp" + numberFormat.format(if (value < 0L) -value else value)
    }

    /** angka rupiah tanpa prefix, untuk header besar */
    fun number(value: Long): String = numberFormat.format(value)

    fun gram(value: Double): String = gramNumber(value) + " gr"

    fun gramNumber(value: Double): String = BigDecimal.valueOf(value)
        .setScale(3, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()
        .replace('.', ',')

    fun percent(value: Double): String = String.format(locale, "%.1f", value) + "%"

    /** bentuk pendek untuk label sempit, mis. Rp2,41 jt */
    fun rupiahCompact(value: Long): String {
        val absolute = if (value < 0L) -value else value
        val sign = if (value < 0L) "-" else ""
        return when {
            absolute >= 1_000_000_000_000L -> sign + "Rp" + compactValue(value, 1_000_000_000_000.0, " T", 2)
            absolute >= 1_000_000_000L -> sign + "Rp" + compactValue(value, 1_000_000_000.0, " M", 2)
            absolute >= 1_000_000L -> sign + "Rp" + compactValue(value, 1_000_000.0, " jt", 2)
            absolute >= 1_000L -> sign + "Rp" + compactValue(value, 1_000.0, " rb", 0)
            else -> sign + "Rp" + numberFormat.format(absolute)
        }
    }

    private fun compactValue(value: Long, divisor: Double, suffix: String, scale: Int): String {
        val scaled = BigDecimal.valueOf(value / divisor)
            .setScale(scale, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
            .replace('.', ',')
        return scaled + suffix
    }

    fun date(millis: Long): String = SimpleDateFormat("d MMM yyyy", locale).format(Date(millis))

    fun dateFull(millis: Long): String = SimpleDateFormat("EEEE, d MMMM yyyy", locale).format(Date(millis))

    /** teks tanggal untuk input manual: dd/MM/yyyy */
    fun dateInput(millis: Long): String = SimpleDateFormat("dd/MM/yyyy", locale).format(Date(millis))

    /** sisipkan pemisah otomatis saat user mengetik tanggal (hanya digit yang dipakai) */
    fun formatDateInput(input: String): String {
        val digits = input.filter { it.isDigit() }.take(8)
        return buildString {
            digits.forEachIndexed { index, char ->
                if (index == 2 || index == 4) append('/')
                append(char)
            }
        }
    }

    /** parse "dd/MM/yyyy" (pemisah apa pun diabaikan) -> epoch millis awal hari lokal */
    fun parseDateInput(text: String): Long? {
        val digits = text.filter { it.isDigit() }
        if (digits.length != 8) return null
        val day = digits.substring(0, 2).toIntOrNull() ?: return null
        val month = digits.substring(2, 4).toIntOrNull() ?: return null
        val year = digits.substring(4, 8).toIntOrNull() ?: return null
        if (year < 1900 || year > 2200 || month !in 1..12 || day !in 1..31) return null
        return runCatching {
            LocalDate.of(year, month, day)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }.getOrNull()
    }

    fun todayStart(): Long = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun toPickerMillis(localStartMillis: Long): Long = Instant.ofEpochMilli(localStartMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .atStartOfDay(ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli()

    fun fromPickerMillis(utcMillis: Long): Long = Instant.ofEpochMilli(utcMillis)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    /** lama kepemilikan, mis. "9 bulan" / "1 tahun 2 bulan" */
    fun duration(fromMillis: Long, toMillis: Long): String {
        val days = ((toMillis - fromMillis) / 86_400_000L).coerceAtLeast(0L)
        if (days < 30L) return "$days hari"
        val months = days / 30L
        if (months < 12L) return "$months bulan"
        val years = months / 12L
        val rest = months % 12L
        return if (rest == 0L) "$years tahun" else "$years tahun $rest bulan"
    }

    fun relative(millis: Long): String {        if (millis <= 0L) return "belum pernah"
        val diff = System.currentTimeMillis() - millis
        return when {
            diff < 60_000L -> "baru saja"
            diff < 3_600_000L -> "${diff / 60_000L} menit lalu"
            diff < 86_400_000L -> "${diff / 3_600_000L} jam lalu"
            diff < 7 * 86_400_000L -> "${diff / 86_400_000L} hari lalu"
            else -> date(millis)
        }
    }
}
