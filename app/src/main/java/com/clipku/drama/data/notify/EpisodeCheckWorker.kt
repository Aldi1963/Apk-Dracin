package com.clipku.drama.data.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.clipku.drama.data.history.FavoriteStore
import com.clipku.drama.data.sonzaix.SonzaixRepository
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Cek berkala (±6 jam via WorkManager): bandingkan jumlah episode terbaru
 * tiap judul favorit dengan yang tersimpan; kirim notifikasi lokal bila bertambah.
 * Kegagalan satu judul tidak menggagalkan yang lain.
 */
class EpisodeCheckWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            FavoriteStore.init(applicationContext)
            val favs = FavoriteStore.items.value
            for (f in favs) {
                try {
                    val total = withTimeoutOrNull(20000) {
                        SonzaixRepository.getDetail(f.contentId).totalEpisodes
                    } ?: continue
                    if (total > f.totalEp && total > 0) {
                        FavoriteStore.updateTotalEp(f.contentId, total)
                        NotifyHelper.showEpisodeUpdate(applicationContext, f.title, total)
                    }
                } catch (_: Exception) {
                    // lewati judul ini, lanjut ke berikutnya
                }
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
