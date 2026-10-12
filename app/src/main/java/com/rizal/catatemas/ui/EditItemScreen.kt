@file:OptIn(ExperimentalMaterial3Api::class)

package com.rizal.catatemas.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rizal.catatemas.data.GoldItem
import com.rizal.catatemas.data.ItemKind
import com.rizal.catatemas.price.GoldBrand
import com.rizal.catatemas.price.PriceStore
import com.rizal.catatemas.price.ValueBasis
import com.rizal.catatemas.ui.components.FieldLabel
import com.rizal.catatemas.ui.components.GoldBarIllustration
import com.rizal.catatemas.ui.components.InfoRow
import com.rizal.catatemas.ui.components.ProfitChip

private val KARAT_OPTIONS = listOf(
    24 to "99,9%",
    23 to "95,8%",
    22 to "91,6% (kadar 916)",
    21 to "87,5%",
    20 to "83,3%",
    19 to "79,2%",
    18 to "75,0%",
    17 to "70,8%",
    16 to "66,7%",
    15 to "62,5%",
    14 to "58,3%",
    13 to "54,2%",
    12 to "50,0%",
    10 to "41,7%",
    9 to "37,5%",
    8 to "33,3%",
)

private val WEIGHT_PRESETS = listOf(0.5, 1.0, 2.0, 3.0, 5.0, 10.0, 25.0, 50.0, 100.0)

private const val DAY_MS = 86_400_000L

