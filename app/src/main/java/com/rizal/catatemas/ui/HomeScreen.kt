@file:OptIn(ExperimentalMaterial3Api::class)

package com.rizal.catatemas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rizal.catatemas.data.ItemKind
import com.rizal.catatemas.price.ValueBasis
import com.rizal.catatemas.ui.components.Dot
import com.rizal.catatemas.ui.components.EmptyPortfolio
import com.rizal.catatemas.ui.components.ErrorBanner
import com.rizal.catatemas.ui.components.GoldBarIllustration
import com.rizal.catatemas.ui.components.HeaderStat
import com.rizal.catatemas.ui.components.ProfitChip
import com.rizal.catatemas.ui.components.StatPill
import com.rizal.catatemas.ui.theme.GoldGradient
import com.rizal.catatemas.ui.theme.HeaderInk

private enum class SortOption(val label: String) {
    TERBARU("Terbaru"),
    TERLAMA("Terlama"),
    BERAT("Berat terbesar"),
    UNTUNG("Untung terbesar"),
    MODAL("Modal terbesar"),
}

private enum class KindFilter(val label: String) {
    SEMUA("Semua"),
    BATANGAN("Logam mulia"),
    PERHIASAN("Perhiasan"),
}

private enum class HomeTab(val label: String) {
    DIMILIKI("Dimiliki"),
    TERJUAL("Riwayat jual"),
}

