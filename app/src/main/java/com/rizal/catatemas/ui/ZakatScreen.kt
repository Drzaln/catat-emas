@file:OptIn(ExperimentalMaterial3Api::class)

package com.rizal.catatemas.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rizal.catatemas.ui.components.InfoRow
import com.rizal.catatemas.ui.theme.GoldGradient
import com.rizal.catatemas.ui.theme.HeaderInk

@Composable
fun ZakatScreen(
    state: ZakatUiState,
    priceDate: String,
    onToggleJewelry: (Boolean) -> Unit,
    onClose: () -> Unit,
) {
    BackHandler { onClose() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Zakat emas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ZakatStatusCard(state)

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("Rincian hitungan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    HorizontalDivider()
                    InfoRow("Total emas dimiliki", Fmt.gram(state.totalGram24) + " murni 24K")
                    InfoRow("• Logam mulia", Fmt.gram(state.bullionGram24))
                    InfoRow("• Perhiasan", Fmt.gram(state.jewelryGram24))
                    HorizontalDivider()
                    InfoRow("Nisab", "${Fmt.gramNumber(state.nisabGram)} gr emas murni")
                    state.nisabValue?.let { InfoRow("Setara nilai nisab", Fmt.rupiah(it)) }
                    InfoRow(
                        "Haul (1 tahun hijriah)",
                        when {
                            state.haulCompleteAt == null -> "-"
                            state.haulMet -> "Sudah terpenuhi"
                            else -> "Genap ${Fmt.date(state.haulCompleteAt)}"
                        },
                    )
                    HorizontalDivider()
                    InfoRow("Masuk hitungan zakat", Fmt.gram(state.baseGram24) + " murni 24K")
                    InfoRow(
                        "Nilainya",
                        if (state.baseValue > 0L) Fmt.rupiah(state.baseValue) + if (state.valueComplete) "" else " (sebagian)"
                        else "-",
                    )
                    InfoRow("Kadar zakat", "2,5% (1/40)")
                    HorizontalDivider()
                    InfoRow("Zakat yang harus dibayar", zakatAmountLabel(state))
                }
            }

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Hitung perhiasan juga",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = if (state.includeJewelry) {
                                "Semua emas (logam mulia + perhiasan) masuk hitungan. Pendapat fatwa MUI & mazhab Hanafi: perhiasan wajib dizakati bila mencapai nisab."
                            } else {
                                "Hanya logam mulia yang dihitung. Pendapat jumhur ulama: perhiasan yang dipakai (bukan disimpan untuk investasi) tidak wajib zakat."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(checked = state.includeJewelry, onCheckedChange = onToggleJewelry)
                }
            }

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Catatan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Note("Berat semua item dikonversi ke emas murni 24K dulu: gram × (karat ÷ 24). Jadi perhiasan 17K 10 gr = 7,08 gr murni.")
                    Note("Nisab 85 gram emas murni, haul 1 tahun hijriah (354 hari), kadar 2,5%.")
                    Note("Emas yang sudah kamu tandai terjual tidak dihitung lagi di sini.")
                    Note("Zakat boleh dibayar dengan emasnya langsung atau uang senilai harga emas saat membayar.")
                    Note("Haul di sini dihitung dari tanggal pembelian tertua pada emas yang masuk hitungan (perkiraan konservatif). Kalau jumlah emas pernah turun di bawah nisab, haul seharusnya dimulai ulang — silakan sesuaikan sendiri.")
                    Note("Nilai memakai estimasi harga buyback/nilai lebur hari ini (data harga: ${priceDate.ifBlank { "-" }}). Sebagian ulama memakai harga pasar/harga jual baru.")
                    Note("Salurkan ke 8 asnaf, boleh lewat amil resmi seperti BAZNAS atau LAZ terpercaya.")
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Fitur ini alat bantu hitung, bukan fatwa. Untuk kondisi khusus, tanyakan ke lembaga zakat atau ustadz terpercaya.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ZakatStatusCard(state: ZakatUiState) {
    val statusText = when {
        !state.hasItems -> "Belum ada data"
        state.wajib -> "Wajib zakat"
        state.reachesNisab -> "Tunggu haul"
        else -> "Belum wajib zakat"
    }

    val reason = when {
        !state.hasItems -> "Tambahkan catatan emas dulu untuk menghitung zakat."
        state.wajib -> "Emasmu sudah mencapai nisab dan kepemilikannya sudah lewat 1 tahun hijriah."
        state.reachesNisab -> "Sudah mencapai nisab, tapi haul belum genap. Zakat ditunaikan setelah " +
            (state.haulCompleteAt?.let { Fmt.dateFull(it) } ?: "-") + "."
        else -> "Kurang ${Fmt.gram(state.shortfallGram)} lagi untuk mencapai nisab 85 gr emas murni."
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(GoldGradient),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = "STATUS ZAKAT EMAS",
                style = MaterialTheme.typography.labelMedium,
                color = HeaderInk.copy(alpha = 0.72f),
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = HeaderInk,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = reason,
                style = MaterialTheme.typography.bodySmall,
                color = HeaderInk.copy(alpha = 0.85f),
            )

            Spacer(Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)),
                color = HeaderInk.copy(alpha = 0.85f),
                trackColor = Color.White.copy(alpha = 0.45f),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${Fmt.gram(state.baseGram24)} dari nisab ${Fmt.gramNumber(state.nisabGram)} gr murni (24K)",
                style = MaterialTheme.typography.labelSmall,
                color = HeaderInk.copy(alpha = 0.8f),
            )

            if (state.reachesNisab) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = if (state.wajib) "Zakat yang wajib dibayar" else "Perkiraan zakat saat haul genap",
                    style = MaterialTheme.typography.labelMedium,
                    color = HeaderInk.copy(alpha = 0.72f),
                )
                Text(
                    text = if (state.baseValue > 0L) Fmt.rupiah(state.zakatRupiah) else "menunggu harga",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = HeaderInk,
                )
                Text(
                    text = "atau setara ${Fmt.gram(state.zakatGram)} emas murni (24K)",
                    style = MaterialTheme.typography.bodySmall,
                    color = HeaderInk.copy(alpha = 0.85f),
                )
            }
        }
    }
}

private fun zakatAmountLabel(state: ZakatUiState): String = when {
    !state.hasItems -> "-"
    !state.reachesNisab -> "Belum wajib (belum nisab)"
    state.baseValue <= 0L -> "${Fmt.gram(state.zakatGram)} emas murni"
    else -> Fmt.rupiah(state.zakatRupiah) + " / " + Fmt.gram(state.zakatGram) + " emas"
}

@Composable
private fun Note(text: String) {
    Text(
        text = "•  $text",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
