package com.clipku.drama.data.model

data class Provider(
    val providerName: String = "",
    val providerSlug: String = "",
    val providerType: String = "",
    val logoUrl: String? = null
)

data class DramaItem(
    val id: String = "",
    val title: String = "",
    val slug: String = "",
    val posterUrl: String? = null,
    val providerName: String = "",
    val type: String = "",
    val rating: Double = 0.0,
    val viewCount: Long = 0,
    val providerViewCount: Long = 0,
    val episodeCount: Int = 0,
    val genres: List<String> = emptyList(),
    val synopsis: String = ""
)

data class ContentsResponse(
    val items: List<DramaItem> = emptyList(),
    val nextCursor: String? = null,
    val hasMore: Boolean? = null
) {
    // API tidak selalu mengembalikan hasMore; fallback ke nextCursor
    val effectiveHasMore: Boolean get() = hasMore ?: (nextCursor != null)
}

data class Episode(
    val id: String = "",
    val episodeNumber: Int = 0,
    val title: String? = null
)

data class DramaDetail(
    val id: String = "",
    val title: String = "",
    val slug: String = "",
    val description: String? = null,
    val posterUrl: String? = null,
    val bannerUrl: String? = null,
    val providerName: String = "",
    val providerSlug: String = "",
    val type: String = "",
    val rating: Double = 0.0,
    val viewCount: Long = 0,
    val genres: List<String> = emptyList(),
    val status: String? = null,
    val episodeCount: Int = 0,
    val episodes: List<Episode> = emptyList()
) {
    // jumlah episode aktual dari array episodes
    val totalEpisodes: Int get() = if (episodes.isNotEmpty()) episodes.size else episodeCount
}

data class VideoQuality(
    val label: String,
    val url: String
)

data class StreamResponse(
    val url: String? = null,
    val type: String? = null,
    val message: String? = null,
    val fallbackFrom: String? = null,
    val qualities: List<VideoQuality> = emptyList()
)

// ===== Provider dynamic features =====
data class ProviderFeaturesResponse(
    val provider: String = "",
    val base: String = "",
    val features: List<ProviderFeature> = emptyList()
)

data class ProviderFeature(
    val name: String = "",
    val path: String = "",
    val url: String = "",
    val description: String? = null,
    val params: List<Map<String, String>> = emptyList()
)

// Item generik hasil parsing feature provider (banners, trending, dsb)
data class FeatureItem(
    val id: String = "",
    val title: String = "",
    val imageUrl: String? = null
)
