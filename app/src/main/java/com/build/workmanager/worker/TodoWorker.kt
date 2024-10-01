package com.build.workmanager.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.RingtoneManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.build.workmanager.R
import kotlinx.coroutines.delay
import kotlin.random.Random

class TodoWorker(private var context: Context, private var workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val reminderName = workerParams.inputData.getString(REMINDER_NAME_DATA)
        val minutes = workerParams.inputData.getString(MINUTES_DATA)?.toLongOrNull() ?: 1
        val isPeriodic = workerParams.inputData.getBoolean(IS_PERIODIC_DATA, false)
        val milliseconds: Long = minutes * 60 * 1000

        try {
            if (isPeriodic.not()) {
                delay(milliseconds)
            }
            showReminderNotification("Reminder, your $reminderName. Don't forget, hurry up!")
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    private fun showReminderNotification(title: String?) {
        val notificationId = Random.nextInt(1, 99)
        val channelId = "thing"

        val channel = NotificationChannel(channelId, channelId, NotificationManager.IMPORTANCE_HIGH)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setColor(ContextCompat.getColor(context, R.color.purple_200))
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setPriority(NotificationManager.IMPORTANCE_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(notificationId, notification)
    }

    companion object {
         const val REMINDER_NAME_DATA = "reminder_name_data"
         const val MINUTES_DATA = "minutes_data"
         const val IS_PERIODIC_DATA = "is_periodic_data"
    }
}