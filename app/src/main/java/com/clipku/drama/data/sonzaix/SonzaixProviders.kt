package com.clipku.drama.data.sonzaix

/**
 * Konfigurasi endpoint per provider di api.sonzaix.codes.
 * URL penuh = https://api.sonzaix.codes/api/{slug}{path}
 * Template path mendukung placeholder {id}, {ep}, {q}, {page}.
 */
data class SxEndpoint(
    /** path template, cth "/api/v1/foryou" atau "/api/v1/drama/{id}/episode/{ep}" */
    val path: String,
    /** query params default */
    val params: Map<String, String> = mapOf("lang" to "id"),
    /** nama param pagination ("page", "pageNum", "pageNo") atau null bila tak ada */
    val pageParam: String? = "page",
    /** halaman pertama: 0 atau 1 */
    val pageStart: Int = 1,
    /** nama param search */
    val queryParam: String? = null,
)

data class SxProviderConfig(
    val slug: String,
    val name: String,
    /** endpoint daftar/home untuk Beranda */
    val list: SxEndpoint,
    /** endpoint search (queryParam wajib diisi) */
    val search: SxEndpoint?,
    /** endpoint detail, wajib ada {id} */
    val detail: SxEndpoint?,
    /** endpoint video episode, wajib ada {id} dan {ep} */
    val episode: SxEndpoint?,
    /** endpoint ranking/populer (opsional) */
    val rank: SxEndpoint? = null,
)

/**
 * Semua 40 provider sonzaix. Disusun dari katalog api.sonzaix.codes (Okt 2026).
 * Provider yang endpoint-nya belum pasti diberi list dari endpoint home generik;
 * bila gagal saat runtime, repository mengembalikan kosong dengan graceful.
 */
