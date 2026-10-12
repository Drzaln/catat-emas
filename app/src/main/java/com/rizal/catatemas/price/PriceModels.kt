package com.rizal.catatemas.price

import kotlinx.serialization.Serializable

@Serializable
data class TierPrice(
    val weight: Double,
    val sell: Long? = null,
    val buyback: Long? = null,
)

@Serializable
data class SourcePrices(
    val source: String,
    val recordedDate: String = "",
    val fetchedAt: Long = 0L,
    /** materialType -> daftar harga per berat */
    val tiers: Map<String, List<TierPrice>> = emptyMap(),
)

@Serializable
data class PriceStore(
    val sources: Map<String, SourcePrices> = emptyMap(),
    /** harga acuan 24K per gram */
    val spotPerGram: Long? = null,
    val updatedAt: Long = 0L,
)
