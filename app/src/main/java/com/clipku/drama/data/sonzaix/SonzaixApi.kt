package com.clipku.drama.data.sonzaix

import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.QueryMap
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

/**
 * API client langsung ke https://api.sonzaix.codes/
 * Semua endpoint dipanggil via URL dinamis karena tiap provider punya path berbeda.
 */
interface SonzaixApi {
    @GET
    suspend fun get(
        @Url url: String,
        @QueryMap params: Map<String, String> = emptyMap()
    ): ResponseBody

    companion object {
        const val BASE_URL = "https://api.sonzaix.codes/"

        // User-Agent browser agar tidak diblokir gateway sonzaix
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Infinix X6833B) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"

        fun fullUrl(path: String): String {
            val p = path.trimStart('/')
            return BASE_URL.trimEnd('/') + "/" + p
        }

        /** Samakan perilaku lama: URL absolut dipakai langsung, relatif digabung ke base. */
        fun resolveUrl(path: String?): String? {
            if (path.isNullOrBlank()) return null
            if (path.startsWith("http://") || path.startsWith("https://")) return path
            return fullUrl(path)
        }
    }
}

object SonzaixClient {
    val api: SonzaixApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("User-Agent", SonzaixApi.USER_AGENT)
                    .header("Accept", "application/json")
                    .header("Referer", "https://api.sonzaix.codes/")
                    .build()
                chain.proceed(req)
            }
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        Retrofit.Builder()
            .baseUrl(SonzaixApi.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SonzaixApi::class.java)
    }
}
