package com.rizal.catatemas.price

import com.rizal.catatemas.data.GoldItem
import com.rizal.catatemas.data.JsonStore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PriceRepository(
    private val api: GoldApi,
    private val store: JsonStore<PriceStore>,
) {
    private val _prices = MutableStateFlow(store.read() ?: PriceStore())
    val prices: StateFlow<PriceStore> = _prices.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val book: PriceBook get() = PriceBook(_prices.value)

    fun clearError() {
        _error.value = null
    }

    suspend fun refresh(sources: Collection<String>, force: Boolean) {
        if (_refreshing.value) return
        val targets = (sources + PriceSources.SPOT).filter { it.isNotBlank() }.distinct()
        if (targets.isEmpty()) return

        _refreshing.value = true
        _error.value = null
        try {
            val results = coroutineScope {
                targets.map { source -> async { source to runCatching { api.fetch(source, force) } } }.awaitAll()
            }

            val merged = _prices.value.sources.toMutableMap()
            var success = 0
            val failed = mutableListOf<String>()

            results.forEach { (source, result) ->
                result
                    .onSuccess { merged[source] = it; success++ }
                    .onFailure { failed += SourceLabel.of(source) }
            }

            if (success > 0) {
                val updated = _prices.value.copy(
                    sources = merged,
                    spotPerGram = SpotResolver.resolve(merged) ?: _prices.value.spotPerGram,
                    updatedAt = System.currentTimeMillis(),
                )
                _prices.value = updated
                store.write(updated)
            }

            _error.value = when {
                failed.isEmpty() -> null
                success == 0 -> "Gagal memperbarui harga (${failed.joinToString(", ")}). Cek koneksi internet."
                else -> "Sebagian harga gagal diambil: ${failed.joinToString(", ")}"
            }
        } finally {
            _refreshing.value = false
        }
    }

    /** refresh hanya kalau data sudah lebih tua dari [maxAgeMs] */
    suspend fun ensure(sources: Collection<String>, maxAgeMs: Long, force: Boolean = false) {
        val targets = (sources + PriceSources.SPOT).filter { it.isNotBlank() }.distinct()
        if (targets.isEmpty()) return
        val now = System.currentTimeMillis()
        val stale = targets.any { source ->
            val cached = _prices.value.sources[source]
            cached == null || cached.tiers.isEmpty() || now - cached.fetchedAt > maxAgeMs
        }
        if (stale || force) refresh(targets, force = false)
    }

    companion object {
        const val FRESH_MS = 30 * 60 * 1000L

        fun sourcesOf(brand: GoldBrand): List<String> = brand.sources.map { it.source }

        fun sourcesFor(items: List<GoldItem>): List<String> =
            items.flatMap { sourcesOf(GoldBrand.of(it.brandKey)) }.distinct()
    }
}
