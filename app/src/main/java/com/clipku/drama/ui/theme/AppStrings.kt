package com.clipku.drama.ui.theme

import com.clipku.drama.data.prefs.AppSettings

/**
 * Tabel string antarmuka Indonesia / English.
 * Bahasa diganti lewat Pengaturan → aplikasi di-recreate, jadi getter biasa cukup
 * (tidak perlu collectAsState — seluruh komposisi dibangun ulang).
 * Catatan: nama genre, judul drama & sinopsis TIDAK diterjemahkan (konten).
 */
object Str {
    private val en: Boolean get() = AppSettings.appLanguage.value == "en"

    // Navigasi bawah
    val home get() = if (en) "Home" else "Beranda"
    val popular get() = if (en) "Popular" else "Populer"
    val explore get() = if (en) "Explore" else "Eksplor"
    val history get() = if (en) "History" else "Riwayat"
    val favorites get() = if (en) "Favorites" else "Favorit"

    // Search
    val search get() = if (en) "Search" else "Cari"
    val searchAllDramas get() = if (en) "Search all dramas..." else "Cari semua drama..."
    val searchHintLong get() = if (en) "Search dramas, genres, keywords..." else "Cari drama, genre, kata kunci..."
    val searchHintShort get() = if (en) "Search dramas..." else "Cari drama..."
    fun searchIn(name: String) = if (en) "Search in $name..." else "Cari di $name..."
    val typeToSearch get() = if (en) "Type to search dramas..." else "Ketik untuk mencari drama..."
    val typeKeywordPressSearch get() = if (en) "Type keywords then press Search" else "Ketik kata kunci lalu tekan Cari"
    val popularSearches get() = if (en) "Popular Searches" else "Pencarian Populer"
    val recentlySearched get() = if (en) "Recently searched" else "Terakhir dicari"
    val didYouMean get() = if (en) "Did you mean" else "Maksud Anda"
    fun noResultsFor(q: String) = if (en) "No results for \"$q\"" else "Tidak ada hasil untuk \"$q\""
    val tryOtherKeywords get() = if (en) "Try other keywords or check spelling." else "Coba kata kunci lain atau periksa ejaan."
    val notFound get() = if (en) "Not found" else "Tidak ditemukan"
    val searchCatalogFailed get() = if (en) "Failed to search catalog." else "Gagal mencari di katalog."
    val resultsFor get() = if (en) "results for" else "hasil untuk"

    // Umum
    val cancel get() = if (en) "Cancel" else "Batal"
    val close get() = if (en) "Close" else "Tutup"
    val back get() = if (en) "Back" else "Kembali"
    val delete get() = if (en) "Delete" else "Hapus"
    val deleteAll get() = if (en) "Delete all" else "Hapus semua"
    val retry get() = if (en) "Retry" else "Coba Lagi"
    val all get() = if (en) "All" else "Semua"
    val off get() = if (en) "Off" else "Mati"
    val small get() = if (en) "Small" else "Kecil"
    val normal get() = if (en) "Normal" else "Normal"
    val large get() = if (en) "Large" else "Besar"
    val untitled get() = if (en) "Untitled" else "Tanpa Judul"
    val seeMore get() = if (en) "See more" else "Lihat lebih banyak"
    val details get() = if (en) "Details" else "Detail"
    val filter get() = if (en) "Filter" else "Filter"
    val filterResults get() = if (en) "Filter Results" else "Hasil Filter"

    // Sort
    val relevant get() = if (en) "Relevant" else "Relevan"
    val topRated get() = if (en) "Top rated" else "Rating tertinggi"
    val mostWatched get() = if (en) "Most watched" else "Paling ditonton"
    val mostEpisodes get() = if (en) "Most episodes" else "Episode terbanyak"
    val titleAZ get() = if (en) "Title A–Z" else "Judul A–Z"

