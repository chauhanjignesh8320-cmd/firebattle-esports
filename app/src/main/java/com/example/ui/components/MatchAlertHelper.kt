package com.example.ui.components

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.AppSettings
import com.example.data.model.LudoRoom
import com.example.data.model.MatchMode
import com.example.data.model.TeamFormat
import com.example.data.model.TournamentMatch
import com.example.data.model.buildReferralInviteLink

private const val ROOM_ALERTS_CHANNEL_ID = "firebattle_room_alerts"

fun hasInternetConnection(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return true
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

fun notifyRoomCredentialsReady(context: Context, match: TournamentMatch) {
    if (!hasNotificationPermission(context)) return
    if (match.roomId.isBlank()) return

    val appContext = context.applicationContext
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (manager != null && manager.getNotificationChannel(ROOM_ALERTS_CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                ROOM_ALERTS_CHANNEL_ID,
                "Match Room Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when Room ID and Password are live for your joined matches."
            }
            manager.createNotificationChannel(channel)
        }
    }

    val openIntent = Intent(appContext, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
        appContext,
        match.id.hashCode(),
        openIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val passLabel = match.roomPass.ifBlank { "Check app" }
    val notification = NotificationCompat.Builder(appContext, ROOM_ALERTS_CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle("Room Live: ${match.title}")
        .setContentText("Room ID: ${match.roomId} • Pass: $passLabel")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .build()

    runCatching {
        NotificationManagerCompat.from(appContext).notify(match.id.hashCode(), notification)
    }
}

fun notifyLudoBoardReady(
    context: Context,
    room: LudoRoom,
    title: String,
    message: String
) {
    if (!hasNotificationPermission(context)) return
    if (room.id.isBlank()) return

    val appContext = context.applicationContext
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (manager != null && manager.getNotificationChannel(ROOM_ALERTS_CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                ROOM_ALERTS_CHANNEL_ID,
                "Match Room Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when Room ID, Password or Ludo Boards are live for your joined matches."
            }
            manager.createNotificationChannel(channel)
        }
    }

    val openIntent = Intent(appContext, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
        appContext,
        ("ludo_${room.id}").hashCode(),
        openIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(appContext, ROOM_ALERTS_CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(title)
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .build()

    runCatching {
        NotificationManagerCompat.from(appContext).notify(("ludo_${room.id}").hashCode(), notification)
    }
}

fun buildMatchShareText(
    match: TournamentMatch,
    settings: AppSettings,
    referralCode: String
): String {
    val modeLabel = MatchMode.fromRaw(match.matchMode).label
    val formatLabel = TeamFormat.fromRaw(match.teamFormat).label
    val downloadUrl = buildReferralInviteLink(settings.appDownloadUrl, referralCode)
    val entryLabel = if (match.entry == 0L) "FREE (₹0)" else "₹${match.entry}"
    val refLine = if (referralCode.isNotBlank()) {
        "\nReferral Code: $referralCode"
    } else ""
    return "Join my Firebattle ${match.game} match!\n" +
        "${match.title} ($modeLabel • $formatLabel)\n" +
        "Time: ${match.matchTime} | Map: ${match.mapName}\n" +
        "Entry: $entryLabel | Prize Pool: ₹${match.prize} | Slots: ${match.joinedCount}/${match.slots}\n" +
        "Download / Open App: $downloadUrl" +
        refLine
}
