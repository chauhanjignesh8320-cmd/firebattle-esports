package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AppMode
import com.example.ui.theme.AmberPending
import com.example.ui.theme.AmberPendingContainer
import com.example.ui.theme.ArenaObsidian
import com.example.ui.theme.ArenaSlateBorder
import com.example.ui.theme.ArenaSlateDark
import com.example.ui.theme.ArenaSlateSurface
import com.example.ui.theme.BlazeOrange
import com.example.ui.theme.CrimsonReject
import com.example.ui.theme.CrimsonRejectContainer
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.EmeraldWinContainer
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.ui.theme.ArenaHeaderNavy
import com.example.ui.theme.ArenaInkDark
import com.example.ui.theme.ArenaInkMuted
import com.example.ui.theme.ArenaLightBorder
import com.example.ui.theme.ArenaLightSurface
import com.example.ui.theme.ArenaWhiteCard
import com.example.ui.theme.CoinGold
import com.example.ui.theme.CoinPillBg
import com.example.ui.theme.ContestPurple

@Composable
fun FirebattleTopBar(
    currentMode: AppMode,
    isAdmin: Boolean,
    onModeSelected: (AppMode) -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
    walletBalance: Long? = null,
    alertCount: Int = 0,
    onWalletClick: () -> Unit = {},
    onAlertsClick: () -> Unit = {}
) {
    Surface(
        color = ArenaHeaderNavy,
        border = BorderStroke(0.5.dp, ArenaSlateBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        if (currentMode == AppMode.PLAYER && walletBalance != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        border = BorderStroke(1.5.dp, CoinGold),
                        color = ArenaObsidian,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_art),
                            contentDescription = stringResource(R.string.app_name),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.clip(CircleShape)
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(R.string.arena_welcome_back),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            maxLines = 1
                        )
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isAdmin) {
                        Surface(
                            color = BlazeOrange,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .heightIn(min = 36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onModeSelected(AppMode.ADMIN) }
                                .testTag("mode_admin_button")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Admin",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ArenaInkDark,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    Box(contentAlignment = Alignment.Center) {
                        IconButton(
                            onClick = onAlertsClick,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("top_alerts_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = stringResource(R.string.enable_room_alerts),
                                tint = TextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Surface(
                            color = CoinGold,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-6).dp, y = 6.dp)
                        ) {
                            Text(
                                text = alertCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = ArenaInkDark,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Surface(
                        color = CoinPillBg,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, CoinGold.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .heightIn(min = 38.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onWalletClick)
                            .testTag("top_wallet_pill")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(BlazeOrange),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "₹",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Text(
                                text = "$walletBalance",
                                style = MaterialTheme.typography.titleSmall,
                                color = ArenaInkDark,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(BlazeOrange.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalFireDepartment,
                            contentDescription = stringResource(R.string.brand_title),
                            tint = BlazeOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.brand_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = BlazeOrange,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isAdmin) {
                        Surface(
                            color = ArenaSlateSurface,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, ArenaSlateBorder)
                        ) {
                            Row(modifier = Modifier.padding(3.dp)) {
                                ModeTogglePill(
                                    label = "Player",
                                    selected = currentMode == AppMode.PLAYER,
                                    onClick = { onModeSelected(AppMode.PLAYER) },
                                    tag = "mode_player_button"
                                )
                                ModeTogglePill(
                                    label = "Admin",
                                    selected = currentMode == AppMode.ADMIN,
                                    onClick = { onModeSelected(AppMode.ADMIN) },
                                    tag = "mode_admin_button"
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onLogoutClick,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = stringResource(R.string.logout),
                            tint = CrimsonReject
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeTogglePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    Box(
        modifier = Modifier
            .heightIn(min = 36.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) BlazeOrange else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) TextPrimary else TextSecondary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when (status.uppercase(Locale.US)) {
        "LIVE", "APPROVED", "PAID" -> EmeraldWinContainer to EmeraldWin
        "PENDING" -> AmberPendingContainer to AmberPending
        "REJECTED" -> CrimsonRejectContainer to CrimsonReject
        else -> ArenaSlateSurface to TextSecondary
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, fg.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun StatTile(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ArenaObsidian,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, ArenaSlateBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = valueColor,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun EmptyArenaState(
    title: String,
    subtitle: String,
    icon: ImageVector = Icons.Outlined.SportsEsports,
    useLightCard: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (useLightCard) ArenaWhiteCard else ArenaSlateDark,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, if (useLightCard) ArenaLightBorder else ArenaSlateBorder),
        shadowElevation = if (useLightCard) 6.dp else 0.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(if (useLightCard) ContestPurple.copy(alpha = 0.12f) else ArenaSlateSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (useLightCard) ContestPurple else BlazeOrange,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (useLightCard) ArenaInkDark else TextPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = if (useLightCard) ArenaInkMuted else TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ErrorBannerCard(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = CrimsonRejectContainer,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, CrimsonReject.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = CrimsonReject,
            modifier = Modifier.padding(14.dp)
        )
    }
}

fun formatTimestampShort(timestamp: Timestamp?): String {
    val ts = timestamp ?: return "Just now"
    val formatter = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    return formatter.format(ts.toDate())
}