@Composable
fun EditItemScreen(
    itemId: String?,
    viewModel: GoldViewModel,
    priceStore: PriceStore,
    onClose: () -> Unit,
) {
    val existing = remember(itemId) { viewModel.findItem(itemId) }
    val isEdit = existing != null

    var kind by rememberSaveable(itemId) { mutableStateOf(existing?.kind ?: ItemKind.BATANGAN) }
    var brandKey by rememberSaveable(itemId) { mutableStateOf(existing?.brandKey ?: GoldBrand.ANTAM.key) }
    var weightText by rememberSaveable(itemId) {
        mutableStateOf(existing?.let { Fmt.gramNumber(it.weightGram) } ?: "1")
    }
    var karat by rememberSaveable(itemId) { mutableStateOf(existing?.karat ?: 24) }
    var dateMillis by rememberSaveable(itemId) { mutableStateOf(existing?.purchaseDate ?: Fmt.todayStart()) }
    var dateText by rememberSaveable(itemId) {
        mutableStateOf(Fmt.dateInput(existing?.purchaseDate ?: Fmt.todayStart()))
    }
    var priceField by rememberSaveable(itemId, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(existing?.purchasePrice?.let { formatDigits(it.toString()) } ?: ""))
    }
    var note by rememberSaveable(itemId) { mutableStateOf(existing?.note ?: "") }
    var photoPath by rememberSaveable(itemId) { mutableStateOf(existing?.photoPath) }
    var pendingPhoto by remember { mutableStateOf<String?>(null) }

    // data penjualan
    var saleDate by rememberSaveable(itemId) { mutableStateOf(existing?.saleDate) }
    var salePrice by rememberSaveable(itemId) { mutableStateOf(existing?.salePrice) }
    var salePlace by rememberSaveable(itemId) { mutableStateOf(existing?.salePlace ?: "") }

    var showDatePicker by remember { mutableStateOf(false) }
    var brandExpanded by remember { mutableStateOf(false) }
    var karatExpanded by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmCancelSale by remember { mutableStateOf(false) }

    // dialog penjualan (pakai state sementara, baru disimpan setelah konfirmasi)
    var showSaleDialog by remember { mutableStateOf(false) }
    var showSaleDatePicker by remember { mutableStateOf(false) }
    var saleDialogDate by remember { mutableStateOf(Fmt.todayStart()) }
    var saleDialogDateText by remember { mutableStateOf("") }
    var saleDialogPlace by remember { mutableStateOf("") }
    var saleDialogPrice by remember { mutableStateOf(TextFieldValue("")) }

    val brand = GoldBrand.of(brandKey)
    val weight = weightText.replace(',', '.').toDoubleOrNull() ?: 0.0
    val priceDigits = priceField.text.filter { it.isDigit() }
    val purchasePrice = priceDigits.toLongOrNull() ?: 0L
    val valid = weight > 0.0 && purchasePrice > 0L && Fmt.parseDateInput(dateText) != null

    val isSold = saleDate != null && (salePrice ?: 0L) > 0L

    LaunchedEffect(brandKey) { viewModel.ensureBrand(brand) }

    val preview = remember(priceStore, brand, kind, weight, karat, purchasePrice) {
        previewFor(priceStore, brand, kind, weight, karat, purchasePrice)
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val saved = viewModel.importPhoto(uri)
            if (saved != null) {
                pendingPhoto?.let { viewModel.discardPhoto(it) }
                pendingPhoto = saved
                photoPath = saved
            }
        }
    }

    fun buildItem(): GoldItem = GoldItem(
        id = existing?.id ?: java.util.UUID.randomUUID().toString(),
        brandKey = brand.key,
        kind = kind,
        weightGram = weight,
        karat = karat,
        purchaseDate = dateMillis,
        purchasePrice = purchasePrice,
        note = note.trim(),
        photoPath = photoPath,
        createdAt = existing?.createdAt ?: System.currentTimeMillis(),
        saleDate = saleDate,
        salePrice = salePrice,
        salePlace = salePlace.trim(),
    )

    fun saveAndClose() {
        viewModel.saveItem(buildItem())
        pendingPhoto = null
        onClose()
    }

    fun openSaleDialog() {
        val start = saleDate ?: System.currentTimeMillis()
        saleDialogDate = start
        saleDialogDateText = Fmt.dateInput(start)
        saleDialogPlace = salePlace
        val suggested = salePrice ?: preview.valuation.value ?: 0L
        val text = if (suggested > 0L) formatDigits(suggested.toString()) else ""
        saleDialogPrice = TextFieldValue(text, selection = TextRange(text.length))
        showSaleDialog = true
    }

    fun cancel() {
        viewModel.discardPhoto(pendingPhoto)
        onClose()
    }

    BackHandler { cancel() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEdit) "Ubah catatan" else "Tambah emas",
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { cancel() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    if (isEdit) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Hapus")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    actionIconContentColor = MaterialTheme.colorScheme.error,
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (isSold) {
                SaleSummaryCard(
                    saleDate = saleDate,
                    salePrice = salePrice,
                    salePlace = salePlace,
                    weightGram = weight,
                    purchaseDate = dateMillis,
                    purchasePrice = purchasePrice,
                    onEdit = { openSaleDialog() },
                    onCancelSale = { confirmCancelSale = true },
                )
            }

            // ---------------------------------------------------------- jenis
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldLabel("Jenis emas")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = kind == ItemKind.BATANGAN,
                        onClick = { kind = ItemKind.BATANGAN },
                        label = { Text("Logam mulia") },
                        leadingIcon = { Text("🥇") },
                        colors = chipColors(),
                    )
                    FilterChip(
                        selected = kind == ItemKind.PERHIASAN,
                        onClick = { kind = ItemKind.PERHIASAN },
                        label = { Text("Perhiasan") },
                        leadingIcon = { Text("💍") },
                        colors = chipColors(),
                    )
                }
            }

            // ---------------------------------------------------------- merk
            ExposedDropdownMenuBox(
                expanded = brandExpanded,
                onExpandedChange = { brandExpanded = it },
            ) {
                OutlinedTextField(
                    value = brand.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Merk") },
                    leadingIcon = {
                        GoldBarIllustration(
                            emblem = brand.emblem,
                            weightText = Fmt.gram(if (weight > 0) weight else 1.0),
                            accent = Color(brand.accent),
                            modifier = Modifier.size(30.dp),
                            cornerPercent = 30,
                            showText = false,
                        )
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = brandExpanded) },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                )
                ExposedDropdownMenu(
                    expanded = brandExpanded,
                    onDismissRequest = { brandExpanded = false },
                ) {
                    GoldBrand.all.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        Modifier
                                            .size(10.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(Color(option.accent)),
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = option.label,
                                        fontWeight = if (option == brand) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            },
                            onClick = {
                                brandKey = option.key
                                brandExpanded = false
                            },
                        )
                    }
                }
            }

            // ---------------------------------------------------------- berat
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { input -> weightText = sanitizeDecimal(input) },
                    label = { Text("Berat") },
                    suffix = { Text("gr") },
                    singleLine = true,
                    isError = weightText.isNotEmpty() && weight <= 0.0,
                    supportingText = { Text("Pakai koma atau titik, mis. 1,5") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(WEIGHT_PRESETS) { preset ->
                        FilterChip(
                            selected = weightText.replace(',', '.').toDoubleOrNull() == preset,
                            onClick = { weightText = Fmt.gramNumber(preset) },
                            label = { Text(Fmt.gram(preset)) },
                            colors = chipColors(),
                        )
                    }
                }
            }

            // ---------------------------------------------------------- karat
            ExposedDropdownMenuBox(
                expanded = karatExpanded,
                onExpandedChange = { karatExpanded = it },
            ) {
                OutlinedTextField(
                    value = "$karat" + "K" + (KARAT_OPTIONS.firstOrNull { it.first == karat }?.let { " • ${it.second}" } ?: ""),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Karat / kadar") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = karatExpanded) },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    supportingText = {
                        if (kind == ItemKind.BATANGAN) Text("Logam mulia biasanya 24K (99,9%)")
                    },
                )
                ExposedDropdownMenu(
                    expanded = karatExpanded,
                    onDismissRequest = { karatExpanded = false },
                ) {
                    KARAT_OPTIONS.forEach { (value, purity) ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "${value}K • ${purity}",
                                    fontWeight = if (value == karat) FontWeight.Bold else FontWeight.Normal,
                                )
                            },
                            onClick = {
                                karat = value
                                karatExpanded = false
                            },
                        )
                    }
                }
            }

            // ---------------------------------------------------------- tanggal
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val parsedPurchaseDate = Fmt.parseDateInput(dateText)
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { raw ->
                        val formatted = Fmt.formatDateInput(raw)
                        dateText = formatted
                        Fmt.parseDateInput(formatted)?.let { dateMillis = it }
                    },
                    label = { Text("Tanggal pembelian") },
                    placeholder = { Text("dd/mm/yyyy") },
                    singleLine = true,
                    isError = dateText.isNotEmpty() && parsedPurchaseDate == null,
                    supportingText = {
                        Text(
                            if (parsedPurchaseDate == null) "Format dd/mm/yyyy, mis. 15/01/2024"
                            else Fmt.dateFull(parsedPurchaseDate),
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) { Text("📅") }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = Fmt.parseDateInput(dateText) == Fmt.todayStart(),
                        onClick = {
                            val today = Fmt.todayStart()
                            dateMillis = today
                            dateText = Fmt.dateInput(today)
                        },
                        label = { Text("Hari ini") },
                        colors = chipColors(),
                    )
                    FilterChip(
                        selected = Fmt.parseDateInput(dateText) == Fmt.todayStart() - DAY_MS,
                        onClick = {
                            val yesterday = Fmt.todayStart() - DAY_MS
                            dateMillis = yesterday
                            dateText = Fmt.dateInput(yesterday)
                        },
                        label = { Text("Kemarin") },
                        colors = chipColors(),
                    )
                }
            }

            // ---------------------------------------------------------- harga beli
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = priceField,
                    onValueChange = { input ->
                        val digits = input.text.filter { it.isDigit() }.take(13)
                        val formatted = formatDigits(digits)
                        priceField = TextFieldValue(formatted, selection = TextRange(formatted.length))
                    },
                    label = { Text("Harga beli (total)") },
                    prefix = { Text("Rp") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    supportingText = {
                        Text(
                            if (purchasePrice > 0L && weight > 0.0) {
                                "≈ ${Fmt.rupiah((purchasePrice / weight).toLong())}/gr"
                            } else {
                                "Total yang kamu bayar saat membeli"
                            },
                        )
                    },
                )
            }

            // ---------------------------------------------------------- estimasi harga
            PricePreviewCard(
                preview = preview,
                brand = brand,
                kind = kind,
                weight = weight,
                karat = karat,
                spotPerGram = priceStore.spotPerGram,
                expiryLabel = brand.sources
                    .mapNotNull { priceStore.sources[it.source]?.recordedDate?.takeIf { d -> d.isNotBlank() } }
                    .firstOrNull().orEmpty(),
            )

            // ---------------------------------------------------------- catatan
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Catatan (opsional)") },
                placeholder = { Text("mis. beli di Butik Antam, sertifikat ada") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            )

            // ---------------------------------------------------------- foto
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldLabel("Foto (opsional)")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GoldBarIllustration(
                        emblem = brand.emblem,
                        weightText = Fmt.gram(if (weight > 0) weight else 1.0),
                        accent = Color(brand.accent),
                        photoPath = photoPath,
                        modifier = Modifier.size(84.dp),
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = {
                                photoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Pilih foto")
                        }
                        if (photoPath != null) {
                            TextButton(onClick = { photoPath = null }) {
                                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Hapus foto")
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            if (!isSold && isEdit) {
                FilledTonalButton(
                    onClick = { openSaleDialog() },
                    enabled = valid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("💰 Tandai sudah terjual", fontWeight = FontWeight.SemiBold)
                }
                Text(
                    text = "Catat tanggal & harga jualnya supaya untung/rugi realisasi masuk ke riwayat penjualan.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Button(
                onClick = { saveAndClose() },
                enabled = valid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    text = if (isEdit) "Simpan perubahan" else "Simpan",
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }

    if (showDatePicker) {
        GoldDatePickerDialog(
            initialMillis = dateMillis,
            onDismiss = { showDatePicker = false },
            onPick = {
                dateMillis = it
                dateText = Fmt.dateInput(it)
                showDatePicker = false
            },
        )
    }

    if (showSaleDatePicker) {
        GoldDatePickerDialog(
            initialMillis = saleDialogDate,
            onDismiss = { showSaleDatePicker = false },
            onPick = {
                saleDialogDate = it
                saleDialogDateText = Fmt.dateInput(it)
                showSaleDatePicker = false
            },
        )
    }

    if (showSaleDialog) {
        val dialogDigits = saleDialogPrice.text.filter { it.isDigit() }
        val dialogPrice = dialogDigits.toLongOrNull() ?: 0L
        val dialogProfit = if (dialogPrice > 0L) dialogPrice - purchasePrice else null
        val dialogPercent = dialogProfit?.let { if (purchasePrice > 0L) it * 100.0 / purchasePrice else null }
        val parsedSaleDate = Fmt.parseDateInput(saleDialogDateText)

        AlertDialog(
            onDismissRequest = { showSaleDialog = false },
            title = { Text(if (isSold) "Ubah data penjualan" else "Tandai emas terjual") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // ------------------------------------------------ tanggal jual (manual / kalender)
                    OutlinedTextField(
                        value = saleDialogDateText,
                        onValueChange = { raw ->
                            val formatted = Fmt.formatDateInput(raw)
                            saleDialogDateText = formatted
                            Fmt.parseDateInput(formatted)?.let { saleDialogDate = it }
                        },
                        label = { Text("Tanggal jual") },
                        placeholder = { Text("dd/mm/yyyy") },
                        singleLine = true,
                        isError = saleDialogDateText.isNotEmpty() && parsedSaleDate == null,
                        supportingText = {
                            Text(
                                if (parsedSaleDate == null) "Format dd/mm/yyyy, mis. 12/10/2026"
                                else "Ketik manual atau pilih dari kalender",
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        trailingIcon = {
                            IconButton(onClick = { showSaleDatePicker = true }) { Text("📅") }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = Fmt.parseDateInput(saleDialogDateText) == Fmt.todayStart(),
                            onClick = {
                                val today = Fmt.todayStart()
                                saleDialogDate = today
                                saleDialogDateText = Fmt.dateInput(today)
                            },
                            label = { Text("Hari ini") },
                            colors = chipColors(),
                        )
                        FilterChip(
                            selected = Fmt.parseDateInput(saleDialogDateText) == Fmt.todayStart() - DAY_MS,
                            onClick = {
                                val yesterday = Fmt.todayStart() - DAY_MS
                                saleDialogDate = yesterday
                                saleDialogDateText = Fmt.dateInput(yesterday)
                            },
                            label = { Text("Kemarin") },
                            colors = chipColors(),
                        )
                    }

                    // ------------------------------------------------ harga jual (manual)
                    OutlinedTextField(
                        value = saleDialogPrice,
                        onValueChange = { input ->
                            val digits = input.text.filter { it.isDigit() }.take(13)
                            val formatted = formatDigits(digits)
                            saleDialogPrice = TextFieldValue(formatted, selection = TextRange(formatted.length))
                        },
                        label = { Text("Harga jual (total diterima)") },
                        prefix = { Text("Rp") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        supportingText = {
                            if (dialogPrice > 0L && weight > 0.0) {
                                Text("≈ ${Fmt.rupiah((dialogPrice / weight).toLong())}/gr")
                            } else {
                                Text("Masukkan total uang yang kamu terima")
                            }
                        },
                    )

                    val marketValue = preview.valuation.value
                    if (marketValue != null && marketValue > 0L) {
                        TextButton(
                            onClick = {
                                val text = formatDigits(marketValue.toString())
                                saleDialogPrice = TextFieldValue(text, selection = TextRange(text.length))
                            },
                        ) {
                            Text("Pakai harga pasar kini (${Fmt.rupiah(marketValue)})")
                        }
                    }

                    OutlinedTextField(
                        value = saleDialogPlace,
                        onValueChange = { saleDialogPlace = it },
                        label = { Text("Dijual ke (opsional)") },
                        placeholder = { Text("mis. Butik Antam, toko emas") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                    )

                    if (dialogProfit != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Modal ${Fmt.rupiah(purchasePrice)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(10.dp))
                            ProfitChip(profit = dialogProfit, percent = dialogPercent, compact = true)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val picked = Fmt.parseDateInput(saleDialogDateText) ?: return@TextButton
                        saleDate = picked
                        salePrice = dialogPrice
                        salePlace = saleDialogPlace.trim()
                        showSaleDialog = false
                        saveAndClose()
                    },
                    enabled = dialogPrice > 0L && weight > 0.0 && parsedSaleDate != null,
                ) { Text("Simpan penjualan") }
            },
            dismissButton = {
                TextButton(onClick = { showSaleDialog = false }) { Text("Batal") }
            },
        )
    }

    if (confirmCancelSale && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmCancelSale = false },
            title = { Text("Batalkan penjualan?") },
            text = { Text("Catatan ini akan kembali ke daftar emas yang dimiliki dan riwayat penjualannya dihapus.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmCancelSale = false
                    saleDate = null
                    salePrice = null
                    salePlace = ""
                    saveAndClose()
                }) { Text("Batalkan penjualan", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmCancelSale = false }) { Text("Tutup") }
            },
        )
    }

    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Hapus catatan ini?") },
            text = { Text("${brand.label} ${Fmt.gram(existing.weightGram)} akan dihapus permanen.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.deleteItem(existing.id)
                    onClose()
                }) { Text("Hapus", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Batal") }
            },
        )
    }
}

@Composable
private fun GoldDatePickerDialog(
    initialMillis: Long,
    onDismiss: () -> Unit,
    onPick: (Long) -> Unit,
) {
    val dateState = rememberDatePickerState(initialSelectedDateMillis = Fmt.toPickerMillis(initialMillis))
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                dateState.selectedDateMillis?.let { onPick(Fmt.fromPickerMillis(it)) } ?: onDismiss()
            }) { Text("Pilih") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        },
    ) {
        DatePicker(state = dateState)
    }
}

