package com.rizal.catatemas.ui

import com.rizal.catatemas.data.GoldItem
import com.rizal.catatemas.data.ItemKind
import com.rizal.catatemas.price.GoldBrand
import com.rizal.catatemas.price.PriceBook
import kotlin.math.roundToLong

data class ZakatUiState(
    val hasItems: Boolean = false,
    val includeJewelry: Boolean = true,
    /** semua emas dikonversi ke kadar 24K */
    val totalGram24: Double = 0.0,
    val bullionGram24: Double = 0.0,
    val jewelryGram24: Double = 0.0,
    /** berat yang masuk hitungan zakat */
    val baseGram24: Double = 0.0,
    val baseValue: Long = 0L,
    val valueComplete: Boolean = true,
    val nisabGram: Double = ZakatCalculator.NISAB_GRAM,
    val nisabValue: Long? = null,
    val reachesNisab: Boolean = false,
    val haulMet: Boolean = false,
    val haulCompleteAt: Long? = null,
    val zakatRupiah: Long = 0L,
    val zakatGram: Double = 0.0,
    val progress: Float = 0f,
) {
    val wajib: Boolean get() = reachesNisab && haulMet

    val shortfallGram: Double get() = (nisabGram - baseGram24).coerceAtLeast(0.0)
}

/**
 * Zakat emas:
 * - nisab 85 gram emas murni (24K)
 * - haul 1 tahun hijriah (354 hari) sejak kepemilikan mencapai nisab
 * - kadar 2,5% (1/40), boleh dibayar dengan emas ataupun uang senilai
 *
 * Berat semua item dikonversi ke kadar 24K dulu: gram x (karat / 24).
 */
object ZakatCalculator {

    const val NISAB_GRAM = 85.0
    const val KADAR = 0.025
    const val HAUL_DAYS = 354L
    private const val DAY_MS = 86_400_000L

    fun compute(
        items: List<GoldItem>,
        book: PriceBook,
        includeJewelry: Boolean,
        now: Long = System.currentTimeMillis(),
    ): ZakatUiState {
        if (items.isEmpty()) return ZakatUiState(includeJewelry = includeJewelry)

        var totalGram24 = 0.0
        var bullionGram24 = 0.0
        var jewelryGram24 = 0.0
        var baseGram24 = 0.0
        var baseValue = 0L
        var valueComplete = true
        var oldestInBase: Long? = null

        items.forEach { item ->
            val gram24 = item.weightGram * (item.karat.coerceIn(1, 24) / 24.0)
            totalGram24 += gram24

            when (item.kind) {
                ItemKind.BATANGAN -> bullionGram24 += gram24
                ItemKind.PERHIASAN -> jewelryGram24 += gram24
            }

            val masukHitungan = item.kind == ItemKind.BATANGAN || includeJewelry
            if (!masukHitungan) return@forEach

            baseGram24 += gram24
            val valuation = book.valuation(
                brand = GoldBrand.of(item.brandKey),
                kind = item.kind,
                weightGram = item.weightGram,
                karat = item.karat,
            )
            if (valuation.value != null) baseValue += valuation.value else valueComplete = false
            val oldest = oldestInBase
            if (oldest == null || item.purchaseDate < oldest) oldestInBase = item.purchaseDate
        }

        val oldest = oldestInBase
        val haulCompleteAt = oldest?.plus(HAUL_DAYS * DAY_MS)
        val haulMet = haulCompleteAt != null && now >= haulCompleteAt
        val reachesNisab = baseGram24 >= NISAB_GRAM

        return ZakatUiState(
            hasItems = true,
            includeJewelry = includeJewelry,
            totalGram24 = totalGram24,
            bullionGram24 = bullionGram24,
            jewelryGram24 = jewelryGram24,
            baseGram24 = baseGram24,
            baseValue = baseValue,
            valueComplete = valueComplete,
            nisabValue = book.spot24PerGram?.let { (it * NISAB_GRAM).roundToLong() },
            reachesNisab = reachesNisab,
            haulMet = haulMet,
            haulCompleteAt = haulCompleteAt,
            zakatRupiah = (baseValue * KADAR).roundToLong(),
            zakatGram = baseGram24 * KADAR,
            progress = (baseGram24 / NISAB_GRAM).coerceIn(0.0, 1.0).toFloat(),
        )
    }
}
