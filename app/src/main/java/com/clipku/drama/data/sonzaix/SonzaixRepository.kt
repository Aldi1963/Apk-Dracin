package com.clipku.drama.data.sonzaix

import com.clipku.drama.data.model.ContentsResponse
import com.clipku.drama.data.model.DramaDetail
import com.clipku.drama.data.model.DramaItem
import com.clipku.drama.data.model.Episode
import com.clipku.drama.data.model.Provider
import com.clipku.drama.data.model.ProviderFeature
import com.clipku.drama.data.model.ProviderFeaturesResponse
import com.clipku.drama.data.model.StreamResponse
import com.clipku.drama.data.model.VideoQuality
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Repository utama: mengambil data langsung dari https://api.sonzaix.codes/
 * dan memetakan ke model UI aplikasi yang sudah ada.
 *
 * ID komposit: "{providerSlug}:{sonzaixId}" — dipakai sebagai id & slug di UI.
 */
object SonzaixRepository {
    // provider disembunyikan: otomatis (monitor v55) ATAU manual (pengaturan)
    private fun isProviderGone(slug: String): Boolean =
        com.clipku.drama.data.health.ProviderHealth.isHidden(slug) ||
            com.clipku.drama.data.prefs.AppSettings.isProviderHiddenManual(slug)
    // cache memori sederhana: list per (slug|page), detail per id, TTL 5 menit
    private data class CacheEntry(val at: Long, val data: Any)
    private val memCache = java.util.concurrent.ConcurrentHashMap<String, CacheEntry>()
    private const val CACHE_TTL = 5 * 60 * 1000L

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T> cached(key: String, loader: suspend () -> T): T {
        val now = System.currentTimeMillis()
        (memCache[key]?.takeIf { now - it.at < CACHE_TTL }?.data as? T)?.let { return it }
        val v = loader()
        memCache[key] = CacheEntry(now, v as Any)
        if (memCache.size > 200) memCache.keys.take(50).forEach { memCache.remove(it) }
        return v
    }

    fun clearCache() = memCache.clear()

    // ---------- Enrichment rating/views dari web backend ----------
    // Web drama.clipku.com punya rating & providerViewCount per judul (tidak ada di API SonzaiX).
    // Dicocokkan via judul yang dinormalisasi; cache 30 menit per provider.
    private val webMetaCache = java.util.concurrent.ConcurrentHashMap<String, Pair<Long, Map<String, Pair<Double, Long>>>>()
    private const val WEB_META_TTL = 30 * 60 * 1000L
    private const val WEB_BASE = "https://drama.clipku.com"

    private fun normalizeTitle(t: String): String =
        t.lowercase().filter { it.isLetterOrDigit() }