@Composable
fun HomeScreen(
    state: PortfolioUiState,
    zakat: ZakatUiState,
    onAdd: () -> Unit,
    onOpenItem: (String) -> Unit,
    onOpenZakat: () -> Unit,
    onOpenSecurity: () -> Unit,
    onOpenInfo: () -> Unit,
    onRefresh: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(KindFilter.SEMUA) }
    var sort by rememberSaveable { mutableStateOf(SortOption.TERBARU) }
    var sortMenu by remember { mutableStateOf(false) }
    var tab by rememberSaveable { mutableStateOf(HomeTab.DIMILIKI) }

    val rows = remember(state.rows, query, filter, sort) {
        state.rows
            .filter { row ->
                when (filter) {
                    KindFilter.SEMUA -> true
                    KindFilter.BATANGAN -> row.item.kind == ItemKind.BATANGAN
                    KindFilter.PERHIASAN -> row.item.kind == ItemKind.PERHIASAN
                }
            }
            .filter { row -> matchesQuery(row.brand.label, row.item.note, row.item.karatLabel, query) }
            .let { list -> sortRows(list, sort) }
    }

    val soldRows = remember(state.soldRows, query) {
        state.soldRows.filter { row -> matchesQuery(row.brand.label, row.item.note, row.item.karatLabel, query) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Catat Emas",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = when {
                                    state.updatedAt <= 0L -> "Harga belum dimuat"
                                    else -> "Harga ${state.priceDate.ifBlank { "-" }} • ${Fmt.relative(state.updatedAt)}"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onRefresh, enabled = !state.refreshing) {
                            if (state.refreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            } else {
                                Icon(Icons.Filled.Refresh, contentDescription = "Perbarui harga")
                            }
                        }
                        IconButton(onClick = onOpenInfo) {
                            Icon(Icons.Filled.Info, contentDescription = "Tentang aplikasi & harga")
                        }
                        IconButton(onClick = onOpenSecurity) {
                            Icon(Icons.Filled.Lock, contentDescription = "Keamanan")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
                PrimaryTabRow(
                    selectedTabIndex = tab.ordinal,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {},
                ) {
                    HomeTab.entries.forEach { option ->
                        Tab(
                            selected = tab == option,
                            onClick = { tab = option },
                            text = {
                                Text(
                                    text = if (option == HomeTab.TERJUAL && state.soldRows.isNotEmpty()) {
                                        "${option.label} (${state.soldRows.size})"
                                    } else {
                                        option.label
                                    },
                                    fontWeight = if (tab == option) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Tambah") },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 4.dp,
                bottom = innerPadding.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (tab) {
                HomeTab.DIMILIKI -> {
                    item { PortfolioHeader(state.totals, state.rows.size, state.priceDate, state.hasPriceData) }

                    if (state.error != null) item { ErrorBanner(state.error.orEmpty()) }

                    item { TodayPriceStrip(state) }

                    if (state.rows.isNotEmpty()) {
                        item { ZakatSummaryCard(zakat = zakat, onClick = onOpenZakat) }
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SearchField(query = query, onQueryChange = { query = it })
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                LazyRow(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    items(KindFilter.entries.toList()) { option ->
                                        FilterChip(
                                            selected = filter == option,
                                            onClick = { filter = option },
                                            label = { Text(option.label) },
                                            colors = chipColors(),
                                        )
                                    }
                                }
                                SortMenu(sort = sort, expanded = sortMenu, onExpandedChange = { sortMenu = it }, onSelect = { sort = it })
                            }
                        }
                    }

                    if (rows.isEmpty()) {
                        item { EmptyPortfolio() }
                    } else {
                        items(rows, key = { it.item.id }) { row ->
                            GoldItemCard(row = row, onClick = { onOpenItem(row.item.id) })
                        }
                        item { Footnote() }
                    }
                }

                HomeTab.TERJUAL -> {
                    item { SoldHeader(totals = state.soldTotals) }

                    if (state.soldRows.isNotEmpty()) {
                        item { SearchField(query = query, onQueryChange = { query = it }) }
                    }

                    if (soldRows.isEmpty()) {
                        item { EmptySold() }
                    } else {
                        items(soldRows, key = { it.item.id }) { row ->
                            SoldItemCard(row = row, onClick = { onOpenItem(row.item.id) })
                        }
                        item {
                            Text(
                                text = "Riwayat ini menyimpan tanggal & harga jual, untung/rugi realisasi, dan lama kamu menyimpan emasnya.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun matchesQuery(label: String, note: String, karat: String, query: String): Boolean {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return true
    return label.lowercase().contains(q) || note.lowercase().contains(q) || karat.lowercase().contains(q)
}

private fun sortRows(list: List<ItemRow>, sort: SortOption): List<ItemRow> = when (sort) {
    SortOption.TERBARU -> list.sortedByDescending { it.item.purchaseDate }
    SortOption.TERLAMA -> list.sortedBy { it.item.purchaseDate }
    SortOption.BERAT -> list.sortedByDescending { it.item.weightGram }
    SortOption.UNTUNG -> list.sortedByDescending { it.profit ?: Long.MIN_VALUE }
    SortOption.MODAL -> list.sortedByDescending { it.item.purchasePrice }
}

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
)

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Cari merk, karat, atau catatan") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
    )
}

@Composable
private fun SortMenu(
    sort: SortOption,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (SortOption) -> Unit,
) {
    Box {
        IconButton(onClick = { onExpandedChange(true) }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Urutkan")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            SortOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.label,
                            fontWeight = if (sort == option) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    onClick = {
                        onSelect(option)
                        onExpandedChange(false)
                    },
                )
            }
        }
    }
}

@Composable
private fun Footnote() {
    Text(
        text = "Harga mengikuti sumber publik (buyback resmi tiap merk / acuan 24K untuk perhiasan). Bukan penawaran resmi. " +
            "Tap item lalu pilih \"Tandai terjual\" kalau emasnya sudah dijual.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
    )
}

@Composable
private fun PortfolioHeader(
    totals: PortfolioTotals,
    itemCount: Int,
    priceDate: String,
    hasPriceData: Boolean,
) {
    val profit = totals.profit
    val profitColor = when {
        totals.pricedCount == 0 -> HeaderInk
        profit > 0L -> Color(0xFF0B5B3C)
        profit < 0L -> Color(0xFF8C1D18)
        else -> HeaderInk
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(26.dp), clip = true)
            .clip(RoundedCornerShape(26.dp))
            .background(GoldGradient),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = "TOTAL EMAS",
                style = MaterialTheme.typography.labelMedium,
                color = HeaderInk.copy(alpha = 0.72f),
                fontWeight = FontWeight.SemiBold,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = Fmt.gramNumber(totals.weightGram),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = HeaderInk,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "gram",
                    style = MaterialTheme.typography.titleSmall,
                    color = HeaderInk.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Estimasi nilai terkini",
                style = MaterialTheme.typography.labelMedium,
                color = HeaderInk.copy(alpha = 0.72f),
            )
            Text(
                text = Fmt.rupiah(totals.value),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = HeaderInk,
            )

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                HeaderStat(label = "Modal", value = Fmt.rupiah(totals.cost))
                HeaderStat(
                    label = if (profit >= 0L) "Untung" else "Rugi",
                    value = Fmt.rupiahSigned(profit) + (totals.profitPercent?.let { "  ${Fmt.percent(it)}" } ?: ""),
                    valueColor = profitColor,
                )
            }

            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$itemCount item",
                    style = MaterialTheme.typography.labelSmall,
                    color = HeaderInk.copy(alpha = 0.8f),
                )
                Dot(Modifier)
                Text(
                    text = if (hasPriceData) "Harga ${priceDate.ifBlank { "-" }}" else "Menunggu harga",
                    style = MaterialTheme.typography.labelSmall,
                    color = HeaderInk.copy(alpha = 0.8f),
                )
                if (totals.unpricedCount > 0) {
                    Dot(Modifier)
                    Text(
                        text = "${totals.unpricedCount} belum ada harga",
                        style = MaterialTheme.typography.labelSmall,
                        color = HeaderInk.copy(alpha = 0.8f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SoldHeader(totals: SoldTotals) {
    val profit = totals.profit
    val profitColor = when {
        totals.count == 0 -> HeaderInk
        profit > 0L -> Color(0xFF0B5B3C)
        profit < 0L -> Color(0xFF8C1D18)
        else -> HeaderInk
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(26.dp), clip = true)
            .clip(RoundedCornerShape(26.dp))
            .background(GoldGradient),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = "TOTAL HASIL PENJUALAN",
                style = MaterialTheme.typography.labelMedium,
                color = HeaderInk.copy(alpha = 0.72f),
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = Fmt.rupiah(totals.proceeds),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = HeaderInk,
            )

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                HeaderStat(label = "Modal", value = Fmt.rupiah(totals.cost))
                HeaderStat(
                    label = if (profit >= 0L) "Untung" else "Rugi",
                    value = Fmt.rupiahSigned(profit) + (totals.profitPercent?.let { "  ${Fmt.percent(it)}" } ?: ""),
                    valueColor = profitColor,
                )
            }

            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${totals.count} item terjual",
                    style = MaterialTheme.typography.labelSmall,
                    color = HeaderInk.copy(alpha = 0.8f),
                )
                if (totals.averageHoldingMillis > 0L) {
                    Dot(Modifier)
                    Text(
                        text = "rata-rata disimpan ${Fmt.duration(0L, totals.averageHoldingMillis)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = HeaderInk.copy(alpha = 0.8f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ZakatSummaryCard(zakat: ZakatUiState, onClick: () -> Unit) {
    val statusColor = if (zakat.wajib) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
    val statusText = when {
        zakat.wajib -> "Wajib"
        zakat.reachesNisab -> "Tunggu haul"
        else -> "Belum wajib"
    }

    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text("🕌", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Zakat emas",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(statusColor.copy(alpha = 0.14f))
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                    )
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = when {
                        zakat.wajib -> "2,5% = ${Fmt.rupiah(zakat.zakatRupiah)} (setara ${Fmt.gram(zakat.zakatGram)})"
                        zakat.reachesNisab -> "Nisab tercapai — zakat saat haul genap${zakat.haulCompleteAt?.let { " (${Fmt.date(it)})" } ?: ""}"
                        else -> "Kurang ${Fmt.gram(zakat.shortfallGram)} lagi menuju nisab 85 gr murni"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (zakat.wajib) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (zakat.wajib) FontWeight.SemiBold else FontWeight.Normal,
                )

                Spacer(Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { zakat.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50)),
                    color = if (zakat.wajib) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }

            Spacer(Modifier.width(10.dp))
            Text("›", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TodayPriceStrip(state: PortfolioUiState) {
    if (!state.hasPriceData) return
    val stats = buildList {
        state.spotPerGram?.let { add("Acuan 24K/gr" to Fmt.rupiah(it)) }
        state.antamBuybackPerGram?.let { add("Antam buyback/gr" to Fmt.rupiah(it)) }
        state.ubsBuybackPerGram?.let { add("UBS buyback/gr" to Fmt.rupiah(it)) }
    }
    if (stats.isEmpty()) return

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(stats) { stat ->
            StatPill(label = stat.first, value = stat.second)
        }
    }
}

@Composable
private fun EmptySold() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("🧾", style = MaterialTheme.typography.displaySmall)
        Text(
            text = "Belum ada emas yang dijual",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Buka catatan emas di tab \"Dimiliki\", lalu pilih \"Tandai terjual\" untuk menyimpan tanggal & harga jualnya.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun GoldItemCard(row: ItemRow, onClick: () -> Unit) {
    val item = row.item
    val valuation = row.valuation

    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Row(Modifier.padding(14.dp)) {
            GoldBarIllustration(
                emblem = row.brand.emblem,
                weightText = Fmt.gram(item.weightGram),
                accent = Color(row.brand.accent),
                photoPath = item.photoPath,
                modifier = Modifier.size(76.dp),
            )

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = row.brand.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(8.dp))
                    KindBadge(kind = item.kind)
                }

                Text(
                    text = "${item.karatLabel} • ${Fmt.gram(item.weightGram)} • beli ${Fmt.date(item.purchaseDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.height(10.dp))

                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        MiniLabel("HARGA BELI")
                        Text(
                            text = Fmt.rupiah(item.purchasePrice),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        MiniLabel("NILAI KINI")
                        Text(
                            text = valuation.value?.let { Fmt.rupiah(it) } ?: "—",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = when (valuation.basis) {
                                ValueBasis.TIDAK_ADA -> MaterialTheme.colorScheme.onSurfaceVariant
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProfitChip(profit = row.profit, percent = row.profitPercent, compact = true)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = valuation.perGram?.let { "${Fmt.rupiahCompact(it)}/gr" }.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun SoldItemCard(row: SoldRow, onClick: () -> Unit) {
    val item = row.item
    val sinceSale = row.priceSinceSalePercent

    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Row(Modifier.padding(14.dp)) {
            GoldBarIllustration(
                emblem = row.brand.emblem,
                weightText = Fmt.gram(item.weightGram),
                accent = Color(row.brand.accent),
                photoPath = item.photoPath,
                modifier = Modifier.size(76.dp),
            )

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = row.brand.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Terjual",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }

                Text(
                    text = "${item.karatLabel} • ${Fmt.gram(item.weightGram)} • terjual ${item.saleDate?.let { Fmt.date(it) } ?: "-"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.height(10.dp))

                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        MiniLabel("MODAL")
                        Text(
                            text = Fmt.rupiah(item.purchasePrice),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        MiniLabel("HASIL JUAL")
                        Text(
                            text = item.salePrice?.let { Fmt.rupiah(it) } ?: "-",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProfitChip(profit = row.profit, percent = row.profitPercent, compact = true)
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = buildString {
                        append(row.soldPerGram?.let { "${Fmt.rupiahCompact(it)}/gr saat jual" }.orEmpty())
                        if (sinceSale != null) {
                            append(" • kini ")
                            append(if (sinceSale >= 0) "+" else "")
                            append(Fmt.percent(sinceSale))
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = buildString {
                        append("Beli ${Fmt.date(item.purchaseDate)}")
                        if (row.holdingLabel.isNotEmpty()) append(" • disimpan ${row.holdingLabel}")
                        if (item.salePlace.isNotBlank()) append(" • ${item.salePlace}")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun MiniLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun KindBadge(kind: ItemKind) {
    Text(
        text = if (kind == ItemKind.BATANGAN) "LM" else "Perhiasan",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}
