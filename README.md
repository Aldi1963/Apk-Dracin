# CLIPKU+

Aplikasi streaming drama Android — nonton drama pendek (short drama) dari 40 provider dalam satu aplikasi.

## Fitur

- **Navigasi Beranda / Populer / Eksplor / Riwayat** dengan bottom bar model kapsul
- **Pencarian full-screen** ala aplikasi drama: pencarian populer 1–10, riwayat pencarian, koreksi typo ("Maksud Anda"), voice search, saran, filter genre & provider, sort episode terbanyak / judul A–Z
- **Player lengkap**: streaming HLS/DASH, lanjutkan otomatis dari posisi terakhir, auto-putar episode berikutnya (bisa dimatikan), double-tap mundur/maju (5/10/15/30 detik, bisa diatur), geser kiri-kanan untuk brightness/volume, kecepatan 0.5x–2x, sleep timer, Picture-in-Picture, kunci orientasi layar per judul
- **Mini player ala YouTube** — tekan back saat nonton, video menyusut jadi bar kecil di atas navigasi dan tetap jalan sambil kamu browsing/cari judul lain; tap bar untuk kembali ke full player
- **Chromecast** — putar di TV
- **Favorit** + notifikasi episode baru tiap 6 jam
- **Riwayat tonton lokal**: progres per episode, tanda centang episode yang sudah ditonton, bersih-bersih otomatis untuk konten yang sudah mati, backup & restore via JSON
- **Statistik nonton**: total durasi dan episode per minggu/bulan
- **Pengaturan**: tema Gelap/Terang/Ikuti Sistem, bahasa Indonesia/English, ukuran font UI, kerapatan grid 2/3 kolom, mode hemat kuota, sembunyikan provider manual, pembersihan cache gambar otomatis
- **Monitoring provider otomatis**: provider yang error beruntun disembunyikan sementara
- **Splash screen** + menu Info Aplikasi

## Teknologi

- Kotlin + Jetpack Compose (Material 3)
- Media3 ExoPlayer — pemutar HLS/DASH
- Coil (gambar), Retrofit + OkHttp + Gson (network)
- WorkManager (cek episode baru di background)
- Sumber data: [SonzaiX API](https://api.sonzaix.codes/)

## Build sendiri

Butuh JDK 21 dan Android SDK dengan build-tools 36.0.0.

```bash
export JAVA_HOME=/path/ke/jdk-21
export ANDROID_HOME=/path/ke/Android/Sdk
gradle :app:assembleRelease
```

APK release yang dihasilkan belum di-sign — sign manual sebelum install:

```bash
apksigner sign --ks debug.keystore --ks-key-alias androiddebugkey --out app-release.apk app-release-unsigned.apk
```

## Unduh APK

APK siap install tersedia di halaman [Releases](https://github.com/Aldi1963/Apk-Dracin/releases) dan di folder `releases/`.

## Developer

Aldi Irawan