    private suspend fun webMetaMap(providerSlug: String): Map<String, Pair<Double, Long>> {
        val now = System.currentTimeMillis()
        webMetaCache[providerSlug]?.let { (at, map) ->
            if (now - at < WEB_META_TTL) return map
        }
        val map = mutableMapOf<String, Pair<Double, Long>>()
        try {
            kotlinx.coroutines.withTimeoutOrNull(8000) {
                val body = SonzaixClient.api.get(
                    "$WEB_BASE/api/contents",
                    mapOf("provider" to providerSlug, "page" to "1", "limit" to "100")
                )
                val arr = com.google.gson.JsonParser.parseString(body.string().trim()).asJsonArray
                for (el in arr) {
                    if (!el.isJsonObject) continue
                    val o = el.asJsonObject
                    val title = o.get("title")?.asString ?: continue
                    val rating = try { o.get("rating")?.asDouble ?: 0.0 } catch (_: Exception) { 0.0 }
                    val views = try { o.get("providerViewCount")?.asLong ?: 0L } catch (_: Exception) { 0L }
                    if (rating > 0 || views > 0) {
                        map[normalizeTitle(title)] = rating to views
                    }
                }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
        }
        webMetaCache[providerSlug] = now to map
        return map
    }

    /** Lengkapi rating & views satu detail dari web backend (cocok judul). */
    suspend fun enrichDetailWithWebMeta(detail: DramaDetail): DramaDetail {
        if (detail.rating > 0) return detail
        return try {
            val map = webMetaMap(detail.providerSlug.ifBlank { detail.id.substringBefore(":", "") })
            val meta = map[normalizeTitle(detail.title)] ?: return detail
            detail.copy(
                rating = if (detail.rating <= 0) meta.first else detail.rating,
                viewCount = if (detail.viewCount <= 0) meta.second else detail.viewCount
            )
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            detail
        }
    }

    /** Lengkapi rating & views item dari web backend (cocok judul). Aman dipanggil berulang. */
    suspend fun enrichWithWebMeta(items: List<DramaItem>): List<DramaItem> {
        val need = items.filter { it.rating <= 0 }
        if (need.isEmpty()) return items
        return try {
            coroutineScope {
                val slugs = need.mapNotNull { it.id.substringBefore(":", "").ifBlank { null } }.distinct()
                val maps = slugs.map { slug ->
                    async(kotlinx.coroutines.Dispatchers.IO) {
                        try { slug to webMetaMap(slug) }
                        catch (e: Exception) {
                            if (e is kotlinx.coroutines.CancellationException) throw e
                            slug to emptyMap<String, Pair<Double, Long>>()
                        }
                    }
                }.awaitAll().toMap()
                val enriched = need.associate { item ->
                    val slug = item.id.substringBefore(":", "")
                    val meta = maps[slug]?.get(normalizeTitle(item.title))
                    val out = if (meta != null) {
                        item.copy(
                            rating = if (item.rating <= 0) meta.first else item.rating,
                            viewCount = if (item.viewCount <= 0) meta.second else item.viewCount,
                            providerViewCount = if (item.providerViewCount <= 0) meta.second else item.providerViewCount
                        )
                    } else item
                    item.id to out
                }
                items.map { enriched[it.id] ?: it }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            items
        }
    }

    // ---------- helpers parsing fleksibel ----------

    private fun JsonObject.str(vararg keys: String): String? {
        for (k in keys) {
            val e = get(k) ?: continue
            if (e.isJsonPrimitive) {
                val s = e.asString.trim()
                if (s.isNotEmpty() && s != "null") return s
            }
        }
        return null
    }

    /** Genre asli provider: melolo category_info[].Name, drakorid category "A,B,C". */
    private fun parseGenres(o: JsonObject): List<String> {
        val out = linkedSetOf<String>()
        try {
            // melolo: category_info = JSON array string atau array langsung
            val ci = o.get("category_info")
            val arr: JsonArray? = when {
                ci == null || ci.isJsonNull -> null
                ci.isJsonArray -> ci.asJsonArray
                ci.isJsonPrimitive -> try {
                    JsonParser.parseString(ci.asString).asJsonArray
                } catch (_: Exception) { null }
                else -> null
            }
            arr?.forEach { el ->
                if (el.isJsonObject) {
                    el.asJsonObject.get("Name")?.asString?.trim()?.takeIf { it.isNotEmpty() }?.let { out.add(it) }
                }
            }
        } catch (_: Exception) {}
        try {
            // drakorid: category = "2026,Drama China,Historical,Romance"
            o.get("category")?.asString?.split(",")?.forEach { g ->
                val t = g.trim()
                // buang tahun & label tipe generik
                if (t.isNotEmpty() && !t[0].isDigit() && !t.equals("Drama China", true)
                    && !t.equals("Drama", true) && !t.equals("Short Drama", true)) {
                    out.add(t)
                }
            }
        } catch (_: Exception) {}
        try {
            // cashdrama: tags = ["Sistem", "Penyamar", ...]
            o.get("tags")?.asJsonArray?.forEach { el ->
                el.asString?.trim()?.takeIf { it.isNotEmpty() }?.let { out.add(it) }
            }
        } catch (_: Exception) {}
        return out.take(5)
    }

    /** "5.4M" -> 5400000, "823K" -> 823000 */
    private fun parseCompactNumber(s: String): Long {
        val t = s.trim().uppercase()
        return try {
            when {
                t.endsWith("M") -> (t.dropLast(1).toDouble() * 1_000_000).toLong()
                t.endsWith("K") -> (t.dropLast(1).toDouble() * 1_000).toLong()
                t.endsWith("B") -> (t.dropLast(1).toDouble() * 1_000_000_000).toLong()
                else -> t.filter { it.isDigit() }.toLongOrNull() ?: 0L
            }
        } catch (_: Exception) { 0L }
    }

    private fun JsonObject.int(vararg keys: String): Int {
        for (k in keys) {
            val e = get(k) ?: continue
            try {
                if (e.isJsonPrimitive) {
                    val p = e.asJsonPrimitive
                    if (p.isNumber) return p.asInt
                    p.asString.toIntOrNull()?.let { return it }
                }
            } catch (_: Exception) { }
        }
        return 0
    }

    private fun JsonObject.arr(vararg keys: String): JsonArray? {
        for (k in keys) {
            val e = get(k) ?: continue
            if (e.isJsonArray) return e.asJsonArray
        }
        return null
    }

    private fun JsonObject.obj(vararg keys: String): JsonObject? {
        for (k in keys) {
            val e = get(k) ?: continue
            if (e.isJsonObject) return e.asJsonObject
        }
        return null
    }

    /** Ambil array item dari berbagai bentuk respons: array langsung, {data:[]}, {list:[]}, {items:[]}, {result:[]} */
    private fun extractArray(el: JsonElement, depth: Int = 0): JsonArray? {
        if (el.isJsonArray) return el.asJsonArray
        if (!el.isJsonObject || depth > 4) return null
        val o = el.asJsonObject
        o.arr("data", "list", "items", "result", "dramas", "videos", "books", "series")?.let {
            // khusus melolo: cell.books kosong, yang isi ada di cell_data[].books
            if (it.size() == 0 && o.has("cell_data")) {
                val merged = com.google.gson.JsonArray()
                o.getAsJsonArray("cell_data")?.forEach { cell ->
                    if (cell.isJsonObject) {
                        cell.asJsonObject.getAsJsonArray("books")?.forEach { b -> merged.add(b) }
                    }
                }
                if (merged.size() > 0) return merged
            }
            // khusus wetv: array berisi section, drama asli ada di section.items[]
            if (it.size() > 0 && it[0].isJsonObject && it[0].asJsonObject.get("items")?.isJsonArray == true) {
                val merged = com.google.gson.JsonArray()
                it.forEach { sec ->
                    if (sec.isJsonObject) {
                        sec.asJsonObject.getAsJsonArray("items")?.forEach { item -> merged.add(item) }
                    }
                }
                if (merged.size() > 0) return merged
            }
            return it
        }
        // cari rekursif di dalam object
        for ((_, v) in o.entrySet()) {
            if (v.isJsonArray && v.asJsonArray.size() > 0) return v.asJsonArray
            if (v.isJsonObject) {
                extractArray(v, depth + 1)?.let { return it }
            }
        }
        return null
    }

    // ---------- HTTP ----------

    private suspend fun fetchJson(url: String, params: Map<String, String>): JsonElement? {
        // retry 1x berdelay untuk gateway yang flaky (429/reset/IncompleteRead)
        repeat(2) { attempt ->
            try {
                val body = SonzaixClient.api.get(url, params)
                val text = body.string().trim()
                return if (text.isEmpty()) null else JsonParser.parseString(text)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                if (attempt == 0) kotlinx.coroutines.delay(1200)
            }
        }
        return null
    }

    private fun buildUrl(cfg: SxProviderConfig, ep: SxEndpoint, vars: Map<String, String> = emptyMap()): Pair<String, Map<String, String>> {
        var path = ep.path
        val params = ep.params.toMutableMap()
        // ganti placeholder di path dan di params
        for ((k, v) in vars) {
            path = path.replace("{$k}", v)
            for ((pk, pv) in params.toMap()) {
                if (pv.contains("{$k}")) params[pk] = pv.replace("{$k}", v)
            }
        }
        val url = SonzaixApi.fullUrl("/api/${cfg.slug}$path")
        return url to params
    }

    /** URL alternatif tanpa prefix /api (beberapa provider pakai pola /slug/...). */
    private fun buildAltUrl(cfg: SxProviderConfig, ep: SxEndpoint, vars: Map<String, String> = emptyMap()): Pair<String, Map<String, String>> {
        var path = ep.path
        val params = ep.params.toMutableMap()
        for ((k, v) in vars) {
            path = path.replace("{$k}", v)
            for ((pk, pv) in params.toMap()) {
                if (pv.contains("{$k}")) params[pk] = pv.replace("{$k}", v)
            }
        }
        val url = SonzaixApi.fullUrl("/${cfg.slug}$path")
        return url to params
    }

    /** Fetch dengan fallback ke URL alternatif bila hasil kosong/gagal. */
    private suspend fun fetchJsonWithFallback(
        cfg: SxProviderConfig, ep: SxEndpoint, vars: Map<String, String> = emptyMap(),
        extraParams: Map<String, String> = emptyMap()
    ): JsonElement? {
        val (url, params) = buildUrl(cfg, ep, vars)
        val mergedParams = params + extraParams
        val primary = fetchJson(url, mergedParams)
        // kalau primary mengembalikan error 404 atau kosong, coba alternatif
        if (primary != null && !isErrorJson(primary)) return primary
        val (altUrl, altParams) = buildAltUrl(cfg, ep, vars)
        if (altUrl != url) {
            val alt = fetchJson(altUrl, altParams + extraParams)
            if (alt != null && !isErrorJson(alt)) return alt
        }
        return primary
    }

    private fun isErrorJson(el: JsonElement): Boolean {
        if (!el.isJsonObject) return false
        val o = el.asJsonObject
        return o.has("error") || (o.has("status") && o.get("status").asInt == 404)
    }

    // ---------- mapping ke model UI ----------

    private fun mapItem(o: JsonObject, providerSlug: String, providerName: String): DramaItem? {
        val id = o.str("id", "bookId", "book_id", "videoId", "video_id", "seriesId", "series_id", "dramaId", "drama_id", "vid", "contentId", "content_id", "cid", "subjectId", "subject_id", "theater_id", "theaterId", "dcup") ?: return null
        val title = o.str("title", "name", "bookName", "book_name", "videoTitle", "video_title", "seriesName", "series_name", "dramaTitle", "drama_title", "theater_name", "theaterName", "nseri") ?: return null
        val cover = o.str("cover", "poster", "posterUrl", "poster_url", "image", "imageUrl", "image_url", "thumbnail", "thumb", "thumb_url", "pic", "img", "banner", "coverUrl", "cover_url", "verticalCover", "vertical_cover", "posterPath", "poster_path", "cover_img", "coverImg", "pday")
        val epCount = o.int("episodes", "episodeCount", "episode_count", "totalEpisodes", "total_episodes", "chapterCount", "chapter_count", "epCount", "total", "ewood")
        // beberapa API: episodes berupa array
        val epCountFromArr = o.arr("episodes", "chapters", "episodeList")?.size() ?: 0
        val rating = try { o.get("rating")?.asDouble ?: o.get("score")?.asDouble ?: 0.0 } catch (_: Exception) { 0.0 }
        val views = try {
            // melolo: cover_stat_infos[0].stat_value = "5.4M" (singkatan K/M)
            val meloloStat = try {
                o.getAsJsonArray("cover_stat_infos")?.firstOrNull()
                    ?.asJsonObject?.get("stat_value")?.asString
            } catch (_: Exception) { null }
            if (!meloloStat.isNullOrBlank()) {
                parseCompactNumber(meloloStat)
            } else {
                // shortmax rank: hotScore = "1911k" (skor popularitas)
                val v = o.get("viewCount") ?: o.get("views") ?: o.get("playCount") ?: o.get("hits") ?: o.get("hotScore")
                when {
                    v == null -> 0L
                    v.isJsonPrimitive && v.asJsonPrimitive.isNumber -> v.asLong
                    else -> parseCompactNumber(v.asString)
                }
            }
        } catch (_: Exception) { 0L }
        val composite = "$providerSlug:$id"
        return DramaItem(
            id = composite,
            title = title,
            slug = composite,
            posterUrl = cover,
            providerName = providerName,
            type = "drama",
            rating = rating,
            viewCount = views,
            episodeCount = if (epCountFromArr > 0) epCountFromArr else epCount,
            genres = parseGenres(o),
            synopsis = (o.str("abstract", "dwill", "synopsis", "description", "desc", "summary", "intro", "sub_abstract") ?: "").take(500)
        )
    }

    // ---------- Smart search lokal (judul + sinopsis + fuzzy) ----------

    /** Index lokal untuk smart search. Dibangun malas dari katalog Melolo (ada sinopsis). */
    private val localIndex = mutableListOf<DramaItem>()
    private var indexBuilt = false

    private suspend fun ensureLocalIndex() {
        if (indexBuilt) return
        indexBuilt = true
        try {
            val items = coroutineScope {
                (1..5).map { p -> async { fetchMeloloBrowse(p, 20) } }.awaitAll()
            }.flatten().distinctBy { it.id }
            synchronized(localIndex) {
                val ids = localIndex.map { it.id }.toSet()
                localIndex.addAll(items.filter { it.id !in ids && it.synopsis.isNotBlank() })
            }
        } catch (_: Exception) { }
    }

    private fun normalize(s: String): String =
        s.lowercase().replace(Regex("[^a-z0-9 ]"), " ").replace(Regex("\\s+"), " ").trim()

    /** Skor relevansi: judul exact > judul fuzzy > sinopsis. 0 = tidak cocok. */
    private fun relevanceScore(query: String, item: DramaItem): Int {
        val q = normalize(query)
        if (q.isBlank()) return 0
        val title = normalize(item.title)
        val syn = normalize(item.synopsis)
        val qWords = q.split(" ").filter { it.length > 2 }
        var score = 0
        if (title.contains(q)) score += 100
        if (qWords.isNotEmpty() && qWords.all { title.contains(it) }) score += 60
        // fuzzy ringan: kata query cocok sebagai awalan kata judul (toleransi typo)
        for (w in qWords) {
            if (w.length >= 4 && title.split(" ").any { tw -> tw.startsWith(w.take(4)) }) score += 15
        }
        if (syn.contains(q)) score += 40
        if (qWords.isNotEmpty() && qWords.all { syn.contains(it) }) score += 25
        return score
    }

    private fun searchLocal(query: String, limit: Int): List<DramaItem> {
        val snapshot = synchronized(localIndex) { localIndex.toList() }
        return snapshot.mapNotNull { item ->
            val s = relevanceScore(query, item)
            if (s > 0) item to s else null
        }.sortedByDescending { it.second }
            .map { it.first }
            .take(limit)
    }

    /** Pemanasan index lokal tanpa menunggu (dipakai saran saat mengetik). */
    suspend fun warmIndex() = ensureLocalIndex()

    private fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        val dp = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            var prev = dp[0]
            dp[0] = i
            for (j in 1..b.length) {
                val cur = dp[j]
                dp[j] = minOf(dp[j] + 1, dp[j - 1] + 1, prev + if (a[i - 1] == b[j - 1]) 0 else 1)
                prev = cur
            }
        }
        return dp[b.length]
    }

