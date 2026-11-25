package com.example.notifications

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebattleMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Admin topic subscription is kept in sync automatically on sign-in in MainActivity.
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        if (!AdminNotificationHelper.isAdminDevice(applicationContext)) return
        if (Firebase.auth.currentUser == null) return

        val data = message.data
        val type = data["type"].orEmpty()
        if (type == "deposit_request" || data.containsKey("depositId")) {
            val depositId = data["depositId"].orEmpty()
            val ign = data["ign"].orEmpty().ifBlank { "Player" }
            val phone = data["phone"].orEmpty()
            val amount = data["amount"]?.toLongOrNull() ?: 0L
            val utr = data["utr"].orEmpty()
            if (amount > 0L) {
                AdminNotificationHelper.showDepositAlert(
                    context = applicationContext,
                    depositId = depositId,
                    ign = ign,
                    phone = phone,
                    amount = amount,
                    utr = utr
                )
            }
        }
    }
}
