package com.fancie.aicompanion

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class CompanionNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.lunakaiPrefs()
        if (!prefs.getBoolean("settings_companion_notifications_enabled", false)) return
        if (CompanionNotificationScheduler.isQuietHoursNow(prefs.getString("settings_notification_quiet_hours", "10:00 PM - 8:00 AM").orEmpty())) {
            CompanionNotificationScheduler.scheduleNext(context)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        ensureNotificationChannel(context)
        val type = prefs.getString("settings_notification_type", "Companion attention message").orEmpty()
        val adultPreview = prefs.getBoolean("settings_adult_notification_previews", false)
        val roleplayEnabled = prefs.getBoolean("settings_roleplay_notifications_enabled", false)
        val title = if (type == "Live Companion call invitation") "Your companion is calling" else "LunaKai"
        val body = when {
            type == "Live Companion call invitation" -> "Your companion is calling"
            type == "Roleplay-mode message" && roleplayEnabled && adultPreview -> "Your companion wants your attention"
            type == "Roleplay-mode message" -> "LunaKai wants your attention"
            type == "Wellness check-in" -> "A gentle check-in is ready"
            type == "Miss-you/check-on-you message" -> "Your companion is thinking of you"
            else -> "Your companion wants your attention"
        }
        val destination = if (type == "Live Companion call invitation") "incoming_call" else "chat"
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("lunakai_route", destination)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            2401,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(if (adultPreview) NotificationCompat.VISIBILITY_PRIVATE else NotificationCompat.VISIBILITY_SECRET)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        CompanionNotificationScheduler.scheduleNext(context)
    }

    private fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "LunaKai companion notifications",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Local opt-in companion reminders and call invitations."
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "lunakai_companion_local"
        const val NOTIFICATION_ID = 2401
    }
}

object CompanionNotificationScheduler {
    fun scheduleNext(context: Context) {
        val prefs = context.lunakaiPrefs()
        if (!prefs.getBoolean("settings_companion_notifications_enabled", false)) {
            cancel(context)
            return
        }
        val frequency = prefs.getString("settings_notification_frequency", "Daily").orEmpty()
        val delayMillis = when (frequency) {
            "Every 3 hours" -> 3L * 60L * 60L * 1000L
            "Twice daily" -> 12L * 60L * 60L * 1000L
            "Weekly" -> 7L * 24L * 60L * 60L * 1000L
            else -> 24L * 60L * 60L * 1000L
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + delayMillis,
            pendingIntent(context),
        )
    }

    fun scheduleIncomingCall(context: Context, delayMillis: Long = 10L * 60L * 1000L) {
        context.savePref("settings_notification_type", "Live Companion call invitation")
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + delayMillis,
            pendingIntent(context),
        )
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        alarmManager.cancel(pendingIntent(context))
    }

    fun isQuietHoursNow(quietHours: String): Boolean {
        if (quietHours.isBlank()) return false
        val parts = quietHours.split("-").map { it.trim() }
        if (parts.size != 2) return false
        val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
        return runCatching {
            val start = LocalTime.parse(parts[0].uppercase(Locale.US), formatter)
            val end = LocalTime.parse(parts[1].uppercase(Locale.US), formatter)
            val now = LocalTime.now()
            if (start <= end) {
                now >= start && now < end
            } else {
                now >= start || now < end
            }
        }.getOrDefault(false)
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, CompanionNotificationReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            2401,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}