    /**
     * "Maksud Anda": cari judul terdekat dari index lokal bila query kemungkinan typo.
     * Dipakai saat hasil pencarian sedikit/kosong. null = tidak ada koreksi yang yakin.
     */
    fun suggestCorrection(query: String): String? {
        val snapshot = synchronized(localIndex) { localIndex.toList() }
        if (snapshot.isEmpty()) return null
        val qNorm = normalize(query)
        val qWords = qNorm.split(" ").filter { it.length >= 3 }
        if (qWords.isEmpty()) return null
        var bestTitle: String? = null
        var bestDist = Int.MAX_VALUE
        for (item in snapshot) {
            val tWords = normalize(item.title).split(" ").filter { it.length >= 3 }
            for (qw in qWords) {
                for (tw in tWords) {
                    if (qw == tw) continue // kata sudah pas, bukan typo
                    val d = levenshtein(qw, tw)
                    val threshold = if (tw.length <= 5) 1 else 2
                    if (d in 1..threshold && d < bestDist) {
                        bestDist = d
                        bestTitle = item.title
                    }
                }
            }
        }
        if (bestTitle != null && normalize(bestTitle) == qNorm) return null
        return bestTitle
    }

    /** Saran judul dari index lokal (cocok awalan/frasa), untuk digabung dengan saran upstream. */
    private fun localSuggestTitles(query: String, limit: Int = 4): List<String> {
        val q = normalize(query)
        if (q.length < 2) return emptyList()
        val snapshot = synchronized(localIndex) { localIndex.toList() }
        return snapshot.map { it.title }
            .filter { it.isNotBlank() && normalize(it).contains(q) }
            .distinct()
            .take(limit)
    }

