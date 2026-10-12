# Catat Emas

Aplikasi Android (Jetpack Compose) untuk mencatat kepemilikan **logam mulia & perhiasan emas**, memantau **harga terkini per merk** secara otomatis, dan menghitung **untung/rugi**, **riwayat penjualan**, serta **zakat emas**.

- APK rilis (minified R8): `app/build/outputs/apk/release/app-release.apk` (~1,4 MB)
- Package: `com.rizal.catatemas`
- minSdk 26 (Android 8.0) · targetSdk/compileSdk 36 · Kotlin 2.2.20 · Compose (BOM 2025.11.01, Material 3)

## Fitur

| Fitur | Keterangan |
|---|---|
| Catat emas | Merk, jenis (logam mulia / perhiasan), berat (gr), karat, tanggal beli, harga beli, catatan, foto |
| Harga otomatis | Harga selalu diperbarui sesuai **merk** yang diinput (Antam, UBS, Galeri 24, Pegadaian, IndoGold, Emasku, Bank BSI, Sampoerna Gold, Lotus Archi, Brankas LM, Laku Emas, Treasury, Aneka Logam, Emas Digital, dll) |
| Header ringkasan | Total emas (gram), estimasi nilai terkini, modal, untung/rugi (Rp + %) |
| Kartu item | Ilustrasi batang emas + emblem merk & berat, harga beli vs nilai kini, untung/rugi, harga per gram |
| Ubah item | Semua field bisa diedit, ada hapus item (dengan konfirmasi) |
| **Emas sudah dijual** | Tandai terjual: input **tanggal jual & harga jual manual** (atau pakai kalender / tombol "Pakai harga pasar kini"), simpan tempat jual |
| **Riwayat penjualan** | Tab "Riwayat jual": total hasil penjualan, modal, untung/rugi **realisasi**, per gram saat jual, pergerakan harga setelah dijual, lama disimpan (mis. "disimpan 9 bulan") |
| **Zakat emas** | Nisab 85 gr murni 24K, haul 1 tahun hijriah (354 hari), kadar 2,5%. Lengkap dengan status (wajib / tunggu haul / belum nisab), progress nisab, rincian hitungan, hasil dalam rupiah **dan** gram emas, sakelar "hitung perhiasan juga" |
| Cari, filter, urutkan | Cari merk/karat/catatan, filter jenis, urutkan (terbaru, berat, untung, modal) |
| **Keamanan** | Kunci aplikasi pakai **PIN 4-6 angka** dan/atau **biometrik** (sidik jari/wajah), plus **cegah screenshot & rekam layar** (FLAG_SECURE). Auto-lock setelah 30 detik di background, tombol "Kunci sekarang", bisa ubah PIN |
| **Pembaruan otomatis** | Cek versi baru dari **GitHub Releases** (tanpa REST API, jadi bebas rate limit), unduh progress, lalu pasang lewat FileProvider. Ada tombol "Cek pembaruan" + "Nanti" (skip versi) |
| Offline-first | Semua data di perangkat (`filesDir/items.json`), harga disimpan cache → tetap kebaca tanpa internet |

## Keamanan

Buka ikon 🔒 di beranda → layar **Keamanan**.

- **Kunci aplikasi (PIN)** — PIN 4-6 angka, diminta setiap aplikasi dibuka. Disimpan sebagai hash `PBKDF2-HMAC-SHA256` (100.000 iterasi) + salt acak di SharedPreferences privat aplikasi — bukan teks biasa, dan tidak pernah dikirim ke server.
- **Biometrik** — memakai `BiometricPrompt` bawaan platform (tanpa dependency tambahan, otomatis tampil saat layar kunci terbuka bila diaktifkan). PIN tetap jadi cadangan. Butuh Android 9+ dan biometrik yang sudah terdaftar di perangkat.
- **Cegah screenshot & rekam layar** — `FLAG_SECURE` di window: screenshot/rekaman jadi hitam dan aplikasi tidak muncul di pratinjau "aplikasi terbaru". Default **aktif**.
- **Auto-lock** — setelah 30 detik di background (jendela 30 detik ini supaya pemilih foto tidak memicu kunci). Layar kunci dibuat sebagai *overlay*, jadi isian form yang sedang dibuka tidak hilang.
- **PIN lupa** → satu-satunya jalan adalah hapus data aplikasi dari pengaturan Android (catatan emas ikut hilang).


