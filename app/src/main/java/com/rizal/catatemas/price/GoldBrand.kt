package com.rizal.catatemas.price

/**
 * Sumber harga logam mulia (API publik, gratis, non-komersial):
 * https://logam-mulia-api.iamutaki.workers.dev
 *
 * Setiap merk dipetakan ke satu / beberapa sumber + nama materialType pada sumber tsb.
 * Urutan penting: sumber pertama yang punya data dipakai lebih dulu.
 */
data class BrandSource(val source: String, val materialType: String) {
    val label: String get() = SourceLabel.of(source)
}

object PriceSources {
    const val BASE_URL = "https://logam-mulia-api.iamutaki.workers.dev/api/prices"

    /** acuan harga 24K (spot / nilai lebur) */
    const val SPOT = "hargaemas-com"
    const val SPOT_TYPE = "Emas Spot"

    /** kalau sumber spot gagal, kira-kira nilai lebur dari harga jual merk */
    const val SPOT_RATIO = 0.93
    val SPOT_FALLBACKS = listOf(
        "indogold" to "Antam",
        "galeri24" to "ANTAM",
        "logammulia" to "Emas Batangan",
    )
}

object SourceLabel {
    private val map = mapOf(
        "anekalogam" to "Aneka Logam",
        "bankbsi" to "Bank BSI",
        "brankaslm" to "Brankas LM",
        "cermati" to "Cermati",
        "emasku" to "Emasku",
        "galeri24" to "Galeri 24",
        "hargaemas-com" to "hargaemas.com",
        "hargaemas-net" to "hargaemas.net",
        "hargaemas-org" to "hargaemas.org",
        "hartadinataabadi" to "Hartadinata Abadi",
        "indogold" to "IndoGold",
        "lakuemas" to "Laku Emas",
        "logammulia" to "Logam Mulia (Antam)",
        "pegadaian" to "Pegadaian",
        "sakumas" to "Sakumas",
        "sampoernagold" to "Sampoerna Gold",
        "treasury" to "Treasury",
    )

    fun of(source: String): String = map[source] ?: source
}

