package com.rizal.catatemas.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rizal.catatemas.BuildConfig
import com.rizal.catatemas.price.GoldBrand
import com.rizal.catatemas.ui.Fmt
import com.rizal.catatemas.ui.PortfolioUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoSheet(
    state: PortfolioUiState,
    appVersion: String,
    updateMessage: String?,
    onCheckUpdate: () -> Unit,
    onDismissUpdateMessage: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Tentang aplikasi", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            InfoRow("Versi aplikasi", "v$appVersion")
            InfoRow("Repositori", BuildConfig.GITHUB_REPO)

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onCheckUpdate, shape = RoundedCornerShape(14.dp)) {
                    Text("Cek pembaruan")
                }
                if (updateMessage != null) {
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = updateMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismissUpdateMessage) {
                        Icon(Icons.Filled.Close, contentDescription = "Tutup pesan", modifier = Modifier.size(18.dp))
                    }
                }
            }

            Text(
                text = "Pembaruan diambil dari GitHub Releases (bukan Play Store). Android akan minta izin " +
                    "\"pasang aplikasi tidak dikenal\" saat memasang APK barunya.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider(Modifier.padding(vertical = 6.dp))

            Text("Tentang harga", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

            Text(
                text = "Harga diambil otomatis dari API publik logam-mulia-api (hasil pemantauan situs resmi: " +
                    "Logam Mulia Antam, Galeri 24, Pegadaian, IndoGold, Emasku/Hartadinata, Bank BSI, Sampoerna Gold, dll). " +
                    "Aplikasi ini tidak berafiliasi dengan merek mana pun.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(2.dp))
            InfoRow("Tanggal data harga", state.priceDate.ifBlank { "-" })
            InfoRow("Terakhir diperbarui", Fmt.relative(state.updatedAt))
            InfoRow("Acuan 24K / gram", state.spotPerGram?.let { Fmt.rupiah(it) } ?: "-")
            InfoRow("Antam buyback / gram", state.antamBuybackPerGram?.let { Fmt.rupiah(it) } ?: "-")
            InfoRow("UBS buyback / gram", state.ubsBuybackPerGram?.let { Fmt.rupiah(it) } ?: "-")

            HorizontalDivider(Modifier.padding(vertical = 6.dp))

            Text("Cara menghitung nilai", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Bullet("Logam mulia (batangan): pakai harga buyback resmi merk untuk berat tersebut. Kalau berat tidak ada di daftar harga (mis. 1,5 gr), harga per gram dihitung dari interpolasi berat terdekat.")
            Bullet("Perhiasan: estimasi nilai lebur = harga acuan 24K × (karat ÷ 24). Perhiasan umumnya dibeli toko di bawah nilai lebur.")
            Bullet("Untung/rugi = nilai terkini − harga beli. Angka ini estimasi, bukan penawaran resmi pembeli.")

            HorizontalDivider(Modifier.padding(vertical = 6.dp))

            Text("Merk yang didukung", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            GoldBrand.all.filter { it.sources.isNotEmpty() }.forEach { brand ->
                val sources = state.rows
                    .filter { it.brand == brand }
                    .mapNotNull { it.valuation.sourceLabel }
                    .distinct()
                Text(
                    text = "• ${brand.label}" + if (sources.isNotEmpty()) " — ${sources.joinToString(", ")}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Bullet("Merk \"Lainnya / Perhiasan\" memakai acuan harga 24K.")

            Spacer(Modifier.height(4.dp))
            Text(
                text = "Semua data emas kamu disimpan hanya di perangkat ini (offline), tidak dikirim ke mana pun. " +
                    "Yang keluar dari perangkat cuma permintaan harga ke API publik.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Bullet(text: String) {
    Text(
        text = "•  $text",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