## Sumber harga

Memakai API publik & gratis: `https://logam-mulia-api.iamutaki.workers.dev` (hasil pemantauan situs resmi).

Cara hitung nilai:

- **Logam mulia (batangan)** → harga **buyback resmi** merk untuk berat tersebut.
  Berat yang tidak ada di daftar resmi (mis. 1,5 gr) diinterpolasi dari berat terdekat (per gram).
- **Perhiasan** → estimasi **nilai lebur** = harga acuan 24K × (karat ÷ 24).
- Kalau merk tidak menyediakan buyback → nilai lebur dipakai sebagai gantinya.
- Harga acuan 24K = `hargaemas.com` (Emas Spot), dengan cadangan dari harga jual Antam bila sumber utama gagal.
- Harga di-refresh otomatis saat aplikasi dibuka (kalau data > 30 menit) dan tombol ⟳ untuk refresh manual.

Pemetaan merk → sumber ada di `price/GoldBrand.kt`. Nilai yang ditampilkan adalah **estimasi**, bukan penawaran resmi pembeli.

## Aturan zakat yang dipakai

- Semua berat dikonversi ke emas murni 24K: `gram × (karat ÷ 24)`.
- Nisab **85 gram** emas murni (24K).
- Haul **1 tahun hijriah (354 hari)**, dihitung dari tanggal pembelian tertua pada emas yang masuk hitungan (perkiraan konservatif).
- Kadar **2,5%** (1/40), boleh dibayar dengan emas atau uang senilai.
- Emas yang sudah ditandai **terjual tidak dihitung**.
- Sakelar "hitung perhiasan juga": ON = semua emas masuk hitungan (pendapat fatwa MUI / mazhab Hanafi), OFF = hanya logam mulia (pendapat jumhur ulama untuk perhiasan yang dipakai).

Fitur ini alat bantu hitung, bukan fatwa.

## Struktur kode

```
app/src/main/java/com/rizal/catatemas/
├── CatatEmasApp.kt            # Application + AppContainer (manual DI, tanpa Hilt)
├── MainActivity.kt
├── data/
│   ├── GoldItem.kt            # model + field penjualan (saleDate/salePrice/salePlace)
│   ├── ItemRepository.kt      # StateFlow daftar item
│   ├── JsonStore.kt           # penyimpanan JSON atomic (tanpa Room/annotation processor)
│   └── PhotoStore.kt          # simpan foto dari photo picker ke storage internal
├── security/
│   └── SecurityManager.kt     # PIN (PBKDF2), biometrik, FLAG_SECURE, auto-lock
├── update/
│   └── Updater.kt             # cek versi + unduh & pasang APK dari GitHub Releases
├── price/
│   ├── GoldBrand.kt           # daftar merk + pemetaan sumber harga
│   ├── GoldApi.kt             # OkHttp + kotlinx.serialization
│   ├── PriceBook.kt           # harga buyback/lebur per item, interpolasi berat
│   ├── PriceModels.kt
│   └── PriceRepository.kt     # refresh paralel, cache, offline
└── ui/
    ├── CatatEmasApp.kt        # navigasi ringan (tanpa navigation-compose)
    ├── HomeScreen.kt          # tab Dimiliki / Riwayat jual, header, kartu item
    ├── EditItemScreen.kt      # form + dialog penjualan + date picker
    ├── ZakatScreen.kt         # status & rincian zakat
    ├── ZakatCalculator.kt     # logika zakat (murni, mudah diuji)
    ├── SecurityScreen.kt      # pengaturan keamanan + dialog PIN
    ├── LockScreen.kt          # overlay layar kunci (PIN + biometrik)
    ├── GoldViewModel.kt       # state gabungan item + harga
    ├── Format.kt              # format rupiah/gram/tanggal/lama simpan
    ├── components/            # ilustrasi batang emas, chip untung/rugi, info sheet
    └── theme/
```

