# CLIPKU+

[![GitHub release](https://img.shields.io/github/v/release/Aldi1963/Apk-Dracin)](https://github.com/Aldi1963/Apk-Dracin/releases)
[![License](https://img.shields.io/github/license/Aldi1963/Apk-Dracin)](LICENSE)
![Platform](https://img.shields.io/badge/platform-Android-3DDC84)

Aplikasi streaming drama pendek (short drama) untuk Android — 40 provider dalam satu aplikasi.

## Fitur

### Penemuan konten
- Navigasi Beranda / Populer / Eksplor / Riwayat dengan bottom bar model kapsul
- Pencarian full-screen: pencarian populer 1–10, riwayat pencarian, koreksi typo ("Maksud Anda"), voice search, saran pencarian, filter genre & provider, sort episode terbanyak / judul A–Z
- Splash screen saat aplikasi dibuka

### Pemutar video
- Streaming HLS/DASH via Media3 ExoPlayer
- **Mini player ala YouTube** — tekan back saat menonton, video menyusut menjadi bar kecil di atas navigasi dan tetap berjalan sambil kamu browsing atau mencari judul lain; tap bar untuk kembali ke full player
- Lanjutkan otomatis dari posisi terakhir menonton
- Auto-putar episode berikutnya (dapat dimatikan)
- Double-tap mundur/maju 10 detik (dapat diatur: 5/10/15/30 detik)
- Geser sisi layar untuk brightness/volume
- Kecepatan putar 0.5x–2x, sleep timer, Picture-in-Picture, kunci orientasi layar per judul
- Chromecast — putar di TV

### Personalisasi
- Favorit + notifikasi episode baru (pengecekan background berkala)
- Riwayat tonton lokal: progres per episode, tanda centang episode yang sudah ditonton, pembersihan otomatis untuk konten yang sudah mati, backup & restore via JSON
- Statistik menonton: total durasi dan episode per minggu/bulan

### Pengaturan
- Tema Gelap / Terang / Ikuti Sistem
- Bahasa Indonesia / English
- Ukuran font antarmuka, kerapatan grid 2/3 kolom
- Mode hemat kuota, sembunyikan provider secara manual, pembersihan cache gambar otomatis
- Menu Info Aplikasi

### Sistem
- Monitoring provider otomatis: provider yang error beruntun disembunyikan sementara dan muncul lagi saat pulih

## Teknologi

| Komponen | Teknologi |
| --- | --- |
| Bahasa & UI | Kotlin, Jetpack Compose (Material 3) |
| Pemutar video | Media3 ExoPlayer (HLS/DASH) |
| Gambar | Coil |
| Jaringan | Retrofit, OkHttp, Gson |
| Background task | WorkManager |
| Sumber data | [SonzaiX API](https://api.sonzaix.codes/) |

## Unduh

APK siap install tersedia di halaman [Releases](https://github.com/Aldi1963/Apk-Dracin/releases) (sebagai asset rilis maupun di folder `releases/`). Paket Maven juga tersedia di [GitHub Packages](https://github.com/Aldi1963/Apk-Dracin/packages).

## Build dari source

Kebutuhan: JDK 21, Android SDK dengan build-tools 36.0.0.

```bash
export JAVA_HOME=/path/ke/jdk-21
export ANDROID_HOME=/path/ke/Android/Sdk
gradle :app:assembleRelease
```

APK release yang dihasilkan belum di-sign — sign manual sebelum install:

```bash
apksigner sign --ks debug.keystore --ks-key-alias androiddebugkey \
  --out app-release.apk app-release-unsigned.apk
```

## Struktur proyek

```
app/src/main/java/com/clipku/drama/
├── MainActivity.kt          # entry point & scaffold utama
├── ClipkuApp.kt             # application class
├── cast/                    # Chromecast
├── data/
│   ├── sonzaix/             # data layer (SonzaiX API)
│   ├── history/            # riwayat tonton lokal
│   ├── prefs/              # pengaturan pengguna
│   ├── notify/             # notifikasi episode baru
│   ├── health/             # monitoring provider
│   └── model/              # model data
└── ui/
    ├── splash/             # splash screen
    ├── home/               # beranda
    ├── explore/            # eksplor & pencarian
    ├── detail/             # detail drama
    ├── player/             # player + mini player
    ├── history/            # riwayat
    ├── settings/           # pengaturan & info aplikasi
    ├── navigation/         # navigasi
    ├── components/         # komponen UI bersama
    └── theme/              # tema, warna, string
```

## Lisensi

Proyek ini dilisensikan di bawah [MIT License](LICENSE).

## Developer

**Aldi Irawan** — [github.com/Aldi1963](https://github.com/Aldi1963)
