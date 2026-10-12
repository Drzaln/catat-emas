package com.rizal.catatemas.price

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class GoldApi(private val client: OkHttpClient = defaultClient()) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun fetch(source: String, force: Boolean = false): SourcePrices = withContext(Dispatchers.IO) {
        val url = buildString {
            append(PriceSources.BASE_URL)
            append('/')
            append(source)
            if (force) append("?refresh=true")
        }
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "CatatEmas/1.0 (Android)")
            .header("Accept", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) throw IOException("Respons kosong")
            toSourcePrices(source, json.decodeFromString<ApiResponse>(body))
        }
    }

    private fun toSourcePrices(source: String, parsed: ApiResponse): SourcePrices {
        val tiers = linkedMapOf<String, MutableList<TierPrice>>()
        var recorded = ""

        parsed.data.forEach { row ->
            val type = row.materialType?.trim().orEmpty()
            val weight = row.weight ?: return@forEach
            if (type.isEmpty() || weight <= 0.0) return@forEach
            if (row.material != null && row.material != "gold") return@forEach

            val unit = row.weightUnit?.lowercase().orEmpty()
            val grams = if (unit.startsWith("kg")) weight * 1000.0 else weight

            val sell = row.sellPrice?.takeIf { it > 0L }
            val buyback = row.buybackPrice?.takeIf { it > 0L }
            if (sell == null && buyback == null) return@forEach

            tiers.getOrPut(type) { mutableListOf() }.add(TierPrice(grams, sell, buyback))
            val date = row.recordedDate.orEmpty()
            if (date.length > recorded.length) recorded = date
        }

        if (tiers.isEmpty()) throw IOException("Data harga kosong")

        return SourcePrices(
            source = source,
            recordedDate = recorded,
            fetchedAt = System.currentTimeMillis(),
            tiers = tiers.mapValues { (_, value) -> value.sortedBy { it.weight } },
        )
    }

    companion object {
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(35, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}

@Serializable
private data class ApiResponse(
    val success: Boolean = false,
    val data: List<ApiRow> = emptyList(),
)

@Serializable
private data class ApiRow(
    val source: String? = null,
    val material: String? = null,
    val materialType: String? = null,
    val weight: Double? = null,
    val weightUnit: String? = null,
    val sellPrice: Long? = null,
    val buybackPrice: Long? = null,
    val recordedDate: String? = null,
)