## Build

```bash
./gradlew :app:assembleDebug     # debug
./gradlew :app:assembleRelease   # rilis, R8 + resource shrinking
./gradlew :app:installRelease
```

atau lewat Makefile:

```bash
make build            # debug APK
make release          # release APK (R8, ~1,4 MB)
make install          # install debug via adb
make install-release  # install release via adb
make ship             # bump patch + build + commit + push + tag v*  (BUMP=minor|major|x.y.z)
make tag              # tag versi yang ada di build.gradle.kts lalu push
make clean
```

APK rilis tanpa `keystore.properties` ditandatangani **debug keystore** supaya tetap bisa di-install.
Untuk rilis asli, buat `keystore.properties` di root project:

```properties
storeFile=keystore.jks
storePassword=...
keyAlias=...
keyPassword=...
```

(`keystore.jks` & `keystore.properties` sudah masuk `.gitignore`.)

## Rilis & pembaruan otomatis

**1. Set repo GitHub** (dipakai fitur cek update) — samakan dengan repo aslimu:

```properties
# gradle.properties
github.repo=owner/catat-emas
```

Nilai ini masuk ke `BuildConfig.GITHUB_REPO`. Di CI tidak perlu diubah: workflow mengirim
`-Pgithub.repo=$GITHUB_REPOSITORY` otomatis.

**2. Secrets CI** (Settings → Secrets and variables → Actions), opsional — kalau tidak diisi,
APK rilis tetap dibuild dengan debug key:

| Secret | Isi |
|---|---|
| `ANDROID_SIGNING_KEY_BASE64` | `base64 -i keystore.jks \| pbcopy` |
| `ANDROID_SIGNING_STORE_PASSWORD` | password keystore |
| `ANDROID_SIGNING_KEY_ALIAS` | alias key |
| `ANDROID_SIGNING_KEY_PASSWORD` | password key |

**3. Rilis** — `./scripts/ship.sh` (atau `make ship`): bump `versionCode`/`versionName`, build
release lokal, commit `Release vX.Y.Z (versionCode N)`, push `main`, tag `vX.Y.Z`, push tag.
Workflow `.github/workflows/build.yml` lalu jalan (JDK 17 → `sdkmanager` platform 36 →
`assembleRelease` → upload artifact → `softprops/action-gh-release` membuat GitHub Release
dengan aset `app-release.apk`).

**4. Cek update di app** — `update/Updater.kt` sengaja **tidak** memakai GitHub REST API
(biar tidak kena rate limit):

- Versi terbaru dibaca dari redirect `GET https://github.com/<repo>/releases/latest` →
  URL akhirnya `/releases/tag/v<versi>`.
- Perbandingan versi numerik (`isNewer`), artinya `1.0.10 > 1.0.9`.
- APK diunduh dari URL tetap `.../releases/latest/download/app-release.apk` (progress %), lalu
  dipasang via `FileProvider` (`REQUEST_INSTALL_PACKAGES`, `res/xml/file_paths.xml`).
- Muncul dialog "Ada pembaruan" saat app dibuka; "Nanti" menyimpan versi yang di-skip.
  Cek manual: ikon ℹ di beranda → **Cek pembaruan**.


Optimasi ukuran yang dipakai (hasil ±1,4 MB):

- `isMinifyEnabled = true`, `isShrinkResources = true`, R8 full mode
- `androidResources.localeFilters` hanya `id`/`in`/`en`, `ui-tooling` cuma di debug
- tanpa Room/Hilt/navigation-compose (persistensi JSON + manual DI)
- proguard rules untuk kotlinx.serialization & OkHttp di `app/proguard-rules.pro`
