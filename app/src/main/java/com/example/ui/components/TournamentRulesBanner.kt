package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.GameTitle
import com.example.data.model.MatchMode
import com.example.data.model.MatchRuleEntry
import com.example.data.model.TournamentMatch
import com.example.data.model.parseMatchRuleEntries
import com.example.ui.theme.ArenaObsidian
import com.example.ui.theme.ArenaSlateDark
import com.example.ui.theme.BlazeOrange
import com.example.ui.theme.CoinGold
import com.example.ui.theme.CrimsonReject
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private val RulesWoodBgTop = Color(0xFF3E2211)
private val RulesWoodBgBottom = Color(0xFF231309)
private val RulesWoodBorder = Color(0xFF9C5D2D)
private val RulesHeaderRed = Color(0xFFFF3B30)

@DrawableRes
private fun bannerArtForMatch(match: TournamentMatch): Int {
    val mode = MatchMode.fromRaw(match.matchMode)
    return when {
        match.game == GameTitle.BGMI.label -> R.drawable.img_game_bgmi
        mode == MatchMode.LONE_WOLF -> R.drawable.img_game_lonewolf
        mode == MatchMode.CLASH_SQUAD -> R.drawable.img_game_clash
        else -> R.drawable.img_game_ffmax
    }
}

@Composable
fun TournamentRulesHeroBanner(
    match: TournamentMatch,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ruleEntries = remember(match.id, match.rules, match.game) {
        parseMatchRuleEntries(match.rules, match.game)
    }
    val visibleRules = remember(ruleEntries) { ruleEntries.take(6) }
    val artRes = remember(match.game, match.matchMode) { bannerArtForMatch(match) }
    val mapLabel = match.mapName.ifBlank { "ARENA" }.uppercase()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(172.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("tournament_rules_banner_${match.id}")
    ) {
        Image(
            painter = painterResource(id = artRes),
            contentDescription = "${match.game} ${match.mapName} Rules Banner",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            ArenaObsidian.copy(alpha = 0.28f),
                            ArenaObsidian.copy(alpha = 0.55f),
                            ArenaObsidian.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left visual column: Game pill + Big Gold Map Title
            Column(
                modifier = Modifier
                    .weight(0.43f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                Surface(
                    color = ArenaObsidian.copy(alpha = 0.82f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, CoinGold.copy(alpha = 0.55f))
                ) {
                    Text(
                        text = match.game.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = CoinGold,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        color = ArenaObsidian.copy(alpha = 0.78f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, CoinGold.copy(alpha = 0.65f))
                    ) {
                        Text(
                            text = mapLabel,
                            color = CoinGold,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = "Tap for full rules",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.88f),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }

            // Right Wooden Signboard: TOURNAMENT RULES + Green Allowed / Red Ban list
            Surface(
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.5.dp, RulesWoodBorder),
                modifier = Modifier
                    .weight(0.57f)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(RulesWoodBgTop, RulesWoodBgBottom)
                            )
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "TOURNAMENT",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "RULES",
                            color = RulesHeaderRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    }

                    visibleRules.forEach { rule ->
                        RuleBoardRow(rule = rule)
                    }
                }
            }
        }
    }
}

@Composable
private fun RuleBoardRow(rule: MatchRuleEntry) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (rule.allowed) {
            Surface(
                color = EmeraldWin,
                shape = CircleShape,
                modifier = Modifier.size(13.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Allowed",
                        tint = ArenaObsidian,
                        modifier = Modifier.size(9.dp)
                    )
                }
            }
        } else {
            Icon(
                imageVector = Icons.Filled.Block,
                contentDescription = "Ban or Not Allowed",
                tint = RulesHeaderRed,
                modifier = Modifier.size(13.dp)
            )
        }
        Text(
            text = rule.label.uppercase(),
            color = if (rule.allowed) Color(0xFFD1FAE5) else Color(0xFFFDE68A),
            fontSize = 9.5.sp,
            lineHeight = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun TournamentRulesDialog(
    match: TournamentMatch,
    onDismiss: () -> Unit
) {
    val entries = remember(match.id, match.rules, match.game) {
        parseMatchRuleEntries(match.rules, match.game)
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
                    imageVector = Icons.Filled.Gavel,
                    contentDescription = null,
                    tint = CoinGold
                )
                Column {
                    Text(
                        text = "Tournament Rules",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${match.title} • ${match.mapName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                entries.forEach { entry ->
                    Surface(
                        color = ArenaObsidian,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            1.dp,
                            if (entry.allowed) EmeraldWin.copy(alpha = 0.5f) else CrimsonReject.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (entry.allowed) Icons.Filled.Check else Icons.Filled.Block,
                                contentDescription = if (entry.allowed) "Allowed" else "Banned",
                                tint = if (entry.allowed) EmeraldWin else CrimsonReject,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = entry.label.uppercase(),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (entry.allowed) EmeraldWin else Color(0xFFFCA5A5),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Breaking room rules or sharing Room ID & Password leads to match kick & prize disqualification.",
                    style = MaterialTheme.typography.bodySmall,
                    color = BlazeOrange,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = BlazeOrange)
            ) {
                Text("OK, Understood", color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        }
    )
}
