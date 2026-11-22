package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AppSettings
import com.example.data.model.MatchMode
import com.example.data.model.MatchRegistration
import com.example.data.model.TeamFormat
import com.example.data.model.TournamentMatch
import com.example.data.model.UserProfile
import com.example.ui.theme.ArenaInkDark
import com.example.ui.theme.ArenaObsidian
import com.example.ui.theme.ArenaSlateBorder
import com.example.ui.theme.ArenaSlateDark
import com.example.ui.theme.ArenaSlateSurface
import com.example.ui.theme.ArenaWhiteCard
import com.example.ui.theme.BlazeOrange
import com.example.ui.theme.CoinGold
import com.example.ui.theme.ContestPurple
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.EmeraldWinContainer
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

data class ArenaFilterSelection(
    val feed: String = "Live",
    val game: String = "All",
    val mode: String = "All",
    val format: String = "All",
    val onlyJoinedOrOngoing: Boolean = false,
    val title: String = "All Matches"
)

private data class EsportGameTile(
    val id: String,
    val title: String,
    val badge: String,
    @DrawableRes val imageRes: Int,
    val selection: ArenaFilterSelection
)

private data class PromoSlide(
    @DrawableRes val imageRes: Int,
    val tag: String,
    val headline: String,
    val actionSelection: ArenaFilterSelection? = null,
    val openReferTab: Boolean = false
)

