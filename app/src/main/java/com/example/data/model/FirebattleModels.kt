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
    REQUESTS,
    PROFILE
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
    BGMI("BGMI"),
    LUDO("Ludo");

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

enum class MatchMode(val label: String) {
    CUSTOM("Custom"),
    CLASH_SQUAD("Clash Squad"),
    LONE_WOLF("Lone Wolf"),
    LUDO("Ludo");

    companion object {
        fun fromRaw(raw: String?): MatchMode =
            entries.firstOrNull { it.label == raw || it.name == raw } ?: CUSTOM
    }
}

enum class TeamFormat(val label: String, val defaultSlots: Long) {
    ONE_VS_ONE("1vs1", 2L),
    TWO_VS_TWO("2vs2", 4L),
    THREE_VS_THREE("3vs3", 6L),
    FOUR_VS_FOUR("4vs4", 8L),
    FULL_MAP("Full Map", 48L);

    companion object {
        fun fromRaw(raw: String?): TeamFormat =
            entries.firstOrNull { it.label == raw || it.name == raw } ?: FULL_MAP
    }
}

const val LEGACY_APK_DOWNLOAD_URL =
    "https://github.com/chauhanjignesh8320-cmd/firebattle-esports/releases/latest/download/Firebattle.apk"

const val LEGACY_GITHUB_PAGES_URL =
    "https://chauhanjignesh8320-cmd.github.io/firebattle-esports/"

const val LEGACY_VERCEL_URL =
    "https://firebattle-esports-k62s.vercel.app/"

const val DEFAULT_APP_DOWNLOAD_URL =
    "https://firebetal.com/"

fun sanitizeReferralCode(raw: String): String =
    raw.trim().uppercase().filter { it.isLetterOrDigit() }.take(16)

fun buildReferralInviteLink(downloadUrl: String, referralCode: String): String {
    val trimmed = downloadUrl.trim().ifBlank { DEFAULT_APP_DOWNLOAD_URL }
    val pathOnly = trimmed.substringBefore('?').substringBefore('#').lowercase()
    val isDirectBinaryOrAlias = pathOnly.endsWith(".apk") ||
        pathOnly.endsWith("/download") ||
        pathOnly.endsWith("/apk")
    val baseUrl = if (isDirectBinaryOrAlias) DEFAULT_APP_DOWNLOAD_URL else trimmed
    val cleanCode = sanitizeReferralCode(referralCode)
    if (cleanCode.isBlank()) return baseUrl
    val separator = if ('?' in baseUrl) "&" else "?"
    return "$baseUrl${separator}ref=$cleanCode"
}

@Keep
@IgnoreExtraProperties
data class AppSettings(
    @DocumentId val id: String = "",
    val upiId: String = "firebattle@upi",
    val upiName: String = "Firebattle Esports",
    val adminPhone: String = "9876543210",
    val referralBonus: Long = 10L,
    val appDownloadUrl: String = DEFAULT_APP_DOWNLOAD_URL,
    val withdrawalFeePercent: Long = 5L,
    val minWithdrawal: Long = 50L,
    val sponsorTitle: String = "",
    val sponsorSubtitle: String = "",
    val sponsorUrl: String = "",
    val depositBonusPercent: Long = 0L,
    val depositOfferMinAmount: Long = 0L,
    val depositOfferLabel: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val hasActiveDepositOffer: Boolean
        get() = depositBonusPercent in 1L..100L

    fun calculateDepositBonus(amount: Long): Long {
        if (amount <= 0L || depositBonusPercent <= 0L) return 0L
        if (amount < depositOfferMinAmount) return 0L
        return ((amount * depositBonusPercent.coerceIn(0L, 100L)) / 100L).coerceIn(0L, amount)
    }
}

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
    val winningBalance: Long = 0L,
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
    val matchMode: String = MatchMode.CUSTOM.label,
    val teamFormat: String = TeamFormat.FULL_MAP.label,
    val matchTime: String = "",
    val mapName: String = "",
    val entry: Long = 0L,
    val perKill: Long = 0L,
    val prize: Long = 0L,
    val slots: Long = 48L,
    val joinedCount: Long = 0L,
    val takenSlots: List<Long> = emptyList(),
    val rules: String = "",
    val roomId: String = "",
    val roomPass: String = "",
    val status: String = MatchStatus.LIVE.wireValue,
    val winnerSummary: String = "",
    val totalPrizePaid: Long = 0L,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class MatchRuleEntry(
    val allowed: Boolean,
    val label: String
)

const val DEFAULT_FF_RULES_TEXT =
    "+ID LEVEL 40+ ALLOWED\n+HUD POV ALLOWED\n-HORSE BAN\n-M79 LAUNCHER BAN\n-DOUBLE VECTOR BAN\n-IDP SHARE NOT ALLOWED"

const val DEFAULT_BGMI_RULES_TEXT =
    "+ID LEVEL 30+ ALLOWED\n+MOBILE FAIR PLAY ALLOWED\n-EMULATOR / GFX BAN\n-TEAMING NOT ALLOWED\n-HACK / CONFIG BAN\n-IDP SHARE NOT ALLOWED"

const val DEFAULT_LUDO_RULES_TEXT =
    "+OFFICIAL LUDO ROOM / IN-APP FAIR PLAY\n+JOIN SLOT BEFORE MATCH TIME\n-NO MOD APK OR EMULATOR\n-NO WRONG SLOT BOOKING\n-ROOM CODE SHARE NOT ALLOWED"

fun defaultRulesForGame(game: String): String = when (game) {
    GameTitle.BGMI.label -> DEFAULT_BGMI_RULES_TEXT
    GameTitle.LUDO.label -> DEFAULT_LUDO_RULES_TEXT
    else -> DEFAULT_FF_RULES_TEXT
}

fun parseMatchRuleEntries(rawRules: String, game: String): List<MatchRuleEntry> {
    val source = rawRules.trim().ifBlank { defaultRulesForGame(game) }
    return source.lines()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .take(8)
        .map { line ->
            when {
                line.startsWith("+") -> MatchRuleEntry(true, line.drop(1).trim())
                line.startsWith("-") || line.startsWith("!") -> MatchRuleEntry(false, line.drop(1).trim())
                line.contains("ALLOWED", ignoreCase = true) && !line.contains("NOT ALLOWED", ignoreCase = true) ->
                    MatchRuleEntry(true, line)
                else -> MatchRuleEntry(false, line)
            }
        }
        .filter { it.label.isNotEmpty() }
}

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
    val slotNumber: Long = 0L,
    val kills: Long = 0L,
    val prizeWon: Long = 0L,
    val isWinner: Boolean = false,
    val payoutNote: String = "",
    @ServerTimestamp val paidAt: Timestamp? = null,
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
    val bonusAmount: Long = 0L,
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
    val feeAmount: Long = 0L,
    val netPayout: Long = 0L,
    val upiId: String = "",
    val status: String = "Pending",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val resolvedNetPayout: Long
        get() = if (netPayout > 0L) netPayout else (amount - feeAmount).coerceAtLeast(0L)
}

@Keep
@IgnoreExtraProperties
data class OwnerPayout(
    @DocumentId val id: String = "",
    val adminUid: String = "",
    val amount: Long = 0L,
    val upiId: String = "",
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppInstall(
    @DocumentId val id: String = "",
    val installId: String = "",
    val userId: String = "",
    val deviceModel: String = "",
    val appVersion: String = "",
    @ServerTimestamp val firstSeenAt: Timestamp? = null,
    @ServerTimestamp val lastActiveAt: Timestamp? = null
)