    // ---------- API publik (dipakai ViewModel) ----------

    fun getProviders(): List<Provider> =
        SonzaixProviders.all
            .filter { !isProviderGone(it.slug) }
            .map {
                Provider(
                    providerName = it.name,
                    providerSlug = it.slug,
                    providerType = "sonzaix",
                    logoUrl = null
                )
            }

    /** Slug provider yang dipantau kesehatannya (10 provider aktif). */
    fun monitoredProviderSlugs(): List<String> = mergedProviderSlugs

    /** Daftar slug aktif = yang dipakai, dikurangi yang disembunyikan monitor kesehatan. */
    private fun activeSlugs(): List<String> =
        mergedProviderSlugs.filter { !isProviderGone(it) }

    /** Probe ringan: true bila endpoint browse mengembalikan minimal 1 item. */
    suspend fun probeProvider(slug: String): Boolean {
        val cfg = SonzaixProviders.bySlug[slug] ?: return false
        return try {
            fetchList(cfg, 1, 1).isNotEmpty()
        } catch (_: Exception) { false }
    }

    /**
     * Ambil daftar konten.
     * @param providerSlug null = gabungan semua provider (ambil dari beberapa provider populer)
     * @param query null = mode list biasa; diisi = mode search
     * @param page halaman (1-based untuk UI)
     */
    suspend fun getContents(
        providerSlug: String?,
        query: String?,
        page: Int,
        limit: Int = 18
    ): ContentsResponse {
        val q = query?.trim().orEmpty()
        val cacheKey = q.lowercase()
        // cache hasil pencarian: query sama tidak request ulang (hemat kuota, instan)
        if (q.isNotEmpty() && page == 1) {
            synchronized(searchCache) { searchCache[cacheKey] }?.let { cached ->
                return ContentsResponse(items = cached, nextCursor = null, hasMore = false)
            }
        }
        var res = fetchContentsOnce(providerSlug, query, page, limit)
        // gateway upstream flaky: bila halaman 1 kosong, coba sekali lagi setelah jeda
        if (page == 1 && res.items.isEmpty()) {
            kotlinx.coroutines.delay(1500)
            res = fetchContentsOnce(providerSlug, query, page, limit)
        }
        // simpan ke cache (hanya hasil non-kosong; hasil kosong boleh dicoba lagi nanti)
        if (q.isNotEmpty() && page == 1 && res.items.isNotEmpty()) {
            synchronized(searchCache) {
                searchCache[cacheKey] = res.items
                while (searchCache.size > 30) {
                    searchCache.remove(searchCache.keys.first())
                }
            }
        }
        return res
    }

    /** Cache hasil pencarian per sesi: query -> items (LRU sederhana, maks 30). */
    private val searchCache = LinkedHashMap<String, List<DramaItem>>(32, 0.75f, true)

    private suspend fun fetchContentsOnce(
        providerSlug: String?,
        query: String?,
        page: Int,
        limit: Int = 18
    ): ContentsResponse {
        return if (!query.isNullOrBlank()) {
            searchAll(query, limit)
        } else if (providerSlug.isNullOrBlank()) {
            // gabungan: ambil dari 6 provider utama paralel, gabung & acak ringan
            coroutineScope {
                // provider mati upstream dikecualikan (probe 13 Okt: 400/502)
                val top = activeSlugs()
                val jobs = top.mapNotNull { SonzaixProviders.bySlug[it] }.map { cfg ->
                    async {
                        try {
                            val list = fetchList(cfg, page, limit)
                            // halaman 1: tambah item ranking (provider yang list-nya tidak paginasi jadi lebih panjang)
                            val rank = if (page == 1) fetchRank(cfg) else emptyList<DramaItem>()
                            list + rank
                        } catch (e: Exception) {
                            if (e is kotlinx.coroutines.CancellationException) throw e
                            emptyList<DramaItem>()
                        }
                    }
                }
                val lists = jobs.awaitAll()
                // tampilkan SEMUA item dari semua provider (jangan take(limit) yang membuang ~90 item)
                val merged = lists.flatten().distinctBy { it.id }.shuffled()
                // berhenti bila tidak ada provider yang mengembalikan item
                val next = if (merged.isEmpty()) null else (page + 1).toString()
                ContentsResponse(items = merged, nextCursor = next, hasMore = next != null)
            }
        } else {
            val cfg = SonzaixProviders.bySlug[providerSlug] ?: return ContentsResponse()
            // Melolo: halaman 2+ browse katalog dalam via search pagination (ratusan judul, 0 overlap)
            val items = if (providerSlug == "melolo" && page > 1) {
                fetchMeloloBrowse(page - 1, limit)
            } else {
                fetchList(cfg, page, limit)
            }
            // halaman 1: gabung ranking agar daftar provider yang tidak paginasi tetap panjang
            val rankItems = if (page == 1) runCatching { fetchRank(cfg) }.getOrDefault(emptyList()) else emptyList()
            val merged = (items + rankItems).distinctBy { it.id }
            val next = if (items.size < limit) null else (page + 1).toString()
            ContentsResponse(items = merged, nextCursor = next, hasMore = next != null)
        }
    }

    /** Provider yang dipakai mode gabungan & search: sudah diprobe hidup 15 Okt 2026. */
    private val mergedProviderSlugs = listOf("dramabite", "stardusttv", "shortmax", "melolo", "playlet", "drakorid", "bonustv", "cashdrama", "wetv", "dotdrama")