@Composable
fun PlayerHomeArenaSection(
    profile: UserProfile,
    settings: AppSettings,
    liveMatches: List<TournamentMatch>,
    completedMatches: List<TournamentMatch>,
    joinedIds: Set<String>,
    myRegistrations: List<MatchRegistration>,
    onOpenArenaFilter: (ArenaFilterSelection) -> Unit,
    onOpenWallet: () -> Unit,
    onOpenRefer: () -> Unit,
    onOpenProfile: () -> Unit,
    onCopyFeedback: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showTopPlayersDialog by rememberSaveable { mutableStateOf(false) }
    var showContactDialog by rememberSaveable { mutableStateOf(false) }

    val ongoingJoinedCount = remember(liveMatches, joinedIds) {
        liveMatches.count { it.id in joinedIds && it.roomId.isNotBlank() }
    }
    val upcomingJoinedCount = remember(liveMatches, joinedIds) {
        liveMatches.count { it.id in joinedIds }
    }

    val promoSlides = remember(settings.referralBonus) {
        listOf(
            PromoSlide(
                imageRes = R.drawable.img_banner_promo,
                tag = "LIVE TOURNAMENTS",
                headline = "Daily Custom, Clash Squad & Lone Wolf Matches",
                actionSelection = ArenaFilterSelection(title = "All Live Matches")
            ),
            PromoSlide(
                imageRes = R.drawable.img_arena_hero,
                tag = "INSTANT ROOM ID",
                headline = "1vs1, 2vs2 & 4vs4 Quick Cash Contests",
                actionSelection = ArenaFilterSelection(mode = "Clash Squad", title = "Clash Squad")
            ),
            PromoSlide(
                imageRes = R.drawable.img_trophy_banner,
                tag = "REFER & WIN",
                headline = "Invite Friends & Earn ₹${settings.referralBonus} Bonus Each",
                openReferTab = true
            )
        )
    }

    val gameTiles = remember {
        listOf(
            EsportGameTile(
                id = "bgmi",
                title = "BGMI",
                badge = "BR",
                imageRes = R.drawable.img_game_bgmi,
                selection = ArenaFilterSelection(game = "BGMI", title = "BGMI Tournaments")
            ),
            EsportGameTile(
                id = "ffmax1",
                title = "FF MAX",
                badge = "MAX",
                imageRes = R.drawable.img_game_ffmax,
                selection = ArenaFilterSelection(game = "Free Fire MAX", title = "Free Fire MAX")
            ),
            EsportGameTile(
                id = "cs1",
                title = "CS 1vs1",
                badge = "1v1",
                imageRes = R.drawable.img_game_clash,
                selection = ArenaFilterSelection(mode = "Clash Squad", format = "1vs1", title = "CS 1vs1 Duels")
            ),
            EsportGameTile(
                id = "bgmi_tdm",
                title = "BGMI TDM",
                badge = "TDM",
                imageRes = R.drawable.img_banner_promo,
                selection = ArenaFilterSelection(game = "BGMI", format = "4vs4", title = "BGMI TDM & 4vs4")
            ),
            EsportGameTile(
                id = "ff_custom",
                title = "FF CUSTOM",
                badge = "FULL",
                imageRes = R.drawable.img_game_ffmax,
                selection = ArenaFilterSelection(game = "Free Fire MAX", mode = "Custom", format = "Full Map", title = "FF Full Map Custom")
            ),
            EsportGameTile(
                id = "cs2",
                title = "CS 2vs2",
                badge = "2v2",
                imageRes = R.drawable.img_game_clash,
                selection = ArenaFilterSelection(mode = "Clash Squad", format = "2vs2", title = "CS 2vs2 Duo")
            ),
            EsportGameTile(
                id = "lone_wolf",
                title = "LONE WOLF",
                badge = "LW",
                imageRes = R.drawable.img_game_lonewolf,
                selection = ArenaFilterSelection(mode = "Lone Wolf", title = "Lone Wolf Matches")
            ),
            EsportGameTile(
                id = "ff_survival",
                title = "FF SURVIVAL",
                badge = "ROOM",
                imageRes = R.drawable.img_arena_hero,
                selection = ArenaFilterSelection(game = "Free Fire MAX", mode = "Custom", title = "FF Survival Rooms")
            ),
            EsportGameTile(
                id = "lw_1v1",
                title = "LW 1vs1",
                badge = "1v1",
                imageRes = R.drawable.img_game_lonewolf,
                selection = ArenaFilterSelection(mode = "Lone Wolf", format = "1vs1", title = "Lone Wolf 1vs1")
            ),
            EsportGameTile(
                id = "lw_2v2",
                title = "LW 2vs2",
                badge = "2v2",
                imageRes = R.drawable.img_game_lonewolf,
                selection = ArenaFilterSelection(mode = "Lone Wolf", format = "2vs2", title = "Lone Wolf 2vs2")
            ),
            EsportGameTile(
                id = "cs_4v4",
                title = "CS 4vs4",
                badge = "4v4",
                imageRes = R.drawable.img_banner_promo,
                selection = ArenaFilterSelection(mode = "Clash Squad", format = "4vs4", title = "CS 4vs4 Squad")
            ),
            EsportGameTile(
                id = "all_live",
                title = "ALL MATCHES",
                badge = "ALL",
                imageRes = R.drawable.img_trophy_banner,
                selection = ArenaFilterSelection(title = "All Live Tournaments")
            )
        )
    }

    val tileRows = remember(gameTiles) { gameTiles.chunked(3) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Orange + White Split Announcement Banner
        item {
            val tickerText = settings.sponsorTitle.takeIf { it.isNotBlank() }?.uppercase()
                ?: stringResource(R.string.arena_announcement_default)
            val sponsorUrl = settings.sponsorUrl
            val hasSponsorLink = sponsorUrl.startsWith("https://") || sponsorUrl.startsWith("http://")

            Surface(
                color = ArenaWhiteCard,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = hasSponsorLink) {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(sponsorUrl)))
                        }
                    }
                    .testTag("arena_announcement_banner")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 58.dp, height = 56.dp)
                            .background(BlazeOrange),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Campaign,
                            contentDescription = "Announcement",
                            tint = ArenaWhiteCard,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(
                        text = tickerText,
                        style = MaterialTheme.typography.titleSmall,
                        color = ArenaInkDark,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }

        // 2. Account Details (4 circular bright orange quick actions)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.arena_account_details),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AccountDetailCircleButton(
                        label = "My Profile",
                        icon = Icons.Filled.Person,
                        testTag = "quick_my_profile",
                        onClick = onOpenProfile,
                        modifier = Modifier.weight(1f)
                    )
                    AccountDetailCircleButton(
                        label = "My Wallet",
                        icon = Icons.Filled.AccountBalanceWallet,
                        testTag = "quick_my_wallet",
                        onClick = onOpenWallet,
                        modifier = Modifier.weight(1f)
                    )
                    AccountDetailCircleButton(
                        label = "Top Players",
                        icon = Icons.Filled.EmojiEvents,
                        testTag = "quick_top_players",
                        onClick = { showTopPlayersDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                    AccountDetailCircleButton(
                        label = "Contact Us",
                        icon = Icons.Filled.HeadsetMic,
                        testTag = "quick_contact_us",
                        onClick = { showContactDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. Hero Promo Banner Slider + Dot Indicators
        item {
            PromoBannerCarousel(
                slides = promoSlides,
                onSlideClick = { slide ->
                    if (slide.openReferTab) {
                        onOpenRefer()
                    } else if (slide.actionSelection != null) {
                        onOpenArenaFilter(slide.actionSelection)
                    }
                }
            )
        }

        // 4. My Contest (3 White Rounded Cards with Purple Trophies)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.arena_my_contest),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MyContestTile(
                        label = "Upcoming",
                        badgeCount = liveMatches.size,
                        testTag = "contest_upcoming_card",
                        onClick = {
                            onOpenArenaFilter(
                                ArenaFilterSelection(
                                    feed = "Live",
                                    title = "Upcoming Matches (${liveMatches.size})"
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    MyContestTile(
                        label = "Ongoing",
                        badgeCount = if (ongoingJoinedCount > 0) ongoingJoinedCount else upcomingJoinedCount,
                        testTag = "contest_ongoing_card",
                        onClick = {
                            onOpenArenaFilter(
                                ArenaFilterSelection(
                                    feed = "Live",
                                    onlyJoinedOrOngoing = true,
                                    title = "My Joined & Ongoing"
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    MyContestTile(
                        label = "Completed",
                        badgeCount = completedMatches.size,
                        testTag = "contest_completed_card",
                        onClick = {
                            onOpenArenaFilter(
                                ArenaFilterSelection(
                                    feed = "Results",
                                    title = "Winners & Completed"
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 5. eSport Games Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.arena_esport_games),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(
                    onClick = { onOpenArenaFilter(ArenaFilterSelection(title = "All Live Tournaments")) },
                    modifier = Modifier.testTag("arena_view_all_matches")
                ) {
                    Text(
                        text = "View All (${liveMatches.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = BlazeOrange,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 6. 3-Column Visual Grid of Game Modes
        items(tileRows) { rowTiles ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowTiles.forEach { tile ->
                    val matchingCount = remember(liveMatches, tile) {
                        liveMatches.count { match ->
                            val sel = tile.selection
                            val modeLabel = MatchMode.fromRaw(match.matchMode).label
                            val formatLabel = TeamFormat.fromRaw(match.teamFormat).label
                            (sel.game == "All" || match.game == sel.game) &&
                                (sel.mode == "All" || modeLabel == sel.mode) &&
                                (sel.format == "All" || formatLabel == sel.format)
                        }
                    }
                    EsportGameGridCard(
                        tile = tile,
                        liveCount = matchingCount,
                        onClick = { onOpenArenaFilter(tile.selection) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(3 - rowTiles.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
        }
    }

    if (showTopPlayersDialog) {
        TopPlayersDialog(
            profile = profile,
            completedMatches = completedMatches,
            myRegistrations = myRegistrations,
            onDismiss = { showTopPlayersDialog = false }
        )
    }

    if (showContactDialog) {
        ContactOrganizerDialog(
            settings = settings,
            onCopyFeedback = onCopyFeedback,
            onDismiss = { showContactDialog = false }
        )
    }
}

@Composable
private fun AccountDetailCircleButton(
    label: String,
    icon: ImageVector,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
            .testTag(testTag)
    ) {
        Surface(
            color = BlazeOrange,
            shape = CircleShape,
            shadowElevation = 4.dp,
            modifier = Modifier.size(58.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = ArenaWhiteCard,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PromoBannerCarousel(
    slides: List<PromoSlide>,
    onSlideClick: (PromoSlide) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { slides.size })

    LaunchedEffect(pagerState, slides.size) {
        if (slides.size <= 1) return@LaunchedEffect
        while (true) {
            delay(4200L)
            val next = (pagerState.currentPage + 1) % slides.size
            pagerState.animateScrollToPage(next)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        HorizontalPager(
            state = pagerState,
            pageSpacing = 10.dp,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val slide = slides[page]
            Surface(
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, ArenaSlateBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSlideClick(slide) }
                    .testTag("promo_banner_slide_$page")
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = slide.imageRes),
                        contentDescription = slide.headline,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        ArenaObsidian.copy(alpha = 0.84f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            color = BlazeOrange,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = slide.tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = ArenaWhiteCard,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = slide.headline,
                            style = MaterialTheme.typography.titleSmall,
                            color = ArenaWhiteCard,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(slides.size) { idx ->
                val isSelected = pagerState.currentPage == idx
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) ContestPurple else ArenaSlateBorder)
                )
            }
        }
    }
}

@Composable
private fun MyContestTile(
    label: String,
    badgeCount: Int,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ArenaWhiteCard,
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 3.dp,
        modifier = modifier
            .heightIn(min = 104.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (badgeCount > 0) {
                Surface(
                    color = BlazeOrange.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    Text(
                        text = badgeCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = BlazeOrange,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = 8.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.EmojiEvents,
                    contentDescription = label,
                    tint = ContestPurple,
                    modifier = Modifier.size(34.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    color = ArenaInkDark,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EsportGameGridCard(
    tile: EsportGameTile,
    liveCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ArenaSlateDark,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (liveCount > 0) BlazeOrange.copy(alpha = 0.6f) else ArenaSlateBorder),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag("game_tile_${tile.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.02f)
            ) {
                Image(
                    painter = painterResource(id = tile.imageRes),
                    contentDescription = tile.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    color = ArenaObsidian.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(bottomEnd = 8.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = tile.badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = CoinGold,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                if (liveCount > 0) {
                    Surface(
                        color = EmeraldWin,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(5.dp)
                    ) {
                        Text(
                            text = "$liveCount LIVE",
                            style = MaterialTheme.typography.labelSmall,
                            color = ArenaInkDark,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BlazeOrange)
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tile.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = ArenaInkDark,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun TopPlayersDialog(
    profile: UserProfile,
    completedMatches: List<TournamentMatch>,
    myRegistrations: List<MatchRegistration>,
    onDismiss: () -> Unit
) {
    val myTotalWon = remember(myRegistrations) { myRegistrations.sumOf { it.prizeWon } }
    val myTotalKills = remember(myRegistrations) { myRegistrations.sumOf { it.kills } }
    val matchChampions = remember(completedMatches) {
        completedMatches.filter { it.winnerSummary.isNotBlank() || it.totalPrizePaid > 0L }.take(10)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ArenaSlateDark,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    tint = CoinGold
                )
                Text(
                    text = "Top Players & Winners",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = EmeraldWinContainer,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, EmeraldWin.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Your Stats (${profile.ign})",
                                style = MaterialTheme.typography.labelMedium,
                                color = EmeraldWin,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$myTotalKills Kills Verified",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = "₹$myTotalWon Won",
                            style = MaterialTheme.typography.titleMedium,
                            color = EmeraldWin,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                if (matchChampions.isEmpty()) {
                    Text(
                        text = "No completed match champions published yet. Join a live match and be the first on the leaderboard!",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                } else {
                    matchChampions.forEach { match ->
                        Surface(
                            color = ArenaObsidian,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, ArenaSlateBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "${match.title} (${match.game})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = BlazeOrange,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "🏆 ${match.winnerSummary.ifBlank { "Prize Settled" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (match.totalPrizePaid > 0L) {
                                    Text(
                                        text = "Total Paid: ₹${match.totalPrizePaid}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldWin
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = BlazeOrange)
            ) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun ContactOrganizerDialog(
    settings: AppSettings,
    onCopyFeedback: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ArenaSlateDark,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.HeadsetMic,
                    contentDescription = null,
                    tint = BlazeOrange
                )
                Text(
                    text = "Contact Organizer",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Need help with match Room ID, deposit UTR approval, or withdrawal settlement? Reach the Firebattle organizer directly:",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Surface(
                    color = ArenaObsidian,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ArenaSlateBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Support Phone", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(
                                text = "+91 ${settings.adminPhone}",
                                style = MaterialTheme.typography.titleSmall,
                                fontFamily = JetBrainsMonoFamily,
                                color = EmeraldWin
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(settings.adminPhone))
                                onCopyFeedback("Support phone copied!")
                            },
                            border = BorderStroke(1.dp, ArenaSlateBorder),
                            modifier = Modifier.heightIn(min = 42.dp)
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy Phone", modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Surface(
                    color = ArenaObsidian,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ArenaSlateBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Official Admin UPI", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(
                                text = settings.upiId,
                                style = MaterialTheme.typography.titleSmall,
                                fontFamily = JetBrainsMonoFamily,
                                color = BlazeOrange
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(settings.upiId))
                                onCopyFeedback("Official UPI ID copied!")
                            },
                            border = BorderStroke(1.dp, ArenaSlateBorder),
                            modifier = Modifier.heightIn(min = 42.dp)
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy UPI", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    runCatching {
                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+91${settings.adminPhone}"))
                        context.startActivity(dialIntent)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldWin, contentColor = ArenaObsidian)
            ) {
                Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Call Now", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        }
    )
}
