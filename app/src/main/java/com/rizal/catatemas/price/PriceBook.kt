package com.rizal.catatemas.price

import com.rizal.catatemas.data.ItemKind
import kotlin.math.abs
import kotlin.math.roundToLong

enum class ValueBasis {
    /** harga buyback resmi dari merk */
    BUYBACK_MERK,

    /** estimasi nilai lebur (harga acuan 24K x kadar) */
    LEBUR,

    /** belum ada data harga */
    TIDAK_ADA,
}

data class Valuation(
    val value: Long?,
    val perGram: Long?,
    val brandSellTotal: Long?,
    val brandSellPerGram: Long?,
    val basis: ValueBasis,
    val sourceLabel: String?,
) {
    companion object {
        val Empty = Valuation(null, null, null, null, ValueBasis.TIDAK_ADA, null)
    }
}

object SpotResolver {

    fun resolve(sources: Map<String, SourcePrices>): Long? {
        sources[PriceSources.SPOT]
            ?.tiers
            ?.get(PriceSources.SPOT_TYPE)
            ?.firstOrNull { abs(it.weight - 1.0) < 0.005 }
            ?.sell
            ?.takeIf { it > 0L }
            ?.let { return it }

        PriceSources.SPOT_FALLBACKS.forEach { (source, type) ->
            val sell = sources[source]
                ?.tiers
                ?.get(type)
                ?.firstOrNull { abs(it.weight - 1.0) < 0.005 }
                ?.sell
                ?.takeIf { it > 0L }
            if (sell != null) return (sell * PriceSources.SPOT_RATIO).roundToLong()
        }
        return null
    }
}

/**
 * Buku harga: mengubah data mentah per sumber menjadi harga 1 item.
 *
 * - Batangan (logam mulia): pakai harga buyback resmi merk untuk berat tsb.
 *   Kalau berat tidak ada di daftar (mis. 1,5 gr / 4 gr) harga per gram diinterpolasi
 *   dari tier terdekat. Kalau merk tidak menyediakan buyback -> estimasi lebur.
 * - Perhiasan: estimasi lebur = harga acuan 24K x (karat / 24).
 */
class PriceBook(private val store: PriceStore) {

    val updatedAt: Long get() = store.updatedAt
    val isEmpty: Boolean get() = store.sources.isEmpty()

    val spot24PerGram: Long? get() = store.spotPerGram ?: SpotResolver.resolve(store.sources)

    fun recordedDateFor(brand: GoldBrand): String? = brand.sources.firstNotNullOfOrNull { source ->
        store.sources[source.source]?.recordedDate?.takeIf { it.isNotBlank() }
    }

    fun sourceNamesFor(brand: GoldBrand): List<String> =
        brand.sources.filter { store.sources.containsKey(it.source) }.map { it.label }

    fun valuation(brand: GoldBrand, kind: ItemKind, weightGram: Double, karat: Int): Valuation {
        if (weightGram <= 0.0) return Valuation.Empty

        var buybackPerGram: Long? = null
        var sellPerGram: Long? = null
        var buybackSource: String? = null

        if (kind == ItemKind.BATANGAN) {
            for (source in brand.sources) {
                val tiers = tiersOf(source) ?: continue
                if (buybackPerGram == null) {
                    buybackPerGram = perGramAt(tiers, weightGram) { it.buyback }
                    if (buybackPerGram != null) buybackSource = source.label
                }
                if (sellPerGram == null) {
                    sellPerGram = perGramAt(tiers, weightGram) { it.sell }
                }
                if (buybackPerGram != null && sellPerGram != null) break
            }
        }

        val karatRatio = karat.coerceIn(1, 24) / 24.0
        val meltPerGram = spot24PerGram?.let { (it * karatRatio).roundToLong() }

        val perGram = buybackPerGram ?: meltPerGram
        val value = perGram?.let { (it * weightGram).roundToLong() }

        val basis = when {
            buybackPerGram != null -> ValueBasis.BUYBACK_MERK
            meltPerGram != null -> ValueBasis.LEBUR
            else -> ValueBasis.TIDAK_ADA
        }

        return Valuation(
            value = value,
            perGram = perGram,
            brandSellTotal = sellPerGram?.let { (it * weightGram).roundToLong() },
            brandSellPerGram = sellPerGram,
            basis = basis,
            sourceLabel = when (basis) {
                ValueBasis.BUYBACK_MERK -> buybackSource
                ValueBasis.LEBUR -> "Acuan 24K"
                ValueBasis.TIDAK_ADA -> null
            },
        )
    }

    /** harga buyback per gram untuk berat tertentu (dipakai di form input) */
    fun buybackPerGram(brand: GoldBrand, weightGram: Double): Long? {
        val valuation = valuation(brand, ItemKind.BATANGAN, weightGram, 24)
        return if (valuation.basis == ValueBasis.BUYBACK_MERK) valuation.perGram else null
    }

    private fun tiersOf(source: BrandSource): List<TierPrice>? = store.sources[source.source]
        ?.tiers
        ?.get(source.materialType)
        ?.takeIf { it.isNotEmpty() }

    private fun perGramAt(tiers: List<TierPrice>, weight: Double, pick: (TierPrice) -> Long?): Long? {
        val exact = tiers.firstOrNull { abs(it.weight - weight) < 0.005 }
            ?.let(pick)
            ?.takeIf { it > 0L }
        if (exact != null) return (exact.toDouble() / weight).roundToLong()

        val points = tiers
            .mapNotNull { tier ->
                pick(tier)?.takeIf { it > 0L }?.let { tier.weight to it.toDouble() / tier.weight }
            }
            .sortedBy { it.first }

        if (points.isEmpty()) return null

        val lower = points.lastOrNull { it.first <= weight }
        val upper = points.firstOrNull { it.first >= weight }

        val perGram = when {
            lower == null && upper == null -> return null
            lower == null -> upper!!.second
            upper == null -> lower.second
            abs(lower.first - upper.first) < 1e-9 -> lower.second
            else -> {
                val ratio = (weight - lower.first) / (upper.first - lower.first)
                lower.second + (upper.second - lower.second) * ratio
            }
        }
        return if (perGram > 0.0) perGram.roundToLong() else null
    }
}