    /**
     * Feed "Terbaru": gabungan feed terbaru semua provider, disusun round-robin
     * (giliran tiap provider) agar variatif. DrakorID memakai /drama/terbaru
     * (paginasi asli), Playlet & ShortMax paginasi, sisanya feed list masing-masing.
     */
    suspend fun getLatest(page: Int = 1, limit: Int = 18): ContentsResponse = coroutineScope {
        val slugs = activeSlugs()
        val perProvider = 6
        val jobs = slugs.map { slug ->
            async {
                val cfg = SonzaixProviders.all.firstOrNull { it.slug == slug } ?: return@async emptyList()
                runCatching { fetchList(cfg, page, perProvider) }.getOrDefault(emptyList())
            }
        }
        val lists = jobs.awaitAll()
        // round-robin: ambil giliran dari tiap provider agar tidak didominasi satu provider
        val interleaved = mutableListOf<DramaItem>()
        val seen = HashSet<String>()
        val maxLen = lists.maxOfOrNull { it.size } ?: 0
        for (i in 0 until maxLen) {
            for (lst in lists) {
                val item = lst.getOrNull(i) ?: continue
                if (seen.add(item.id)) interleaved.add(item)
            }
        }
        val result = interleaved.take(limit)
        val next = if (result.size < limit) null else (page + 1).toString()
        ContentsResponse(items = result, nextCursor = next, hasMore = next != null)
    }

    private suspend fun fetchList(cfg: SxProviderConfig, page: Int, limit: Int): List<DramaItem> {
        val ep = cfg.list
        val vars = mutableMapOf<String, String>()
        val params = ep.params.toMutableMap()
        if (ep.pageParam != null) {
            val p = (page - 1) + ep.pageStart
            params[ep.pageParam] = p.toString()
            vars["page"] = p.toString()
        }
        val json = fetchJsonWithFallback(cfg, ep, vars) ?: return emptyList()
        val arr = extractArray(json) ?: return emptyList()
        return arr.mapNotNull { el ->
            if (el.isJsonObject) mapItem(el.asJsonObject, cfg.slug, cfg.name) else null
        }.take(limit)
    }

    /** Search ke semua provider paralel, gabung hasilnya (timeout 12 detik). */
    /**
     * Ambil subtitle (khusus BiliTV, auto-translate ke [lang]).
     * Kembalikan [SubtitleData] berisi URL http atau konten VTT mentah.
     */
    data class SubtitleData(val url: String? = null, val vttContent: String? = null)

    suspend fun getSubtitle(compositeId: String, ep: Int, lang: String = "id"): SubtitleData? {
        return try {
            val (slug, rawId) = parseComposite(compositeId) ?: return null
            if (slug != "bilitv") return null
            withTimeoutOrNull(10000) {
                val url = SonzaixApi.fullUrl("/api/bilitv/api/v1/subtitle/$rawId/$ep")
                val body = try {
                    SonzaixClient.api.get(url, mapOf("lang" to lang, "format" to "vtt"))
                } catch (_: Exception) { return@withTimeoutOrNull null }
                val text = try { body.string().trim() } catch (_: Exception) { return@withTimeoutOrNull null }
                if (text.isEmpty()) return@withTimeoutOrNull null
                // VTT mentah langsung
                if (text.contains("WEBVTT")) return@withTimeoutOrNull SubtitleData(vttContent = text)
                // JSON: cari URL atau konten
                try {
                    val json = JsonParser.parseString(text)
                    if (json.isJsonObject) {
                        val o = json.asJsonObject
                        o.str("url", "subtitleUrl", "subtitle_url", "vtt", "vttUrl")?.let {
                            if (it.startsWith("http")) return@withTimeoutOrNull SubtitleData(url = it)
                        }
                        o.str("content", "data", "subtitle")?.let { c ->
                            if (c.contains("WEBVTT")) return@withTimeoutOrNull SubtitleData(vttContent = c)
                        }
                    }
                } catch (_: Exception) { }
                null
            }
        } catch (_: Exception) { null }
    }

    /** Saran pencarian dari endpoint suggest (pakai Melolo). */
    suspend fun getSuggestions(query: String): List<String> {
        if (query.trim().length < 2) return emptyList()
        // saran multi-sumber: index lokal (cepat, offline) + suggest upstream Melolo
        val local = localSuggestTitles(query)
        val remote = try {
            withTimeoutOrNull(6000) {
                val cfg = SonzaixProviders.bySlug["melolo"] ?: return@withTimeoutOrNull emptyList<String>()
                val ep = SxEndpoint("/api/v1/search/suggest", mapOf("lang" to "id"), queryParam = "q")
                val json = fetchJsonWithFallback(cfg, ep, emptyMap(), mapOf("q" to query.trim()))
                    ?: return@withTimeoutOrNull emptyList<String>()
                if (!json.isJsonObject) return@withTimeoutOrNull emptyList<String>()
                json.asJsonObject.getAsJsonArray("query_result")
                    ?.mapNotNull { try { it.asString } catch (_: Exception) { null } }
                    ?.filter { it.isNotBlank() }
                    ?: emptyList()
            } ?: emptyList()
        } catch (_: Exception) { emptyList() }
        return (local + remote).distinct().take(8)
    }

