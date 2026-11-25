package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object AdminNotificationHelper {
    const val CHANNEL_ID_DEPOSITS = "admin_deposit_alerts"
    const val EXTRA_OPEN_ADMIN_DEPOSITS = "open_admin_deposits"
    const val TOPIC_ADMIN_DEPOSITS = "admin_deposits"

    private const val PREFS_ALERTS = "firebattle_admin_alerts"
    private const val KEY_NOTIFIED_IDS = "notified_deposit_ids"
    private const val PREFS_APP = "firebattle_prefs"
    private const val KEY_IS_ADMIN_DEVICE = "is_admin_device"

    fun setAdminDeviceFlag(context: Context, isAdmin: Boolean) {
        context.applicationContext
            .getSharedPreferences(PREFS_APP, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_IS_ADMIN_DEVICE, isAdmin)
            .apply()
    }

    fun isAdminDevice(context: Context): Boolean {
        return context.applicationContext
            .getSharedPreferences(PREFS_APP, Context.MODE_PRIVATE)
            .getBoolean(KEY_IS_ADMIN_DEVICE, false)
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        val existing = manager.getNotificationChannel(CHANNEL_ID_DEPOSITS)
        if (existing != null && existing.importance >= NotificationManager.IMPORTANCE_HIGH) return

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audioAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val channel = NotificationChannel(
            CHANNEL_ID_DEPOSITS,
            "Admin Add Cash Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Instant top alerts whenever a player submits an Add Money UPI request."
            enableLights(true)
            enableVibration(true)
            vibrationPattern = longArrayOf(0L, 250L, 120L, 250L)
            setSound(soundUri, audioAttrs)
        }
        manager.createNotificationChannel(channel)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun markSeenQuietly(context: Context, depositIds: Collection<String>) {
        if (depositIds.isEmpty()) return
        val prefs = context.applicationContext.getSharedPreferences(PREFS_ALERTS, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY_NOTIFIED_IDS, emptySet()).orEmpty().toMutableList()
        var changed = false
        for (id in depositIds) {
            if (id.isNotBlank() && id !in current) {
                current.add(id)
                changed = true
            }
        }
        if (changed) {
            prefs.edit()
                .putStringSet(KEY_NOTIFIED_IDS, current.takeLast(80).toSet())
                .apply()
        }
    }

    private fun tryClaimNotification(context: Context, depositId: String): Boolean {
        if (depositId.isBlank()) return true
        val prefs = context.applicationContext.getSharedPreferences(PREFS_ALERTS, Context.MODE_PRIVATE)
        val existing = prefs.getStringSet(KEY_NOTIFIED_IDS, emptySet()).orEmpty()
        if (depositId in existing) return false
        val updated = (existing.toList() + depositId).takeLast(80).toSet()
        prefs.edit().putStringSet(KEY_NOTIFIED_IDS, updated).apply()
        return true
    }

    fun showDepositAlert(
        context: Context,
        depositId: String,
        ign: String,
        phone: String,
        amount: Long,
        utr: String
    ): Boolean {
        val appContext = context.applicationContext
        ensureChannel(appContext)
        if (!hasNotificationPermission(appContext)) return false
        if (!tryClaimNotification(appContext, depositId)) return false

        val cleanIgn = ign.trim().ifBlank { "Player" }
        val cleanPhone = phone.trim()
        val phoneText = if (cleanPhone.isNotBlank()) " (+91 $cleanPhone)" else ""
        val refText = utr.trim().ifBlank { depositId }

        val title = "💰 New Add Cash: ₹$amount"
        val body = "$cleanIgn$phoneText paid ₹$amount • Ref: $refText\nTap to approve in Admin Panel."

        val openIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_ADMIN_DEPOSITS, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            depositId.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID_DEPOSITS)
            .setSmallIcon(R.drawable.ic_notification_alert)
            .setContentTitle(title)
            .setContentText("$cleanIgn$phoneText • ₹$amount (Tap to Approve)")
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationId = if (depositId.isNotBlank()) {
            2000 + (depositId.hashCode() and 0x0FFF_FFFF)
        } else {
            (System.currentTimeMillis() and 0x0FFF_FFFF).toInt()
        }

        return try {
            NotificationManagerCompat.from(appContext).notify(notificationId, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun showTestAlert(context: Context): Boolean {
        val appContext = context.applicationContext
        ensureChannel(appContext)
        if (!hasNotificationPermission(appContext)) return false

        val openIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_ADMIN_DEPOSITS, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            9991,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID_DEPOSITS)
            .setSmallIcon(R.drawable.ic_notification_alert)
            .setContentTitle("🔔 Firebattle Admin Alerts Active")
            .setContentText("New Add Cash requests will pop up here at the top of your phone.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return try {
            NotificationManagerCompat.from(appContext).notify(9991, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }
}
