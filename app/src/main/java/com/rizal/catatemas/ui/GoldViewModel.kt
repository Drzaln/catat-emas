package com.rizal.catatemas.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rizal.catatemas.AppContainer
import com.rizal.catatemas.backup.BackupManager
import com.rizal.catatemas.data.GoldItem
import com.rizal.catatemas.data.ItemRepository
import com.rizal.catatemas.data.PhotoStore
import com.rizal.catatemas.price.GoldBrand
import com.rizal.catatemas.price.PriceBook
import com.rizal.catatemas.price.PriceRepository
import com.rizal.catatemas.price.PriceStore
import com.rizal.catatemas.price.Valuation
import com.rizal.catatemas.security.SecurityManager
import com.rizal.catatemas.security.SecurityState
import com.rizal.catatemas.update.UpdateState
import com.rizal.catatemas.update.Updater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class ItemRow(
    val item: GoldItem,
    val brand: GoldBrand,
    val valuation: Valuation,
    val profit: Long?,
    val profitPercent: Double?,
)

data class SoldRow(
    val item: GoldItem,
    val brand: GoldBrand,
    /** nilai pasar hari ini kalau masih disimpan (info pergerakan harga) */
    val currentValue: Long?,
) {
    val profit: Long = item.realizedProfit ?: 0L
    val profitPercent: Double? get() = item.realizedPercent
    val soldPerGram: Long? get() = item.soldPerGram
    val holdingLabel: String
        get() = item.saleDate?.let { Fmt.duration(item.purchaseDate, it) }.orEmpty()

    /** selisih harga sekarang vs harga waktu dijual */
    val priceSinceSalePercent: Double?
        get() {
            val sold = item.salePrice ?: return null
            val now = currentValue ?: return null
            if (sold <= 0L) return null
            return (now - sold) * 100.0 / sold
        }
}

data class SoldTotals(
    val count: Int = 0,
    val weightGram: Double = 0.0,
    val cost: Long = 0L,
    val proceeds: Long = 0L,
    val averageHoldingMillis: Long = 0L,
) {
    val profit: Long get() = proceeds - cost
    val profitPercent: Double? get() = if (cost > 0L) profit * 100.0 / cost else null
}

data class PortfolioTotals(
    val weightGram: Double = 0.0,
    val cost: Long = 0L,
    val value: Long = 0L,
    val pricedCount: Int = 0,
    val unpricedCount: Int = 0,
) {
    val profit: Long get() = value - cost
    val profitPercent: Double? get() = if (cost > 0L) profit * 100.0 / cost else null
}

data class PortfolioUiState(
    val rows: List<ItemRow> = emptyList(),
    val totals: PortfolioTotals = PortfolioTotals(),
    val soldRows: List<SoldRow> = emptyList(),
    val soldTotals: SoldTotals = SoldTotals(),
    val refreshing: Boolean = false,
    val error: String? = null,
    val updatedAt: Long = 0L,
    val priceDate: String = "",
    val spotPerGram: Long? = null,
    val antamBuybackPerGram: Long? = null,
    val ubsBuybackPerGram: Long? = null,
    val hasPriceData: Boolean = false,
)

