package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class AppMode {
    PLAYER,
    ADMIN
}

enum class PlayerTab {
    TOURNAMENTS,
    WALLET,
    REFER,
    REQUESTS
}

enum class AdminTab {
    MATCHES,
    DEPOSITS,
    WITHDRAWALS,
    PLAYERS,
    SETTINGS
}

enum class GameTitle(val label: String) {
    FREE_FIRE_MAX("Free Fire MAX"),
    BGMI("BGMI");

    companion object {
        fun fromRaw(raw: String?): GameTitle =
            entries.firstOrNull { it.label == raw || it.name == raw } ?: FREE_FIRE_MAX
    }
}

enum class MatchStatus(val wireValue: String) {
    LIVE("LIVE"),
    COMPLETED("COMPLETED");

    companion object {
        fun fromRaw(raw: String?): MatchStatus =
            entries.firstOrNull { it.wireValue == raw || it.name == raw } ?: LIVE
    }
}

@Keep
@IgnoreExtraProperties
data class AppSettings(
    @DocumentId val id: String = "",
    val upiId: String = "firebattle@upi",
    val upiName: String = "Firebattle Esports",
    val adminPhone: String = "9876543210",
    val referralBonus: Long = 10L,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val ign: String = "",
    val gameUid: String = "",
    val phone: String = "",
    val wallet: Long = 0L,
    val referralCode: String = "",
    val referredByUid: String = "",
    val referredByCode: String = "",
    val referralCount: Long = 0L,
    val referralEarnings: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ReferralRecord(
    @DocumentId val id: String = "",
    val referredUid: String = "",
    val referredIgn: String = "",
    val referrerUid: String = "",
    val referrerCode: String = "",
    val bonusAmount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TournamentMatch(
    @DocumentId val id: String = "",
    val title: String = "",
    val game: String = GameTitle.FREE_FIRE_MAX.label,
    val matchTime: String = "",
    val mapName: String = "",
    val entry: Long = 0L,
    val perKill: Long = 0L,
    val prize: Long = 0L,
    val slots: Long = 48L,
    val joinedCount: Long = 0L,
    val roomId: String = "",
    val roomPass: String = "",
    val status: String = MatchStatus.LIVE.wireValue,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MatchRegistration(
    @DocumentId val id: String = "",
    val matchId: String = "",
    val userId: String = "",
    val ign: String = "",
    val gameUid: String = "",
    val phone: String = "",
    val entryPaid: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DepositRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val phone: String = "",
    val ign: String = "",
    val amount: Long = 0L,
    val utr: String = "",
    val status: String = "Pending",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WithdrawalRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val phone: String = "",
    val ign: String = "",
    val amount: Long = 0L,
    val upiId: String = "",
    val status: String = "Pending",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