    /** Ranking populer asli dari endpoint rank tiap provider (paralel). */
    /** Ambil item ranking satu provider (best-effort, kosong bila tidak ada/gagal). */
    private suspend fun fetchRank(cfg: SxProviderConfig): List<DramaItem> {
        val r = cfg.rank ?: return emptyList()
        return try {
            val json = fetchJsonWithFallback(cfg, r) ?: return emptyList()
            val arr = extractArray(json) ?: return emptyList()
            arr.mapNotNull { el ->
                if (el.isJsonObject) mapItem(el.asJsonObject, cfg.slug, cfg.name) else null
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emptyList()
        }
    }

    /**
     * Browse katalog dalam Melolo via search pagination (offset).
     * Terbukti 0 overlap antar halaman (probe 15 Okt 2026).
     * Query "a" dipakai agar cakupan katalog maksimal (ratusan judul).
     */
    private suspend fun fetchMeloloBrowse(browsePage: Int, limit: Int): List<DramaItem> {
        val cfg = SonzaixProviders.bySlug["melolo"] ?: return emptyList()
        val s = cfg.search ?: return emptyList()
        return try {
            val offset = (browsePage - 1) * limit
            val (url, baseParams) = buildUrl(cfg, s, mapOf("q" to "a"))
            val params = baseParams.toMutableMap()
            params["limit"] = limit.toString()
            params["offset"] = offset.toString()
            params[s.queryParam ?: "q"] = "a"
            val json = fetchJson(url, params) ?: return emptyList()
            val arr = extractArray(json) ?: return emptyList()
            arr.mapNotNull { el ->
                if (el.isJsonObject) mapItem(el.asJsonObject, cfg.slug, cfg.name) else null
            }.take(limit)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emptyList()
        }
    }

    suspend fun getRanking(limit: Int = 30): ContentsResponse {
        val results: List<DramaItem> = withTimeoutOrNull(15000) {
            coroutineScope {
                SonzaixProviders.all.mapNotNull { cfg ->
                    if (cfg.rank == null) return@mapNotNull null
                    if (isProviderGone(cfg.slug)) return@mapNotNull null
                    async<List<DramaItem>> { fetchRank(cfg) }
                }.awaitAll().flatten()
            }
        } ?: emptyList()
        // dedup + prioritaskan yang ada cover + ada rating/views
        val dedup = results.distinctBy { it.id }
            .sortedWith(
                compareByDescending<DramaItem> { if (!it.posterUrl.isNullOrBlank()) 1 else 0 }
                    .thenByDescending { it.rating }
                    .thenByDescending { it.viewCount }
            )
            .take(limit)
        return ContentsResponse(items = dedup, nextCursor = null, hasMore = false)
    }

    // provider yang diketahui mati upstream — jangan buang request
    private val deadSlugs = setOf("reelshort", "netshort", "freereels", "dramabox")

    private suspend fun searchAll(query: String, limit: Int): ContentsResponse {
        // HANYA 10 provider terprobe (bukan 36): OkHttp membatasi 5 koneksi/host,
        // 36 request paralel antri berlapis sehingga timeout 12 dtk selalu membunuh semua hasil.
        val results: List<DramaItem> = withTimeoutOrNull(20000) {
            coroutineScope {
                val upstreamJob = async {
                    activeSlugs().mapNotNull { SonzaixProviders.bySlug[it] }.mapNotNull { cfg ->
                        val s = cfg.search ?: return@mapNotNull null
                        async<List<DramaItem>> {
                            try {
                                val vars = mapOf("q" to query)
                                val extraParams = mutableMapOf<String, String>()
                                s.queryParam?.let { extraParams[it] = query }
                                val json = fetchJsonWithFallback(cfg, s, vars, extraParams) ?: return@async emptyList<DramaItem>()
                                val arr = extractArray(json) ?: return@async emptyList<DramaItem>()
                                arr.mapNotNull { el ->
                                    if (el.isJsonObject) mapItem(el.asJsonObject, cfg.slug, cfg.name) else null
                                }
                            } catch (e: Exception) {
                                if (e is kotlinx.coroutines.CancellationException) throw e
                                emptyList<DramaItem>()
                            }
                        }
                    }.awaitAll().flatten()
                }
                // smart search lokal: kata di judul + sinopsis + nama mirip (fuzzy)
                val localJob = async {
                    ensureLocalIndex()
                    searchLocal(query, limit)
                }
                val merged = (upstreamJob.await() + localJob.await()).distinctBy { it.id }
                // urutkan by relevansi terhadap query, lalu prioritaskan yang ada cover
                merged.sortedWith(
                    compareByDescending<DramaItem> { relevanceScore(query, it) }
                        .thenByDescending { if (!it.posterUrl.isNullOrBlank()) 1 else 0 }
                )
            }
        } ?: emptyList()
        val dedup = results.distinctBy { it.id }.take(limit)
        return ContentsResponse(items = dedup, nextCursor = null, hasMore = false)
    }


    /** compositeId format "slug:id" */
    suspend fun getDetail(compositeId: String): DramaDetail =
        cached("detail:$compositeId") { fetchDetail(compositeId) }

    private suspend fun fetchDetail(compositeId: String): DramaDetail {
        val (slug, rawId) = parseComposite(compositeId)
            ?: throw IllegalArgumentException("ID tidak valid")
        val cfg = SonzaixProviders.bySlug[slug]
            ?: throw IllegalArgumentException("Provider tidak dikenal")
        val dep = cfg.detail ?: throw IllegalStateException("Detail tidak tersedia untuk ${cfg.name}")

        // drakorid/moviebox pakai query param id; lainnya path param
        val (url, params) = buildUrl(cfg, dep, mapOf("id" to rawId))
        val json = fetchJson(url, params) ?: throw IllegalStateException("Gagal memuat detail")
        val obj = when {
            json.isJsonObject -> json.asJsonObject
            else -> throw IllegalStateException("Format detail tidak dikenal")
        }
        // beberapa API bungkus di {data:{...}}
        val d = obj.obj("data", "result", "drama", "book", "video", "info", "series") ?: obj

        val title = d.str("title", "name", "bookName", "book_name", "videoTitle", "video_title", "seriesName", "series_name", "dramaTitle", "drama_title") ?: "Tanpa Judul"
        val descRaw = d.str("description", "desc", "synopsis", "summary", "intro", "abstract", "sub_abstract", "content", "meta_sinopsis", "meta_description")
        // beberapa provider (drakorid) kirim sinopsis sebagai HTML — strip tag
        val descStripped = descRaw?.replace(Regex("<[^>]*>"), " ")?.replace(Regex("\\s+"), " ")?.trim()
        val desc = descStripped?.takeIf { it.isNotEmpty() }
        // status: drakorid "Aired: Oct 12, 2026 - ?" = Ongoing; dua tanggal = Tamat
        val status: String? = try {
            val aired = Regex("Aired:\\s*([^\\n<]+)", RegexOption.IGNORE_CASE)
                .find(descStripped ?: "")?.groupValues?.getOrNull(1)?.trim()
            when {
                aired.isNullOrBlank() -> null
                aired.contains("?") -> "Ongoing"
                aired.contains("-") -> "Tamat"
                else -> null
            }
        } catch (_: Exception) { null }
        val cover = d.str("cover", "poster", "posterUrl", "poster_url", "image", "imageUrl", "image_url", "thumbnail", "thumb", "thumb_url", "pic", "img", "banner", "verticalCover", "vertical_cover")
        val banner = d.str("banner", "horizontalCover", "horizontal_cover", "coverLandscape", "cover_landscape") ?: cover

        // episode list: cari array episodes/chapters
        val epArr = d.arr("episodes", "chapters", "episodeList", "chapterList", "list")
        val episodes = mutableListOf<Episode>()
        epArr?.forEachIndexed { idx, el ->
            if (!el.isJsonObject) return@forEachIndexed
            val eo = el.asJsonObject
            val num = eo.int("number", "episodeNumber", "ep", "episode", "chapterNo", "index")
            val epNum = if (num > 0) num else idx + 1
            val epTitle = eo.str("title", "name", "chapterTitle") ?: "Episode $epNum"
            val epId = eo.str("id", "chapterId", "episodeId") ?: epNum.toString()
            episodes.add(Episode(id = epId, episodeNumber = epNum, title = epTitle))
        }
        // fallback: jumlah episode dari field count
        val countHint = d.int("episodes", "episodeCount", "episode_count", "totalEpisodes", "total_episodes", "chapterCount", "chapter_count", "total", "epCount")
        if (episodes.isEmpty() && countHint > 0) {
            repeat(minOf(countHint, 200)) { i ->
                episodes.add(Episode(id = (i + 1).toString(), episodeNumber = i + 1, title = "Episode ${i + 1}"))
            }
        }

        return DramaDetail(
            id = compositeId,
            title = title,
            slug = compositeId,
            description = desc,
            posterUrl = cover,
            bannerUrl = banner,
            providerName = cfg.name,
            providerSlug = cfg.slug,
            type = "drama",
            genres = parseGenres(d),
            status = status,
            episodeCount = if (episodes.isNotEmpty()) episodes.size else countHint,
            episodes = episodes
        )
    }

    /** Ambil URL video episode. compositeId "slug:id". */
    suspend fun getStream(compositeId: String, ep: Int, extra: Map<String, String> = emptyMap()): StreamResponse {
        val (slug, rawId) = parseComposite(compositeId)
            ?: return StreamResponse(message = "ID tidak valid")
        val cfg = SonzaixProviders.bySlug[slug]
            ?: return StreamResponse(message = "Provider tidak dikenal")
        val epc = cfg.episode
            ?: return StreamResponse(message = "Stream tidak tersedia untuk ${cfg.name}")

        // kasus khusus: dotdrama/microdrama/shorten — video URL ada di detail
        if (cfg.slug in listOf("dotdrama", "microdrama", "shorten")) {
            return streamFromDetail(cfg, rawId, ep)
        }
        // kasus khusus: melolo multi-video — ambil semua lalu pilih episode
        if (cfg.slug == "melolo") {
            return meloloStream(cfg, rawId, ep)
        }
        // kasus khusus: sodareels butuh ewash dari /info
        if (cfg.slug == "sodareels") {
            return sodareelsStream(cfg, rawId, ep)
        }
        // kasus khusus: playlet mengembalikan m3u8 mentah (bukan JSON) — URL-nya langsung video
        if (cfg.slug == "playlet") {
            val epc2 = cfg.episode ?: return StreamResponse(message = "Stream tidak tersedia")
            val (pUrl, _) = buildUrl(cfg, epc2, mapOf("id" to rawId, "ep" to ep.toString()))
            return StreamResponse(url = pUrl, type = "hls")
        }
        // kasus khusus: drakorid butuh nilai field 'streaming' dari detail
        if (cfg.slug == "drakorid") {
            return drakoridStream(cfg, rawId, ep)
        }

        val vars = mutableMapOf("id" to rawId, "ep" to ep.toString(), "quality" to "720")
        vars.putAll(extra)
        val (url, params) = buildUrl(cfg, epc, vars)
        val json = fetchJson(url, params)
            ?: return StreamResponse(message = "Gagal memuat video (server tidak merespons)")
        val videoUrl = extractVideoUrl(json)
            ?: return StreamResponse(message = "URL video tidak ditemukan untuk episode ini")
        val qualities = extractQualities(json, cfg.slug).toMutableList()
        // BiliTV: endpoint dukung param quality, sediakan opsi meski response cuma 1 URL
        if (qualities.isEmpty() && cfg.slug == "bilitv") {
            qualities.addAll(listOf("480", "720", "1080").map { q ->
                VideoQuality("${q}p", "__quality__:$q")
            })
        }
        return StreamResponse(
            url = videoUrl,
            type = if (videoUrl.contains(".m3u8")) "hls" else "mp4",
            qualities = qualities.distinctBy { it.label }
        )
    }

    /** Kumpulkan varian kualitas video_480/720/1080 dari JSON. */
    private fun extractQualities(json: JsonElement, slug: String): List<VideoQuality> {
        val out = mutableListOf<VideoQuality>()
        fun collect(o: JsonObject) {
            val q480 = o.str("video_480")
            val q720 = o.str("video_720")
            val q1080 = o.str("video_1080")
            if (!q480.isNullOrBlank() && q480.startsWith("http")) out.add(VideoQuality("480p", q480))
            if (!q720.isNullOrBlank() && q720.startsWith("http")) out.add(VideoQuality("720p", q720))
            if (!q1080.isNullOrBlank() && q1080.startsWith("http")) out.add(VideoQuality("1080p", q1080))
        }
        fun walk(el: JsonElement, depth: Int = 0) {
            if (depth > 3) return
            when {
                el.isJsonObject -> {
                    val o = el.asJsonObject
                    collect(o)
                    o.obj("video")?.let { collect(it) }
                    for (k in listOf("data", "result", "stream", "play")) {
                        o.get(k)?.let { walk(it, depth + 1) }
                    }
                }
                el.isJsonArray -> el.asJsonArray.forEach { walk(it, depth + 1) }
            }
        }
        walk(json)
        return out.distinctBy { it.label }
    }

    private fun extractVideoUrl(json: JsonElement): String? {
        if (json.isJsonPrimitive) {
            val s = json.asString
            if (s.startsWith("http")) return s
            return null
        }
        if (!json.isJsonObject) return null
        val o = json.asJsonObject
        // field langsung (termasuk varian per-provider dari hasil probe)
        o.str(
            "video", "videoUrl", "url", "playUrl", "streamUrl", "src", "file", "source",
            "hls", "m3u8", "h264", "h265", "hls_url", "stream_url",
            "video_720", "video_1080", "video_480"
        )?.let {
            if (it.startsWith("http")) return it
        }
        // nested: data.video.video_720 (shortmax), dsb
        o.obj("video")?.str("video_720", "video_1080", "video_480", "url")?.let {
            if (it.startsWith("http")) return it
        }
        // nested: data, result, stream, play
        for (k in listOf("data", "result", "stream", "play")) {
            val n = o.get(k) ?: continue
            if (n.isJsonObject) {
                extractVideoUrl(n)?.let { return it }
            } else if (n.isJsonPrimitive && n.asString.startsWith("http")) {
                return n.asString
            }
        }
        // array of sources: adaptive[0].url (cashdrama), sources, videos, urls, qualities
        o.arr("adaptive", "sources", "videos", "urls", "qualities")?.let { arr ->
            for (el in arr) {
                extractVideoUrl(el)?.let { return it }
            }
        }
        return null
    }

    private suspend fun streamFromDetail(cfg: SxProviderConfig, rawId: String, ep: Int): StreamResponse {
        return try {
            val detail = getDetail("${cfg.slug}:$rawId")
            // coba fetch ulang detail mentah untuk cari video URL episode
            val dep = cfg.detail ?: return StreamResponse(message = "Detail tidak tersedia")
            val (url, params) = buildUrl(cfg, dep, mapOf("id" to rawId))
            val json = fetchJson(url, params) ?: return StreamResponse(message = "Gagal memuat detail")
            val obj = if (json.isJsonObject) json.asJsonObject else return StreamResponse(message = "Format tidak dikenal")
            val d = obj.obj("data", "result", "drama") ?: obj
            val epArr = d.arr("episodes", "chapters", "episodeList")
            val target = epArr?.firstOrNull { el ->
                el.isJsonObject && (
                    el.asJsonObject.int("number", "episodeNumber", "ep", "index") == ep
                    )
            } ?: epArr?.let { if (ep - 1 in 0 until it.size()) it[ep - 1] else null }
            val vurl = target?.let { extractVideoUrl(it) }
                ?: return StreamResponse(message = "Video episode $ep tidak tersedia")
            StreamResponse(url = vurl, type = if (vurl.contains(".m3u8")) "hls" else "mp4")
        } catch (e: Exception) {
            StreamResponse(message = "Gagal memuat video: ${e.message}")
        }
    }

    private suspend fun meloloStream(cfg: SxProviderConfig, rawId: String, ep: Int): StreamResponse {
        val epc = cfg.episode ?: return StreamResponse(message = "Stream tidak tersedia")
        val (url, params) = buildUrl(cfg, epc, mapOf("id" to rawId))
        val json = fetchJson(url, params) ?: return StreamResponse(message = "Gagal memuat video")
        val arr = extractArray(json) ?: return StreamResponse(message = "Format video tidak dikenal")
        val target = arr.firstOrNull { el ->
            el.isJsonObject && el.asJsonObject.int("number", "episode", "ep") == ep
        } ?: if (ep - 1 in 0 until arr.size()) arr[ep - 1] else null
        val vurl = target?.let { extractVideoUrl(it) }
            ?: return StreamResponse(message = "Video episode $ep tidak tersedia")
        return StreamResponse(url = vurl, type = if (vurl.contains(".m3u8")) "hls" else "mp4")
    }

    /** drakorid: ambil field 'streaming' dari detail, lalu panggil /drama/stream?streaming=... */
    private suspend fun drakoridStream(cfg: SxProviderConfig, rawId: String, ep: Int): StreamResponse {
        return try {
            val dep = cfg.detail ?: return StreamResponse(message = "Detail tidak tersedia")
            val (url, params) = buildUrl(cfg, dep, mapOf("id" to rawId))
            val json = fetchJson(url, params) ?: return StreamResponse(message = "Gagal memuat detail")
            val obj = if (json.isJsonObject) json.asJsonObject else return StreamResponse(message = "Format tidak dikenal")
            val d = obj.obj("data", "result") ?: obj
            // cari episode ke-ep lalu ambil field streaming
            val epArr = d.arr("episodes")?.let { arr ->
                // episodes.data[...] untuk drakorid
                val first = arr.firstOrNull()
                if (first != null && first.isJsonObject && first.asJsonObject.has("data")) {
                    first.asJsonObject.get("data")?.takeIf { it.isJsonArray }?.asJsonArray ?: arr
                } else arr
            } ?: d.obj("episodes")?.arr("data")
            var streaming: String? = null
            epArr?.forEach { el ->
                if (!el.isJsonObject) return@forEach
                val eo = el.asJsonObject
                val num = eo.int("episode_number", "episodeNumber", "ep", "number")
                if (num == ep || (num == 0 && streaming == null)) {
                    streaming = eo.str("streaming")
                    if (num == ep) return@forEach
                }
            }
            if (streaming.isNullOrBlank()) return StreamResponse(message = "Link streaming episode $ep tidak tersedia")
            val epc = cfg.episode ?: return StreamResponse(message = "Stream tidak tersedia")
            val (sUrl, sParams) = buildUrl(cfg, epc, mapOf("stream" to streaming!!))
            val sJson = fetchJson(sUrl, sParams) ?: return StreamResponse(message = "Gagal memuat stream")
            val vurl = extractVideoUrl(sJson) ?: return StreamResponse(message = "URL video tidak ditemukan")
            StreamResponse(url = vurl, type = if (vurl.contains(".m3u8")) "hls" else "mp4")
        } catch (e: Exception) {
            StreamResponse(message = "Gagal memuat video: ${e.message}")
        }
    }

    private suspend fun sodareelsStream(cfg: SxProviderConfig, rawId: String, ep: Int): StreamResponse {        // 1. ambil /info untuk dapat ewash
        val (infoUrl, infoParams) = buildUrl(cfg, SxEndpoint("/api/v1/info/{id}"), mapOf<String, String>("id" to rawId))
        val infoJson = fetchJson(infoUrl, infoParams) ?: return StreamResponse(message = "Gagal memuat info")
        val ewash: String = (if (infoJson.isJsonObject) infoJson.asJsonObject.str("ewash") else null)
            ?: return StreamResponse(message = "Gagal mendapatkan token video")
        // 2. panggil /drama/{id} dengan ewash
        val epc = cfg.episode ?: return StreamResponse(message = "Stream tidak tersedia")
        val (url, params) = buildUrl(cfg, epc, mapOf<String, String>("id" to rawId, "ewash" to ewash))
        val json = fetchJson(url, params) ?: return StreamResponse(message = "Gagal memuat video")
        val arr = extractArray(json)
        val target = arr?.firstOrNull { el ->
            el.isJsonObject && el.asJsonObject.int("number", "episode", "ep") == ep
        } ?: arr?.let { if (ep - 1 in 0 until it.size()) it[ep - 1] else null }
        val vurl = target?.let { extractVideoUrl(it) } ?: extractVideoUrl(json)
            ?: return StreamResponse(message = "Video episode $ep tidak tersedia")
        return StreamResponse(url = vurl, type = if (vurl.contains(".m3u8")) "hls" else "mp4")
    }

    /** Fitur dinamis per provider untuk tab Eksplor: daftar endpoint dari katalog. */
    fun getProviderFeatures(slug: String): ProviderFeaturesResponse {        val cfg = SonzaixProviders.bySlug[slug] ?: return ProviderFeaturesResponse(provider = slug)
        val feats = mutableListOf<ProviderFeature>()
        // tambahkan endpoint list utama sebagai fitur
        feats.add(
            ProviderFeature(
                name = "Beranda",
                path = cfg.list.path,
                url = SonzaixApi.fullUrl("/api/${cfg.slug}${cfg.list.path}"),
                description = "Konten utama ${cfg.name}"
            )
        )
        cfg.search?.let {
            feats.add(
                ProviderFeature(
                    name = "Pencarian",
                    path = it.path,
                    url = SonzaixApi.fullUrl("/api/${cfg.slug}${it.path}"),
                    description = com.clipku.drama.ui.theme.Str.searchIn(cfg.name)
                )
            )
        }
        return ProviderFeaturesResponse(provider = cfg.name, base = SonzaixApi.BASE_URL, features = feats)
    }

    private fun parseComposite(composite: String): Pair<String, String>? {
        val idx = composite.indexOf(':')
        if (idx <= 0) return null
        return composite.substring(0, idx) to composite.substring(idx + 1)
    }

    /** Fetch mentah untuk fitur eksplor (mengembalikan string JSON). */
    suspend fun getRawJson(url: String): String? {
        return try {
            SonzaixClient.api.get(url).string()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            null
        }
    }
}