@Composable
private fun SaleSummaryCard(
    saleDate: Long?,
    salePrice: Long?,
    salePlace: String,
    weightGram: Double,
    purchaseDate: Long,
    purchasePrice: Long,
    onEdit: () -> Unit,
    onCancelSale: () -> Unit,
) {
    val profit = salePrice?.let { it - purchasePrice }
    val percent = profit?.let { if (purchasePrice > 0L) it * 100.0 / purchasePrice else null }
    val perGram = salePrice?.let { if (weightGram > 0.0) (it / weightGram).toLong() else null }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Sudah terjual",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Spacer(Modifier.width(10.dp))
                ProfitChip(profit = profit, percent = percent, compact = true)
            }

            HorizontalDivider()

            InfoRow("Tanggal jual", saleDate?.let { Fmt.dateFull(it) } ?: "-")
            InfoRow("Harga jual", salePrice?.let { Fmt.rupiah(it) } ?: "-")
            perGram?.let { InfoRow("Per gram", "${Fmt.rupiah(it)}/gr") }
            if (saleDate != null) InfoRow("Lama disimpan", Fmt.duration(purchaseDate, saleDate))
            if (salePlace.isNotBlank()) InfoRow("Dijual ke", salePlace)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEdit, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) {
                    Text("Ubah data jual", maxLines = 1)
                }
                TextButton(onClick = onCancelSale, modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Batalkan jual",
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun PricePreviewCard(
    preview: FormPricePreview,
    brand: GoldBrand,
    kind: ItemKind,
    weight: Double,
    karat: Int,
    spotPerGram: Long?,
    expiryLabel: String,
) {
    val valuation = preview.valuation

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = when (valuation.basis) {
                    ValueBasis.BUYBACK_MERK -> "Estimasi nilai jual kembali"
                    ValueBasis.LEBUR -> "Estimasi nilai lebur"
                    ValueBasis.TIDAK_ADA -> "Harga belum tersedia"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )

            if (valuation.value != null) {
                Text(
                    text = Fmt.rupiah(valuation.value),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = buildString {
                        append(valuation.perGram?.let { "${Fmt.rupiah(it)}/gr" }.orEmpty())
                        append(" • ")
                        append(
                            when (valuation.basis) {
                                ValueBasis.BUYBACK_MERK -> "buyback ${valuation.sourceLabel ?: brand.label} ${Fmt.gram(weight)}"
                                else -> "acuan 24K ${spotPerGram?.let { Fmt.rupiah(it) } ?: "-"}/gr × kadar $karat/24"
                            },
                        )
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                )
                if (valuation.brandSellTotal != null && kind == ItemKind.BATANGAN) {
                    Text(
                        text = "Harga jual baru merk: ${Fmt.rupiah(valuation.brandSellTotal)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Potensi " + if ((preview.profit ?: 0L) >= 0L) "untung" else "rugi",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                    )
                    Spacer(Modifier.width(8.dp))
                    ProfitChip(profit = preview.profit, percent = null, compact = true)
                }
            } else {
                Text(
                    text = if (brand.sources.isEmpty()) {
                        "Merk ini memakai acuan harga 24K. Tekan \"Perbarui\" di beranda setelah tersambung internet."
                    } else {
                        "Data harga ${brand.label} belum ada di perangkat. Connect ke internet lalu tekan \"Perbarui\" di beranda."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            if (expiryLabel.isNotBlank()) {
                Text(
                    text = "Data harga per $expiryLabel",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
)

/** hanya angka + satu pemisah desimal */
private fun sanitizeDecimal(input: String): String {
    val filtered = input.filter { it.isDigit() || it == ',' || it == '.' }
    val firstSeparator = filtered.indexOfFirst { it == ',' || it == '.' }
    if (firstSeparator < 0) return filtered.take(9)
    val head = filtered.substring(0, firstSeparator + 1)
    val tail = filtered.substring(firstSeparator + 1).filter { it.isDigit() }
    return (head + tail).take(9)
}

private fun formatDigits(digits: String): String {
    if (digits.isEmpty()) return ""
    val reversed = digits.reversed()
    val grouped = reversed.chunked(3).joinToString(".")
    return grouped.reversed()
}
