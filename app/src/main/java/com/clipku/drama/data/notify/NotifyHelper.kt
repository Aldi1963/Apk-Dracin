package com.clipku.drama.data.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.clipku.drama.MainActivity

/** Notifikasi lokal in-app (bukan Telegram): episode baru untuk judul favorit. */
object NotifyHelper {
    const val CHANNEL_ID = "clipku_episode_baru"
    private const val CHANNEL_NAME = "Episode baru"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    fun showEpisodeUpdate(context: Context, title: String, totalEp: Int) {
        ensureChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context, title.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Episode baru tersedia")
            .setContentText("$title — sekarang ada $totalEp episode")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$title — sekarang ada $totalEp episode. Buka aplikasi untuk menonton.")
            )
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(title.hashCode(), notif)
    }
}