    // Pengaturan
    val settings get() = if (en) "Settings" else "Pengaturan"
    val theme get() = if (en) "Theme" else "Tema"
    val themeDark get() = if (en) "Dark" else "Gelap"
    val themeLight get() = if (en) "Light" else "Terang"
    val themeSystem get() = if (en) "Follow System" else "Ikuti Sistem"
    val language get() = if (en) "Language" else "Bahasa"
    val dataSaver get() = if (en) "Data saver" else "Hemat kuota"
    val dataSaverDesc get() = if (en) "Low quality on cellular data" else "Kualitas rendah saat data seluler"
    val autoNextEp get() = if (en) "Auto-play next episode" else "Putar otomatis episode berikutnya"
    val autoNextEpDesc get() = if (en) "Countdown before moving on" else "Hitung mundur sebelum pindah episode"
    val doubleTapDuration get() = if (en) "Double-tap seek duration" else "Durasi double-tap"
    val gridDensity get() = if (en) "Grid density" else "Kerapatan grid"
    val uiFontSize get() = if (en) "Interface font size" else "Ukuran font antarmuka"
    val clearImageCache get() = if (en) "Clear image cache" else "Hapus cache gambar"
    val autoClearCache get() = if (en) "Auto-clear image cache" else "Hapus cache gambar otomatis"
    val never get() = if (en) "Never" else "Tidak pernah"
    fun everyDays(d: Int) = if (en) "Every $d days" else "Tiap $d hari"
    val providers get() = if (en) "Providers" else "Provider"
    val appearance get() = if (en) "Appearance" else "Tampilan"
    val playback get() = if (en) "Playback" else "Pemutaran"
    val network get() = if (en) "Network" else "Jaringan"
    val storage get() = if (en) "Storage" else "Penyimpanan"
    val others get() = if (en) "Others" else "Lainnya"
    val searchProviders get() = if (en) "Search providers..." else "Cari provider..."
    fun providersHidden(n: Int, total: Int) = if (en) "$n of $total hidden" else "$n dari $total disembunyikan"
    val autoHiddenNote get() = if (en) "Auto-hidden (unhealthy)" else "Disembunyikan otomatis (tidak sehat)"
    val noProvidersFound get() = if (en) "No matching providers" else "Tidak ada provider yang cocok"
    val exploreProviders get() = if (en) "Explore Providers" else "Eksplor Provider"
    val hideDeadProviderNote get() = if (en) "Hidden ones are excluded from Explore & search" else "Yang disembunyikan dikecualikan dari Eksplor & pencarian"
    val watchStats get() = if (en) "Watch statistics" else "Statistik nonton"
    val thisWeek get() = if (en) "This week" else "Minggu ini"
    val thisMonth get() = if (en) "This month" else "Bulan ini"
    val totalDuration get() = if (en) "Total watch time" else "Total durasi nonton"
    val episodesWatched get() = if (en) "Episodes watched" else "Episode ditonton"
    val about get() = if (en) "About" else "Tentang"
    val version get() = if (en) "Version" else "Versi"

    // Info aplikasi
    val appInfo get() = if (en) "App Info" else "Info Aplikasi"
    val appName get() = if (en) "App name" else "Nama aplikasi"
    val packageLabel get() = if (en) "Package" else "Paket"
    val developer get() = "Developer"
    val github get() = "GitHub"
    val privacy get() = if (en) "Privacy" else "Privasi"
    val privacyNote get() = if (en)
        "Watch history, favorites, search history and settings are stored locally on this device only. Streaming content is provided by third-party providers via the data source above."
        else "Riwayat tonton, favorit, riwayat pencarian, dan pengaturan tersimpan lokal di HP ini saja. Konten streaming disediakan provider pihak ketiga melalui sumber data di atas."
    val copyright get() = "© 2026 CLIPKU+"
    val appTagline get() = if (en) "Your favorite short dramas" else "Drama pendek favoritmu"

    // Player
    val videoQuality get() = if (en) "Video quality" else "Kualitas video"
    val playbackSpeed get() = if (en) "Playback speed" else "Kecepatan putar"
    val subtitleSize get() = if (en) "Subtitle size" else "Ukuran subtitle"
    val subtitles get() = if (en) "Subtitles" else "Subtitle"
    val sleepTimer get() = if (en) "Sleep timer" else "Sleep timer"
    fun timerActive(m: Int) = if (en) "Timer active: $m min" else "Timer aktif: $m menit"
    val sleepTimerPaused get() = if (en) "Sleep timer: video paused" else "Sleep timer: video dijeda"
    fun minLabel(m: Int) = if (en) "$m min" else "$m mnt"
    val synopsis get() = if (en) "Synopsis" else "Sinopsis"
    val episode get() = if (en) "Episode" else "Episode"
    val selectEpisode get() = if (en) "Select Episode" else "Pilih Episode"
    val nextEpisode get() = if (en) "Next episode" else "Episode berikutnya"
    val prevEpisode get() = if (en) "Previous episode" else "Episode sebelumnya"
    fun nextEpIn(cur: Int, sec: Int) = if (en) "EP ${cur + 1} in $sec seconds" else "EP ${cur + 1} dalam $sec detik"
    val completedNoNext get() = if (en) "Completed — no next episode" else "Tamat — tidak ada episode berikutnya"
    val resume get() = if (en) "Resume" else "Lanjutkan"
    val keepWatching get() = if (en) "Keep watching" else "Lanjut nonton"
    fun continueEpisode(ep: Int) = if (en) "Continue Episode $ep" else "Lanjutkan Episode $ep"
    val watchEpisode1 get() = if (en) "Watch Episode 1" else "Tonton Episode 1"
    val exitVideoTitle get() = if (en) "Exit video?" else "Keluar dari video?"
    val exitVideoDesc get() = if (en) "Your progress is saved automatically." else "Progres nonton kamu sudah tersimpan otomatis."
    val yesExit get() = if (en) "Yes, exit" else "Ya, keluar"
    val play get() = if (en) "Play" else "Putar"
    val pause get() = if (en) "Pause" else "Jeda"
    val miniPlayer get() = if (en) "Mini player" else "Mini player"
    val minimize get() = if (en) "Minimize" else "Perkecil"
    val closePlayer get() = if (en) "Close player" else "Tutup pemutar"
    fun seekSec(s: Int) = if (en) "${if (s > 0) "+" else ""}$s sec" else "${if (s > 0) "+" else ""}$s detik"
    fun continueFrom(pos: String) = if (en) "Continue from $pos" else "Lanjut dari $pos"
    val watched get() = if (en) "Watched" else "Sudah ditonton"
    val checkingHistory get() = if (en) "Checking history..." else "Memeriksa riwayat..."
    val loadingStream get() = if (en) "Loading stream..." else "Memuat stream..."
    val videoFailedRetry get() = if (en) "Video failed to play. Retry or change episode." else "Video gagal diputar. Coba lagi atau ganti episode."

