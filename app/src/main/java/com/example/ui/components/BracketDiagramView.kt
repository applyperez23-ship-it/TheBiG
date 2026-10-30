package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.Match
import com.example.data.local.entity.Team
import com.example.util.TeamCrestView

@Composable
fun BracketDiagramView(
    matches: List<Match>,
    teams: List<Team>,
    onMatchClick: (Long) -> Unit
) {
    val teamMap = remember(teams) { teams.associateBy { it.id } }

    // Group matches by round
    val quarters = matches.filter { it.roundName.contains("Cuartos", ignoreCase = true) }
    val semis = matches.filter { it.roundName.contains("Semi", ignoreCase = true) }
    val finals = matches.filter { it.roundName.contains("Final", ignoreCase = true) && !it.roundName.contains("Semi", ignoreCase = true) && !it.roundName.contains("Cuartos", ignoreCase = true) && !it.roundName.contains("Octavos", ignoreCase = true) }

    val hasAnyKnockout = quarters.isNotEmpty() || semis.isNotEmpty() || finals.isNotEmpty()

    if (!hasAnyKnockout) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No hay partidos de fase eliminatoria generados todavía.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Cuadro Eliminatorio (Brackets)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Scrollable bracket container
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Column 1: Cuartos de Final (if any)
                if (quarters.isNotEmpty()) {
                    BracketColumn(
                        roundTitle = "Cuartos de Final",
                        matches = quarters,
                        teamMap = teamMap,
                        onMatchClick = onMatchClick
                    )
                }

                // Column 2: Semifinales
                if (semis.isNotEmpty()) {
                    BracketColumn(
                        roundTitle = "Semifinales",
                        matches = semis,
                        teamMap = teamMap,
                        onMatchClick = onMatchClick
                    )
                }

                // Column 3: Gran Final
                if (finals.isNotEmpty()) {
                    BracketColumn(
                        roundTitle = "Gran Final",
                        matches = finals,
                        teamMap = teamMap,
                        isFinal = true,
                        onMatchClick = onMatchClick
                    )
                }
            }
        }
    }
}

@Composable
private fun BracketColumn(
    roundTitle: String,
    matches: List<Match>,
    teamMap: Map<Long, Team>,
    isFinal: Boolean = false,
    onMatchClick: (Long) -> Unit
) {
    Column(
        modifier = Modifier.width(220.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isFinal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = roundTitle,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                color = if (isFinal) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 6.dp)
            )
        }

        matches.forEach { match ->
            BracketMatchCard(
                match = match,
                homeTeam = teamMap[match.homeTeamId],
                awayTeam = teamMap[match.awayTeamId],
                onClick = { onMatchClick(match.id) }
            )
        }
    }
}

@Composable
private fun BracketMatchCard(
    match: Match,
    homeTeam: Team?,
    awayTeam: Team?,
    onClick: () -> Unit
) {
    val isFinished = match.status == "FINISHED"
    val isLive = match.status == "LIVE"

    val homeWon = isFinished && (match.homeScore > match.awayScore || (match.homePenalties ?: 0) > (match.awayPenalties ?: 0))
    val awayWon = isFinished && (match.awayScore > match.homeScore || (match.awayPenalties ?: 0) > (match.homePenalties ?: 0))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isLive) Color(0xFF10B981) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Home team row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    TeamCrestView(team = homeTeam, size = 22.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = homeTeam?.shortName ?: homeTeam?.name ?: "TBD",
                        fontSize = 12.sp,
                        fontWeight = if (homeWon) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (homeWon) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
                Text(
                    text = if (isFinished || isLive) "${match.homeScore}" else "-",
                    fontSize = 13.sp,
                    fontWeight = if (homeWon) FontWeight.Black else FontWeight.Normal
                )
            }

            // Away team row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    TeamCrestView(team = awayTeam, size = 22.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = awayTeam?.shortName ?: awayTeam?.name ?: "TBD",
                        fontSize = 12.sp,
                        fontWeight = if (awayWon) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (awayWon) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
                Text(
                    text = if (isFinished || isLive) "${match.awayScore}" else "-",
                    fontSize = 13.sp,
                    fontWeight = if (awayWon) FontWeight.Black else FontWeight.Normal
                )
            }

            if (match.homePenalties != null && match.awayPenalties != null) {
                Text(
                    text = "Penaltis: ${match.homePenalties} - ${match.awayPenalties}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