class GoldViewModel(
    private val items: ItemRepository,
    private val prices: PriceRepository,
    private val photos: PhotoStore,
    private val security: SecurityManager,
    private val updater: Updater,
    private val backupManager: BackupManager,
) : ViewModel() {

    /** dipakai layar edit untuk estimasi harga live */
    val priceStore: StateFlow<PriceStore> = prices.prices

    // pembaruan aplikasi (GitHub Releases)
    private val _update = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val update: StateFlow<UpdateState> = _update

    private val _updateMessage = MutableStateFlow<String?>(null)
    val updateMessage: StateFlow<String?> = _updateMessage

    val appVersion: String get() = updater.currentVersion

    private var updateChecked = false

    fun checkForUpdate(force: Boolean = false) {
        if (updateChecked && !force) return
        updateChecked = true
        if (force) _update.value = UpdateState.Idle
        viewModelScope.launch {
            val latest = runCatching { updater.latestVersion() }.getOrNull()
            if (latest == null) {
                if (force) _updateMessage.value = "Tidak bisa menghubungi GitHub."
                return@launch
            }
            if (!updater.isNewer(latest, updater.currentVersion)) {
                if (force) _updateMessage.value = "Kamu sudah pakai versi terbaru (v${updater.currentVersion})."
                return@launch
            }
            if (!force && updater.skippedVersion() == latest) return@launch
            _update.value = UpdateState.Available(latest)
        }
    }

    fun downloadUpdate(version: String) {
        if (_update.value is UpdateState.Downloading) return
        viewModelScope.launch {
            _update.value = UpdateState.Downloading(version, 0)
            try {
                val file = updater.download { percent ->
                    _update.value = UpdateState.Downloading(version, percent)
                }
                _update.value = UpdateState.Ready(version, file)
            } catch (e: Exception) {
                _update.value = UpdateState.Failed(e.message ?: "Unduhan gagal")
            }
        }
    }

    fun installUpdate(file: File) {
        if (!updater.canRequestInstalls()) {
            updater.openInstallSettings()
            return
        }
        runCatching { updater.install(file) }
            .onFailure { _update.value = UpdateState.Failed("Tidak bisa membuka pemasang APK") }
    }

    /** "Nanti" = jangan tawarkan versi ini lagi */
    fun dismissUpdate() {
        (_update.value as? UpdateState.Available)?.version?.let { updater.skipVersion(it) }
        _update.value = UpdateState.Idle
    }

    fun clearUpdateMessage() {
        _updateMessage.value = null
    }

    // ------------------------------------------------------------- backup (export/import terenkripsi)

    private val _backupStatus = MutableStateFlow(BackupStatus())
    val backupStatus: StateFlow<BackupStatus> = _backupStatus

    fun exportBackup(uri: Uri, passphrase: String) {
        viewModelScope.launch {
            _backupStatus.value = BackupStatus(working = true)
            backupManager.export(uri, passphrase)
                .onSuccess { count ->
                    _backupStatus.value = BackupStatus(message = "Backup $count catatan tersimpan.")
                }
                .onFailure { error ->
                    _backupStatus.value = BackupStatus(
                        message = error.message ?: "Export gagal",
                        isError = true,
                    )
                }
        }
    }

    fun importBackup(uri: Uri, passphrase: String) {
        viewModelScope.launch {
            _backupStatus.value = BackupStatus(working = true)
            backupManager.import(uri, passphrase)
                .onSuccess { count ->
                    _backupStatus.value = BackupStatus(message = "$count catatan dipulihkan.")
                    prices.ensure(
                        PriceRepository.sourcesFor(items.items.value),
                        maxAgeMs = PriceRepository.FRESH_MS,
                    )
                }
                .onFailure { error ->
                    _backupStatus.value = BackupStatus(
                        message = error.message ?: "Restore gagal",
                        isError = true,
                    )
                }
        }
    }

    fun clearBackupStatus() {
        _backupStatus.value = BackupStatus()
    }

    // keamanan
    val securityState: StateFlow<SecurityState> = security.state
    val locked: StateFlow<Boolean> = security.locked
    val pinLength: Int get() = security.pinLength

    fun unlock() = security.unlock()
    fun lockNow() = security.lockNow()
    fun verifyPin(pin: String): Boolean = security.verifyPin(pin)
    fun setPin(pin: String) = security.setPin(pin)
    fun clearPin() = security.clearPin()
    fun setBiometricEnabled(enabled: Boolean) = security.setBiometricEnabled(enabled)
    fun setSecureScreen(enabled: Boolean) = security.setSecureScreen(enabled)

    fun onAppForegrounded() = security.onAppStarted()
    fun onAppBackgrounded() = security.onAppStopped()

    private val includeJewelryZakat = MutableStateFlow(true)

    /** hitungan zakat emas (nisab 85 gr murni, haul 1 tahun hijriah, kadar 2,5%) */
    val zakat: StateFlow<ZakatUiState> = combine(
        items.items,
        prices.prices,
        includeJewelryZakat,
    ) { itemList, store, includeJewelry ->
        // emas yang sudah dijual tidak masuk hitungan zakat
        ZakatCalculator.compute(itemList.filterNot { it.isSold }, PriceBook(store), includeJewelry)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ZakatUiState())

    fun setIncludeJewelryInZakat(value: Boolean) {
        includeJewelryZakat.value = value
    }

    val state: StateFlow<PortfolioUiState> = combine(
        items.items,
        prices.prices,
        prices.refreshing,
        prices.error,
    ) { itemList, store, refreshing, error ->
        buildState(itemList, store, refreshing, error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PortfolioUiState())

    fun onStart() {
        viewModelScope.launch {
            prices.ensure(PriceRepository.sourcesFor(items.items.value), maxAgeMs = PriceRepository.FRESH_MS)
        }
        checkForUpdate()
    }

    fun refresh(force: Boolean = true) {
        viewModelScope.launch {
            prices.refresh(PriceRepository.sourcesFor(items.items.value), force = force)
        }
    }

    fun ensureBrand(brand: GoldBrand) {
        if (brand.sources.isEmpty()) return
        viewModelScope.launch {
            prices.ensure(PriceRepository.sourcesOf(brand), maxAgeMs = PriceRepository.FRESH_MS)
        }
    }

    fun dismissError() = prices.clearError()

    fun findItem(id: String?): GoldItem? = items.find(id)

    fun saveItem(item: GoldItem) {
        val previous = items.find(item.id)
        if (previous != null && previous.photoPath != null && previous.photoPath != item.photoPath) {
            photos.delete(previous.photoPath)
        }
        items.upsert(item)
        viewModelScope.launch {
            prices.ensure(PriceRepository.sourcesOf(GoldBrand.of(item.brandKey)), maxAgeMs = PriceRepository.FRESH_MS)
        }
    }

    fun deleteItem(id: String) {
        val existing = items.find(id)
        items.delete(id)
        photos.delete(existing?.photoPath)
    }

    fun importPhoto(uri: Uri): String? = photos.saveFromUri(uri)

    /** buang foto yang diimpor lalu batal disimpan */
    fun discardPhoto(path: String?) = photos.delete(path)

    private fun buildState(
        itemList: List<GoldItem>,
        store: PriceStore,
        refreshing: Boolean,
        error: String?,
    ): PortfolioUiState {
        val book = PriceBook(store)
        val owned = itemList.filterNot { it.isSold }

        val rows = owned.map { item ->
            val brand = GoldBrand.of(item.brandKey)
            val valuation = book.valuation(brand, item.kind, item.weightGram, item.karat)
            val profit = valuation.value?.let { it - item.purchasePrice }
            val percent = valuation.value?.let {
                if (item.purchasePrice > 0L) (it - item.purchasePrice) * 100.0 / item.purchasePrice else null
            }
            ItemRow(item, brand, valuation, profit, percent)
        }

        var weight = 0.0
        var cost = 0L
        var value = 0L
        var priced = 0
        var unpriced = 0
        rows.forEach { row ->
            weight += row.item.weightGram
            cost += row.item.purchasePrice
            val v = row.valuation.value
            if (v != null) {
                value += v
                priced++
            } else {
                unpriced++
            }
        }

        val soldRows = itemList
            .filter { it.isSold }
            .sortedByDescending { it.saleDate ?: 0L }
            .map { item ->
                val brand = GoldBrand.of(item.brandKey)
                val valuation = book.valuation(brand, item.kind, item.weightGram, item.karat)
                SoldRow(item = item, brand = brand, currentValue = valuation.value)
            }

        val soldTotals = SoldTotals(
            count = soldRows.size,
            weightGram = soldRows.sumOf { it.item.weightGram },
            cost = soldRows.sumOf { it.item.purchasePrice },
            proceeds = soldRows.sumOf { it.item.salePrice ?: 0L },
            averageHoldingMillis = soldRows
                .mapNotNull { it.item.holdingMillis }
                .takeIf { it.isNotEmpty() }
                ?.average()?.toLong() ?: 0L,
        )

        val priceDate = store.sources.values
            .map { it.recordedDate }
            .filter { it.isNotBlank() }
            .maxByOrNull { it.length }
            .orEmpty()

        return PortfolioUiState(
            rows = rows,
            totals = PortfolioTotals(weight, cost, value, priced, unpriced),
            soldRows = soldRows,
            soldTotals = soldTotals,
            refreshing = refreshing,
            error = error,
            updatedAt = store.updatedAt,
            priceDate = priceDate,
            spotPerGram = book.spot24PerGram,
            antamBuybackPerGram = book.buybackPerGram(GoldBrand.ANTAM, 1.0),
            ubsBuybackPerGram = book.buybackPerGram(GoldBrand.UBS, 1.0),
            hasPriceData = !book.isEmpty,
        )
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer {
                GoldViewModel(
                    items = container.itemRepository,
                    prices = container.priceRepository,
                    photos = container.photoStore,
                    security = container.securityManager,
                    updater = container.updater,
                    backupManager = container.backupManager,
                )
            }
        }
    }
}

data class BackupStatus(
    val working: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
)

/** state kecil untuk layar edit */
data class FormPricePreview(
    val valuation: Valuation = Valuation.Empty,
    val profit: Long? = null,
)

internal fun previewFor(store: PriceStore, brand: GoldBrand, kind: com.rizal.catatemas.data.ItemKind, weight: Double, karat: Int, purchasePrice: Long): FormPricePreview {
    val valuation = PriceBook(store).valuation(brand, kind, weight, karat)
    val profit = valuation.value?.let { it - purchasePrice }
    return FormPricePreview(valuation, profit)
}