    // Riwayat & favorit
    val noHistoryYet get() = if (en) "No history yet" else "Belum ada riwayat"
    val noFavoritesYet get() = if (en) "No favorites yet" else "Belum ada favorit"
    val tapHeartHint get() = if (en) "Tap the heart icon on a drama page" else "Ketuk ikon hati di halaman drama"
    val clearHistory get() = if (en) "Clear history" else "Hapus riwayat"
    val removeFavorite get() = if (en) "Remove favorite" else "Hapus favorit"
    val backupHistoryFav get() = if (en) "Backup history & favorites" else "Backup riwayat & favorit"
    val backupSaved get() = if (en) "Backup saved" else "Backup tersimpan"
    val backupSaveFailed get() = if (en) "Failed to save backup" else "Gagal menyimpan backup"
    val restoreFromBackup get() = if (en) "Restore from backup" else "Pulihkan dari backup"
    fun restoredCounts(h: Int, f: Int, s: Int) =
        if (en) "Restored: $h history, $f favorites, $s searches"
        else "Dipulihkan: $h riwayat, $f favorit, $s pencarian"
    val invalidBackupFile get() = if (en) "Invalid backup file" else "File backup tidak valid"
    fun lastEpOf(provider: String, ep: Int, total: Int) =
        if (en) "$provider • Last EP $ep/$total" else "$provider • Terakhir EP $ep/$total"
    fun cleanedDeadHistory(n: Int) =
        if (en) "$n dead history removed" else "$n riwayat mati dibersihkan"
    fun skippedCount(n: Int) = if (en) " ($n skipped)" else " ($n dilewati)"

    // Beranda / eksplor
    val browseDramas get() = if (en) "Browse Dramas" else "Jelajahi Drama"
    val browse get() = if (en) "Browse" else "Jelajahi"
    val exploreFeatures get() = if (en) "Explore features →" else "Eksplor fitur →"
    fun epOf(cur: Int, total: Int) = if (en) "EP $cur of $total" else "EP $cur dari $total"
    val trending get() = if (en) "Trending" else "Trending"
    val trendingNow get() = if (en) "Trending Now" else "Trending Sekarang"
    val latest get() = if (en) "Latest" else "Terbaru"
    val ranking get() = if (en) "Ranking" else "Peringkat"
    val recommended get() = if (en) "Recommended" else "Rekomendasi"
    val collection get() = if (en) "Collection" else "Koleksi"
    val category get() = if (en) "Category" else "Kategori"
    val genre get() = if (en) "Genre" else "Genre"

    // Cast
    val castToDevice get() = if (en) "Cast to device" else "Cast ke perangkat"
    val castToTv get() = if (en) "Cast to TV" else "Cast ke TV"
    val castingToTv get() = if (en) "Casting to TV…" else "Casting ke TV…"
    val castDevices get() = if (en) "Cast devices" else "Perangkat Cast"
    val noCastDevices get() = if (en)
        "No Cast devices found.\nMake sure your TV/Chromecast is on and on the same WiFi."
        else "Tidak ada perangkat Cast ditemukan.\nPastikan TV/Chromecast menyala dan terhubung ke WiFi yang sama."

    // Format angka ringkas: 5,4 jt → 5.4M, 823 rb → 823K
    fun compactNumber(v: Long): String {
        fun trimZero(d: Double): String {
            val s = if (en) String.format("%.1f", d) else String.format("%.1f", d).replace(".", ",")
            return if (s.endsWith(if (en) ".0" else ",0")) s.dropLast(2) else s
        }
        return when {
            v >= 1_000_000 -> "${trimZero(v / 1_000_000.0)}${if (en) "M" else " jt"}"
            v >= 1_000 -> "${trimZero(v / 1_000.0)}${if (en) "K" else " rb"}"
            else -> v.toString()
        }
    }
    fun viewsWatched(v: Long) = if (en) "${compactNumber(v)} views" else "${compactNumber(v)} ditonton"
    fun episodeCount(n: Int) = if (en) "$n episodes" else "$n episode"
}
