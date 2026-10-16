package com.clipku.drama

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.clipku.drama.data.notify.EpisodeCheckWorker
import java.util.concurrent.TimeUnit

class ClipkuApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        // cek episode baru judul favorit tiap ±6 jam (hanya saat ada koneksi)
        try {
            val req = PeriodicWorkRequestBuilder<EpisodeCheckWorker>(6, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "episode_check",
                ExistingPeriodicWorkPolicy.KEEP,
                req
            )
        } catch (_: Exception) { }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil"))
                    .maxSizePercent(0.05)
                    .build()
            }
            .build()
    }
}
