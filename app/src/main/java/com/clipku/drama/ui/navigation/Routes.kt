package com.clipku.drama.ui.navigation

object Routes {
    const val HOME = "home"
    const val POPULAR = "popular"
    const val EXPLORE = "explore"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val STATS = "stats"
    const val SPLASH = "splash"
    const val APP_INFO = "app_info"
    const val PROVIDERS = "providers"
    const val DETAIL = "detail/{slug}?title={title}&poster={poster}"
    const val PLAYER = "player/{contentId}/{ep}/{totalEp}/{title}/{poster}/{provider}"

    fun detail(slug: String, title: String = "", poster: String = "") =
        "detail/$slug?title=${enc(title)}&poster=${enc(poster)}"
    fun player(contentId: String, ep: Int, totalEp: Int, title: String, poster: String = "", provider: String = "") =
        "player/$contentId/$ep/$totalEp/${enc(title)}/${enc(poster)}/${enc(provider)}"

    private fun enc(s: String) = java.net.URLEncoder.encode(s, "UTF-8")
}
