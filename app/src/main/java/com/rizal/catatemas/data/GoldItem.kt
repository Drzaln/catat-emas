package com.rizal.catatemas.data

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class ItemKind {
    BATANGAN,
    PERHIASAN,
}

@Serializable
data class GoldItem(
    val id: String = UUID.randomUUID().toString(),
    val brandKey: String,
    val kind: ItemKind = ItemKind.BATANGAN,
    val weightGram: Double,
    val karat: Int = 24,
    /** epoch millis, awal hari lokal */
    val purchaseDate: Long,
    /** total harga beli (rupiah), bukan per gram */
    val purchasePrice: Long,
    val note: String = "",
    val photoPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    /** terisi kalau sudah dijual */
    val saleDate: Long? = null,
    /** total uang yang diterima saat menjual */
    val salePrice: Long? = null,
    val salePlace: String = "",
) {
    val karatLabel: String get() = if (karat >= 24) "24K" else "${karat}K"

    val isSold: Boolean get() = saleDate != null && (salePrice ?: 0L) > 0L

    /** untung/rugi realisasi (kalau sudah dijual) */
    val realizedProfit: Long? get() = salePrice?.let { it - purchasePrice }

    val realizedPercent: Double?
        get() = realizedProfit?.let { if (purchasePrice > 0L) it * 100.0 / purchasePrice else null }

    val holdingMillis: Long? get() = saleDate?.let { it - purchaseDate }

    val soldPerGram: Long? get() = salePrice?.let { if (weightGram > 0) (it / weightGram).toLong() else null }
}

@Serializable
data class ItemStore(val items: List<GoldItem> = emptyList())
