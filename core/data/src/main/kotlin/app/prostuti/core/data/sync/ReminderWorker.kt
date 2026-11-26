package app.prostuti.core.data.sync

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.prostuti.core.data.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Local daily study reminder (works without push infrastructure and offline). */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return Result.success()
        ensureChannel(ctx)
        val launch = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
        val pending = launch?.let {
            android.app.PendingIntent.getActivity(ctx, 0, it, android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val notification = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(ctx.getString(R.string.reminder_title))
            .setContentText(ctx.getString(R.string.reminder_body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(ctx).notify(NOTIFICATION_ID, notification)
        return Result.success()
    }

    companion object {
        const val CHANNEL = "study_reminders"
        const val NOTIFICATION_ID = 1001
        fun ensureChannel(ctx: Context) {
            if (Build.VERSION.SDK_INT >= 26) {
                val nm = ctx.getSystemService(NotificationManager::class.java)
                nm.createNotificationChannel(NotificationChannel(CHANNEL, ctx.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT))
            }
        }
    }
}

@Singleton
class ReminderScheduler @Inject constructor(@ApplicationContext private val context: Context) {
    fun schedule(hour: Int?) {
        val wm = WorkManager.getInstance(context)
        if (hour == null) { wm.cancelUniqueWork(UNIQUE); return }
        val now = LocalDateTime.now()
        var next = now.with(LocalTime.of(hour, 0))
        if (!next.isAfter(now)) next = next.plusDays(1)
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(Duration.between(now, next).toMinutes(), TimeUnit.MINUTES)
            .build()
        wm.enqueueUniquePeriodicWork(UNIQUE, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    private companion object { const val UNIQUE = "study-reminder" }
}