object SonzaixProviders {
    val all: List<SxProviderConfig> = listOf(
        SxProviderConfig(
            slug = "dramabite", name = "DramaBite",
            list = SxEndpoint("/api/v1/foryou", mapOf("lang" to "id"), "page", 0),
            search = SxEndpoint("/api/v1/search", mapOf("lang" to "id", "limit" to "20"), null, queryParam = "q"),
            detail = SxEndpoint("/api/v1/drama/{id}", mapOf("lang" to "id")),
            episode = SxEndpoint("/api/v1/drama/{id}/episode/{ep}", mapOf("lang" to "id")),
        ),
        SxProviderConfig(
            slug = "freereels", name = "FreeReels",
            list = SxEndpoint("/api/v1/foryou", mapOf("lang" to "id"), "page", 1),
            search = SxEndpoint("/api/v1/search", mapOf("lang" to "id", "limit" to "20"), null, queryParam = "q"),
            detail = SxEndpoint("/api/v1/dramas/{id}", mapOf("lang" to "id")),
            episode = SxEndpoint("/api/v1/dramas/{id}/play/{ep}", mapOf("lang" to "id")),
        ),
        SxProviderConfig(
            slug = "reelshort", name = "ReelShort",
            list = SxEndpoint("/api/v1/foryou", mapOf("lang" to "id")),
            search = SxEndpoint("/api/v1/search", mapOf("lang" to "id"), "page", 1, queryParam = "q"),
            detail = SxEndpoint("/api/v1/book/{id}", mapOf("lang" to "id")),
            // reelshort: chapter list dulu baru video per chapter
            episode = SxEndpoint("/api/v1/book/{id}/chapter/{ep}/video", mapOf("lang" to "id")),
        ),
        SxProviderConfig(
            slug = "netshort", name = "NetShort",
            list = SxEndpoint("/api/v1/feed/{page}", mapOf("lang" to "id"), "page", 1),
            search = SxEndpoint("/api/v1/search/{q}/{page}", mapOf("lang" to "id"), "page", 1, queryParam = "q"),
            detail = SxEndpoint("/api/v1/detail/{id}", mapOf("lang" to "id")),
            episode = SxEndpoint("/api/v1/episode/{id}/{ep}", mapOf("lang" to "id")),
        ),
        SxProviderConfig(
            slug = "stardusttv", name = "StardustTV",
            list = SxEndpoint("/api/v1/homepage", mapOf("lang" to "id")),
            search = SxEndpoint("/api/v1/search", mapOf("lang" to "id", "page" to "1", "page_size" to "20"), null, queryParam = "q"),
            detail = SxEndpoint("/api/v1/video/{id}", mapOf("lang" to "id")),
            episode = SxEndpoint("/api/v1/video/{id}/episode/{ep}", mapOf("lang" to "id")),
        ),
        SxProviderConfig(
            slug = "shortmax", name = "ShortMax",
            list = SxEndpoint("/api/v1/foryou", mapOf("lang" to "id"), "page", 1),
            search = SxEndpoint("/api/v1/search", mapOf("lang" to "id"), "page", 1, queryParam = "q"),
            detail = SxEndpoint("/api/v1/detail/{id}", mapOf("lang" to "id")),
            episode = SxEndpoint("/api/v1/play/{id}", mapOf("lang" to "id", "ep" to "{ep}")),
            rank = SxEndpoint("/api/v1/feed/ranked", mapOf("lang" to "id")),
        ),
        SxProviderConfig(
            slug = "drakorid", name = "DrakorID",
            list = SxEndpoint("/drama/terbaru", mapOf("limit" to "30"), "page", 1),
            search = SxEndpoint("/drama/search", mapOf("limit" to "30"), "page", 1, queryParam = "q"),
            detail = SxEndpoint("/drama/detail", mapOf("id" to "{id}")),
            episode = SxEndpoint("/drama/stream", mapOf("streaming" to "{stream}")),
        ),
        SxProviderConfig(
            slug = "moviebox", name = "MovieBox",
            list = SxEndpoint("/homepage", mapOf("tabId" to "1")),
            search = SxEndpoint("/search", mapOf("perPage" to "20"), "page", 1, queryParam = "q"),
            detail = SxEndpoint("/detail", mapOf("subjectId" to "{id}")),
            episode = SxEndpoint("/download-series", mapOf("subjectId" to "{id}", "epTo" to "{ep}")),
        ),
        SxProviderConfig(
            slug = "melolo", name = "Melolo",
            list = SxEndpoint("/api/v1/bookmall", mapOf("lang" to "id")),
            search = SxEndpoint("/api/v1/search", mapOf("lang" to "id", "limit" to "20"), "offset", 0, queryParam = "q"),
            detail = SxEndpoint("/api/v1/series", mapOf("lang" to "id", "id" to "{id}")),
            episode = SxEndpoint("/api/v1/multi-video", mapOf("lang" to "id", "id" to "{id}")),
            rank = SxEndpoint("/api/v1/rank", mapOf("lang" to "id")),
        ),
        SxProviderConfig(
            slug = "bilitv", name = "BiliTV",
            list = SxEndpoint("/api/v1/home", mapOf("lang" to "id"), "page", 1),
            search = SxEndpoint("/api/v1/search", mapOf("lang" to "id"), null, queryParam = "q"),
            detail = SxEndpoint("/api/v1/drama/{id}", mapOf("lang" to "id")),
            episode = SxEndpoint("/api/v1/drama/{id}/episode/{ep}", mapOf("lang" to "id", "quality" to "{quality}")),
        ),
        SxProviderConfig(
            slug = "bonustv", name = "BonusTV",
            list = SxEndpoint("/api/recommend", mapOf("lang" to "id", "page_size" to "20"), "page", 1),
            search = SxEndpoint("/api/search", mapOf("lang" to "id", "page_size" to "20"), "page", 1, queryParam = "q"),
            detail = SxEndpoint("/api/video/{id}", mapOf("lang" to "id")),
            episode = SxEndpoint("/api/stream/{id}", mapOf("lang" to "id", "episode_num" to "{ep}", "resolution" to "720p")),
        ),
        SxProviderConfig(
            slug = "cashdrama", name = "CashDrama",
            list = SxEndpoint("/api/v1/home", mapOf("lang" to "id", "pageSize" to "20"), "page", 1),
            search = SxEndpoint("/api/v1/search", mapOf("lang" to "id"), "page", 1, queryParam = "q"),
            detail = SxEndpoint("/api/v1/drama/{id}", mapOf("lang" to "id")),
            episode = SxEndpoint("/api/v1/play/{id}/{ep}", mapOf("lang" to "id")),
        ),
        SxProviderConfig(
            slug = "dotdrama", name = "DotDrama",
            list = SxEndpoint("/api/v1/dramas", mapOf("lang" to "id", "limit" to "50"), "page", 1),
            search = null,
            detail = SxEndpoint("/api/v1/dramas/{id}"),
            episode = null, // detail sudah berisi video URL per episode
        ),
        SxProviderConfig(
            slug = "dramabox", name = "DramaBox",
            list = SxEndpoint("/home", mapOf("lang" to "id"), "page", 1),
            search = SxEndpoint("/search", mapOf("lang" to "id"), "page", 1, queryParam = "q"),
            detail = SxEndpoint("/detail/{id}", mapOf("lang" to "id")),
            episode = SxEndpoint("/stream", mapOf("lang" to "id", "bookId" to "{id}", "chapterIndex" to "{ep}")),
        ),
        SxProviderConfig(
            slug = "dramanova", name = "DramaNova",
            list = SxEndpoint("/discover/feed", mapOf("pageSize" to "20"), "pageNum", 1),
            search = SxEndpoint("/drama/search", mapOf("pageSize" to "20"), "pageNum", 1, queryParam = "q"),
            detail = SxEndpoint("/detail/{id}"),
            episode = SxEndpoint("/unlock/{id}/{ep}"),
        ),
        SxProviderConfig(
            slug = "dramapops", name = "DramaPops",
            list = SxEndpoint("/api/homepage", mapOf("lang" to "id")),
            search = SxEndpoint("/api/drama/search", mapOf("lang" to "id", "limit" to "20"), null, queryParam = "q"),
            detail = SxEndpoint("/api/drama/{id}", mapOf("lang" to "id")),
            episode = SxEndpoint("/api/drama/{id}/{ep}", mapOf("lang" to "id", "quality" to "720p")),
        ),
        SxProviderConfig(
            slug = "dramarush", name = "DramaRush",
            list = SxEndpoint("/api/v1/tabs/0", mapOf("lang" to "id")),
            search = SxEndpoint("/api/v1/search/{q}", mapOf("lang" to "id"), queryParam = "q"),
            detail = SxEndpoint("/api/v1/drama/{id}", mapOf("lang" to "id")),
            episode = SxEndpoint("/api/v1/play/{id}/{ep}", mapOf("lang" to "id")),
        ),
        SxProviderConfig(
            slug = "flareflow", name = "FlareFlow",
            list = SxEndpoint("/api/home", mapOf("lang" to "id")),
            search = SxEndpoint("/api/search", mapOf("lang" to "id"), "page", 1, queryParam = "keyword"),
            detail = SxEndpoint("/api/drama/{id}", mapOf("lang" to "id")),
            episode = SxEndpoint("/api/drama/{id}/play", mapOf("lang" to "id", "ep" to "{ep}")),
        ),
        SxProviderConfig(
            slug = "flextv", name = "FlexTV",
            list = SxEndpoint("/api/v1/tabs/1", mapOf("lang" to "id"), "page", 1),
            search = SxEndpoint("/api/v1/search", mapOf("lang" to "id"), "page", 1, queryParam = "q"),
            detail = SxEndpoint("/api/v1/series/{id}", mapOf("lang" to "id")),
            episode = SxEndpoint("/api/v1/play/{id}/{ep}", mapOf("lang" to "id")),
            rank = SxEndpoint("/api/v1/ranking", mapOf("lang" to "id")),
        ),
        SxProviderConfig(
            slug = "flickreels", name = "FlickReels",
            list = SxEndpoint("/api/v1/for-you", mapOf("lang" to "id", "page_size" to "20"), "page", 1),
            search = SxEndpoint("/api/v1/search", mapOf("lang" to "id"), null, queryParam = "keyword"),
            detail = SxEndpoint("/api/v1/chapters/{id}", mapOf("lang" to "id")),
            episode = SxEndpoint("/api/v1/stream/{id}/{ep}", mapOf("lang" to "id")),
            rank = SxEndpoint("/api/v1/hot-rank", mapOf("lang" to "id", "page_size" to "20")),
        ),
        SxProviderConfig(
            slug = "luminareels", name = "LuminaReels",
            list = SxEndpoint("/api/drama/list", mapOf("lang" to "id", "page_size" to "20"), "page", 1),
            search = SxEndpoint("/api/drama/search", mapOf("lang" to "id", "page_size" to "20"), "page", 1, queryParam = "q"),
            detail = SxEndpoint("/api/drama/detail", mapOf("lang" to "id", "id" to "{id}")),
            episode = SxEndpoint("/api/stream/{id}/{ep}/master.m3u8", mapOf("lang" to "id")),
        ),
        SxProviderConfig(
            slug = "meloshort", name = "MeloShort",
            list = SxEndpoint("/api/v1/dramas/discover", mapOf("lang" to "id", "limit" to "20"), "page", 1),
            search = SxEndpoint("/api/v1/dramas/search", mapOf("lang" to "id"), null, queryParam = "q"),
            detail = SxEndpoint("/api/v1/dramas/{id}/episodes", mapOf("lang" to "id", "limit" to "100"), "page", 1),
            episode = SxEndpoint("/api/v1/dramas/{id}/episodes/{ep}", mapOf("lang" to "id")),
        ),
        SxProviderConfig(
            slug = "microdrama", name = "MicroDrama",
            list = SxEndpoint("/api/v1/dramas", mapOf("lang" to "id", "limit" to "20")),
            search = SxEndpoint("/api/v1/dramas/search", mapOf("lang" to "id"), null, queryParam = "q"),
            detail = SxEndpoint("/api/v1/dramas/{id}"),
            episode = null,
        ),
        SxProviderConfig(
            slug = "moboreels", name = "MoboReels",
            list = SxEndpoint("/api/hotList", mapOf("listId" to "10", "langId" to "3")),
            search = SxEndpoint("/api/search", mapOf("pageSize" to "20", "langId" to "3"), "pageNo", 1, queryParam = "searchKey"),
            detail = SxEndpoint("/api/seriesDetail", mapOf("seriesId" to "{id}", "langId" to "3")),
            episode = SxEndpoint("/api/video", mapOf("seriesId" to "{id}", "episNum" to "{ep}", "langId" to "3")),
        ),
        SxProviderConfig(
            slug = "playlet", name = "Playlet",
            list = SxEndpoint("/shorts/recommend", mapOf("lang" to "id", "pageSize" to "20"), "page", 1),
            search = SxEndpoint("/search", mapOf("lang" to "id", "pageSize" to "20"), "page", 1, queryParam = "keyword"),
            detail = SxEndpoint("/theater/{id}", mapOf("lang" to "id")),
            episode = SxEndpoint("/theater/{id}/open/{ep}/playlist.m3u8"),
        ),
        SxProviderConfig(
            slug = "radreels", name = "RadReels",
            list = SxEndpoint("/api/v1/foryou", mapOf("lang" to "id"), "page", 1),
            search = SxEndpoint("/api/v1/search/{q}", mapOf("lang" to "id"), "page", 1, queryParam = "q"),
            detail = SxEndpoint("/api/v1/drama/{id}", mapOf("lang" to "id"), "page", 1),
            episode = SxEndpoint("/api/v1/video/{id}/{ep}", mapOf("lang" to "id")),
            rank = SxEndpoint("/api/v1/ranking", mapOf("lang" to "id")),
        ),
        SxProviderConfig(
            slug = "rapidtv", name = "RapidTV",
            list = SxEndpoint("/api/v1/dramas", mapOf("lang" to "id", "size" to "20"), "page", 1),
            search = SxEndpoint("/api/v1/search", mapOf("lang" to "id", "size" to "20"), "page", 1, queryParam = "q"),
            detail = SxEndpoint("/api/v1/dramas/{id}", mapOf("lang" to "id")),
            episode = SxEndpoint("/api/v1/dramas/{id}/episodes", mapOf("lang" to "id")),
        ),
        SxProviderConfig(
            slug = "raptdrama", name = "RaptDrama",
            list = SxEndpoint("/open/recommend", mapOf("lang" to "id"), "page", 1),
            search = SxEndpoint("/open/search", mapOf("lang" to "id"), "page", 1, queryParam = "keyword"),
            detail = SxEndpoint("/video/getchapters", mapOf("lang" to "id", "vid" to "{id}")),
            episode = SxEndpoint("/video/playlist", mapOf("lang" to "id", "vid" to "{id}")),
        ),
        SxProviderConfig(
            slug = "reelbuzz", name = "ReelBuzz",
            list = SxEndpoint("/api/drama/module/recommend", mapOf("language" to "id", "pageSize" to "20"), "pageNum", 1),
            search = SxEndpoint("/api/search", mapOf("language" to "id"), null, queryParam = "keyword"),
            detail = SxEndpoint("/api/drama/detail", mapOf("language" to "id", "dramaId" to "{id}")),
            episode = SxEndpoint("/api/drama/video/{id}/{ep}", mapOf("language" to "id")),
        ),
        SxProviderConfig(
            slug = "sereal", name = "Sereal",
            list = SxEndpoint("/api/index/home/{page}", mapOf("lang" to "id"), "page", 1),
            search = SxEndpoint("/api/index/search/{q}", mapOf("lang" to "id"), queryParam = "q"),
            detail = SxEndpoint("/api/content/detail", mapOf("lang" to "id", "contentId" to "{id}")),
            episode = SxEndpoint("/api/watch/{id}/{ep}", mapOf("lang" to "id")),
        ),
        SxProviderConfig(
            slug = "shortbox", name = "ShortBox",
            list = SxEndpoint("/api/recommend", mapOf("languages" to "id")),
            search = SxEndpoint("/api/search", mapOf("languages" to "id", "page_size" to "20"), "page", 1, queryParam = "q"),
            detail = SxEndpoint("/api/info/{id}", mapOf("languages" to "id")),
            episode = SxEndpoint("/api/stream/{id}/{ep}", mapOf("quality" to "720p")),
        ),
        SxProviderConfig(
            slug = "shorten", name = "Shorten",
            list = SxEndpoint("/api/v1/releases", mapOf("perPage" to "20"), "page", 1),
            search = null,
            detail = SxEndpoint("/api/v1/series/{id}"),
            episode = null,
        ),
        SxProviderConfig(
            slug = "shortsky", name = "ShortSky",
            list = SxEndpoint("/api/home", mapOf("lang" to "id")),
            search = SxEndpoint("/api/search", mapOf("lang" to "id"), "page", 1, queryParam = "q"),
            detail = SxEndpoint("/api/drama/{id}", mapOf("lang" to "id")),
            episode = SxEndpoint("/api/drama/{id}/episode/{ep}", mapOf("lang" to "id")),
        ),
        SxProviderConfig(
            slug = "shortwave", name = "ShortWave",
            list = SxEndpoint("/api/top", mapOf("lang" to "id")),
            search = SxEndpoint("/api/search/{q}", mapOf("lang" to "id"), queryParam = "q"),
            detail = SxEndpoint("/api/drama/{id}", mapOf("lang" to "id")),
            episode = SxEndpoint("/api/stream/{id}/{ep}", mapOf("lang" to "id")),
            rank = SxEndpoint("/api/rankings", mapOf("lang" to "id")),
        ),
        SxProviderConfig(
            slug = "shotshort", name = "ShotShort",
            list = SxEndpoint("/api/popular", mapOf("lang" to "id"), "page", 1),
            search = SxEndpoint("/api/search", mapOf("lang" to "id"), "page", 1, queryParam = "q"),
            detail = SxEndpoint("/api/book/{id}", mapOf("lang" to "id")),
            episode = SxEndpoint("/api/book/{id}/chapter/{ep}", mapOf("lang" to "id")),
        ),
        SxProviderConfig(
            slug = "snackshort", name = "SnackShort",
            list = SxEndpoint("/api/v1/home", mapOf("lang" to "id")),
            search = SxEndpoint("/api/v1/search", mapOf("lang" to "id", "limit" to "20"), "page", 1, queryParam = "q"),
            detail = SxEndpoint("/api/v1/book/{id}", mapOf("lang" to "id")),
            episode = SxEndpoint("/api/v1/book/{id}/episode/{ep}", mapOf("lang" to "id")),
        ),
        SxProviderConfig(
            slug = "sodareels", name = "SodaReels",
            list = SxEndpoint("/api/v1/home", mapOf("lang" to "id", "count" to "20"), "page", 1),
            search = SxEndpoint("/api/v1/search", mapOf("lang" to "id"), null, queryParam = "q"),
            detail = SxEndpoint("/api/v1/info/{id}"),
            episode = SxEndpoint("/api/v1/drama/{id}", mapOf("ewash" to "{ewash}")),
        ),
        SxProviderConfig(
            slug = "starshort", name = "StarShort",
            list = SxEndpoint("/api/v1/dramas", mapOf("lang" to "4")),
            search = SxEndpoint("/api/v1/dramas/search", mapOf("lang" to "4"), null, queryParam = "q"),
            detail = SxEndpoint("/api/v1/dramas/{id}", mapOf("lang" to "4")),
            episode = SxEndpoint("/api/v1/dramas/{id}/episodes/{ep}", mapOf("lang" to "4")),
        ),
        SxProviderConfig(
            slug = "vigloo", name = "Vigloo",
            list = SxEndpoint("/api/v1/rank", mapOf("lang" to "id")),
            search = SxEndpoint("/api/v1/search", mapOf("lang" to "id", "limit" to "20"), null, queryParam = "q"),
            detail = SxEndpoint("/api/v1/drama/{id}", mapOf("lang" to "id")),
            episode = SxEndpoint("/api/v1/stream", mapOf("seasonId" to "{id}", "ep" to "{ep}")),
        ),
        SxProviderConfig(
            slug = "wetv", name = "WeTV",
            list = SxEndpoint("/api/feed", mapOf("lang" to "id", "country" to "id")),
            search = SxEndpoint("/api/search", mapOf("lang" to "id", "country" to "id"), null, queryParam = "keyword"),
            detail = SxEndpoint("/api/detail", mapOf("lang" to "id", "country" to "id", "cid" to "{id}")),
            episode = SxEndpoint("/api/play", mapOf("lang" to "id", "country" to "id", "cid" to "{id}", "vid" to "{ep}")),
        ),
    )

    val bySlug: Map<String, SxProviderConfig> = all.associateBy { it.slug }
}