enum class GoldBrand(
    val key: String,
    val label: String,
    val emblem: String,
    val accent: Long,
    val sources: List<BrandSource>,
) {
    ANTAM(
        key = "antam",
        label = "Antam",
        emblem = "ANTAM",
        accent = 0xFFD4A017,
        sources = listOf(
            BrandSource("indogold", "Antam"),
            BrandSource("galeri24", "ANTAM"),
            BrandSource("logammulia", "Emas Batangan"),
        ),
    ),
    ANTAM_RETRO(
        key = "antam_retro",
        label = "Antam Retro",
        emblem = "RETRO",
        accent = 0xFFC99A2E,
        sources = listOf(
            BrandSource("galeri24", "ANTAM MULIA RETRO"),
            BrandSource("indogold", "Antam"),
        ),
    ),
    ANTAM_NON_PEGADAIAN(
        key = "antam_non_pegadaian",
        label = "Antam Non Pegadaian",
        emblem = "ANTAM",
        accent = 0xFFC7A11F,
        sources = listOf(BrandSource("galeri24", "ANTAM NON PEGADAIAN")),
    ),
    UBS(
        key = "ubs",
        label = "UBS",
        emblem = "UBS",
        accent = 0xFF1E6F5C,
        sources = listOf(
            BrandSource("galeri24", "UBS"),
            BrandSource("indogold", "UBS"),
        ),
    ),
    UBS_KARAKTER(
        key = "ubs_karakter",
        label = "UBS Disney / Karakter",
        emblem = "UBS",
        accent = 0xFF3F6FBF,
        sources = listOf(
            BrandSource("galeri24", "UBS MICKEY FULLBODY"),
            BrandSource("galeri24", "UBS ELSA"),
            BrandSource("galeri24", "UBS HELLO KITTY"),
        ),
    ),
    LOTUS_ARCHI(
        key = "lotus_archi",
        label = "Lotus Archi",
        emblem = "LOTUS",
        accent = 0xFFB25E8A,
        sources = listOf(BrandSource("galeri24", "LOTUS ARCHI")),
    ),
    GALERI24(
        key = "galeri24",
        label = "Galeri 24",
        emblem = "G24",
        accent = 0xFF2F6FB0,
        sources = listOf(BrandSource("galeri24", "GALERI 24")),
    ),
    GALERI24_BATIK(
        key = "galeri24_batik",
        label = "Galeri 24 Batik Series",
        emblem = "BATIK",
        accent = 0xFF2A5C93,
        sources = listOf(BrandSource("galeri24", "BATIK SERIES")),
    ),
    DINAR_G24(
        key = "dinar_g24",
        label = "Dinar G24",
        emblem = "DINAR",
        accent = 0xFF9C6B1F,
        sources = listOf(BrandSource("galeri24", "DINAR G24")),
    ),
    INDOGOLD(
        key = "indogold",
        label = "IndoGold",
        emblem = "IG",
        accent = 0xFF8E5BD6,
        sources = listOf(BrandSource("indogold", "IndoGold")),
    ),
    EMASKU(
        key = "emasku",
        label = "Emasku (Hartadinata)",
        emblem = "EMASKU",
        accent = 0xFFE08A2E,
        sources = listOf(
            BrandSource("emasku", "Gold"),
            BrandSource("hartadinataabadi", "EMASKU"),
        ),
    ),
    PEGADAIAN(
        key = "pegadaian",
        label = "Pegadaian",
        emblem = "PGA",
        accent = 0xFF1F7A5C,
        sources = listOf(
            BrandSource("galeri24", "SENTRA BUYBACK"),
            BrandSource("hargaemas-org", "Emas Logam Mulia Pegadaian"),
        ),
    ),
    BANK_BSI(
        key = "bsi",
        label = "Bank BSI",
        emblem = "BSI",
        accent = 0xFF0E7490,
        sources = listOf(BrandSource("bankbsi", "Emas")),
    ),
    SAMPORNA(
        key = "samporna",
        label = "Sampoerna Gold",
        emblem = "SGLD",
        accent = 0xFFB5453B,
        sources = listOf(BrandSource("sampoernagold", "unknown")),
    ),
    BRANKAS(
        key = "brankas",
        label = "Brankas LM",
        emblem = "BRK",
        accent = 0xFF6B7280,
        sources = listOf(BrandSource("brankaslm", "Emas Fisik")),
    ),
    LAKU_EMAS(
        key = "lakuemas",
        label = "Laku Emas",
        emblem = "LAKU",
        accent = 0xFF7C9A2E,
        sources = listOf(BrandSource("lakuemas", "Laku Emas")),
    ),
    TREASURY(
        key = "treasury",
        label = "Treasury",
        emblem = "TRSY",
        accent = 0xFF4F6D8F,
        sources = listOf(BrandSource("treasury", "unknown")),
    ),
    ANEKA_LOGAM(
        key = "anekalogam",
        label = "Aneka Logam",
        emblem = "ANEKA",
        accent = 0xFFA0562E,
        sources = listOf(BrandSource("anekalogam", "LM Antam produksi tahun 2026")),
    ),
    EMAS_DIGITAL(
        key = "emas_digital",
        label = "Emas Digital",
        emblem = "DIGI",
        accent = 0xFF5B8DEF,
        sources = listOf(BrandSource("cermati", "Emas Digital")),
    ),
    LAINNYA(
        key = "lainnya",
        label = "Lainnya / Perhiasan",
        emblem = "EMAS",
        accent = 0xFF8D6E63,
        sources = emptyList(),
    ),
    ;

    companion object {
        val all: List<GoldBrand> = entries
        fun of(key: String?): GoldBrand = entries.firstOrNull { it.key == key } ?: LAINNYA
    }
}
