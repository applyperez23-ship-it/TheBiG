package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.SportsSoccer
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.Match
import com.example.data.local.entity.Team
import com.example.util.TeamCrestView

/**
 * Vertical continuous knockout bracket tree faithfully based on user sketch 96_sin_t_tulo_20261103131033_3f5ed78d.png.
 * Shows facing semifinal crests converging into finalist circles (?),
 * and converging downwards into the Grand Final Trophy circle.
 */
@Composable
fun VerticalKnockoutTreeFlow(
    matches: List<Match>,
    teams: List<Team>,
    onMatchClick: (Long) -> Unit
) {
    val teamMap = remember(teams) { teams.associateBy { it.id } }

    val repechaje = matches.filter {
        it.roundName.contains("Repechaje", ignoreCase = true) ||
        it.roundName.contains("Playoff", ignoreCase = true)
    }
    val quarters = matches.filter { it.roundName.contains("Cuartos", ignoreCase = true) }
    val semis = matches.filter { it.roundName.contains("Semi", ignoreCase = true) }
    val grandFinal = matches.filter {
        it.roundName.contains("Final", ignoreCase = true) &&
        !it.roundName.contains("Semi", ignoreCase = true) &&
        !it.roundName.contains("Cuartos", ignoreCase = true) &&
        !it.roundName.contains("Octavos", ignoreCase = true) &&
        !it.roundName.contains("Repechaje", ignoreCase = true)
    }

    val hasAnyKnockout = repechaje.isNotEmpty() || quarters.isNotEmpty() || semis.isNotEmpty() || grandFinal.isNotEmpty()

    if (!hasAnyKnockout) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Classification indicator header
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ÁRBOL DE ELIMINACIÓN DIRECTA (PLAYOFFS)",
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
        }

        // Section: Repechaje (if present)
        if (repechaje.isNotEmpty()) {
            PhaseHeaderBadge("REPECHAJE Y PLAYOFFS PREVIOS")
            repechaje.forEach { m ->
                KnockoutMatchCard(match = m, teamMap = teamMap, onMatchClick = onMatchClick)
            }
            PhaseConnectorArrow()
        }

        // Section: Cuartos de Final (if present)
        if (quarters.isNotEmpty()) {
            PhaseHeaderBadge("CUARTOS DE FINAL")
            quarters.forEach { m ->
                KnockoutMatchCard(match = m, teamMap = teamMap, onMatchClick = onMatchClick)
            }
            PhaseConnectorArrow()
        }

        // Main Tree Structure (Semifinales -> Finalist Circles '?' -> Grand Final Trophy Circle)
        PhaseHeaderBadge("FASE FINAL: SEMIFINALES Y COPA", isGold = true)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Determine Semi 1 and Semi 2
                val semi1 = semis.getOrNull(0)
                val semi2 = semis.getOrNull(1)

                val semi1WinnerId = semi1?.let { calculateWinnerId(it) }
                val semi2WinnerId = semi2?.let { calculateWinnerId(it) }

                val finalist1Team = semi1WinnerId?.let { teamMap[it] }
                val finalist2Team = semi2WinnerId?.let { teamMap[it] }

                val finalMatch = grandFinal.firstOrNull()
                val championId = finalMatch?.let { calculateWinnerId(it) }
                val championTeam = championId?.let { teamMap[it] }

                // TOP ROW: Two Semifinal Pairings Facing Each Other
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Semifinal 1 (Left Branch)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = semi1?.roundName ?: "Semifinal 1",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FacingMatchHeader(
                            match = semi1,
                            teamMap = teamMap,
                            onMatchClick = onMatchClick
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Semifinal 2 (Right Branch)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = semi2?.roundName ?: "Semifinal 2",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FacingMatchHeader(
                            match = semi2,
                            teamMap = teamMap,
                            onMatchClick = onMatchClick
                        )
                    }
                }

                // Connector lines from Semis to Finalist Circles
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                ) {
                    val lineColor = Color(0xFFF59E0B)
                    val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)

                    val leftCenterX = size.width * 0.25f
                    val rightCenterX = size.width * 0.75f
                    val branchSpread = size.width * 0.16f

                    // Left U-Bracket down to stem
                    val pathLeft = Path().apply {
                        moveTo(leftCenterX - branchSpread, 0f)
                        lineTo(leftCenterX - branchSpread, size.height * 0.5f)
                        lineTo(leftCenterX + branchSpread, size.height * 0.5f)
                        lineTo(leftCenterX + branchSpread, 0f)
                        moveTo(leftCenterX, size.height * 0.5f)
                        lineTo(leftCenterX, size.height)
                    }
                    drawPath(pathLeft, lineColor, style = stroke)

                    // Right U-Bracket down to stem
                    val pathRight = Path().apply {
                        moveTo(rightCenterX - branchSpread, 0f)
                        lineTo(rightCenterX - branchSpread, size.height * 0.5f)
                        lineTo(rightCenterX + branchSpread, size.height * 0.5f)
                        lineTo(rightCenterX + branchSpread, 0f)
                        moveTo(rightCenterX, size.height * 0.5f)
                        lineTo(rightCenterX, size.height)
                    }
                    drawPath(pathRight, lineColor, style = stroke)
                }

                // MIDDLE ROW: Two Finalist Circles ('?' or Winner Crest)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        WinnerCircleNode(
                            team = finalist1Team,
                            label = "Finalista 1",
                            isDecided = finalist1Team != null,
                            onClick = { semi1?.let { onMatchClick(it.id) } }
                        )
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        WinnerCircleNode(
                            team = finalist2Team,
                            label = "Finalista 2",
                            isDecided = finalist2Team != null,
                            onClick = { semi2?.let { onMatchClick(it.id) } }
                        )
                    }
                }

                // T-Bracket Connector down to Trophy Circle
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                ) {
                    val lineColor = Color(0xFFF59E0B)
                    val stroke = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)

                    val leftCenterX = size.width * 0.25f
                    val rightCenterX = size.width * 0.75f
                    val centerX = size.width * 0.5f

                    val pathT = Path().apply {
                        // Horizontal bar connecting both finalist nodes
                        moveTo(leftCenterX, size.height * 0.25f)
                        lineTo(rightCenterX, size.height * 0.25f)
                        // Vertical stem going down into Trophy Circle
                        moveTo(centerX, size.height * 0.25f)
                        lineTo(centerX, size.height)
                    }
                    drawPath(pathT, lineColor, style = stroke)
                }

                // BOTTOM ROW: Trophy & Champion Circle
                GrandFinalTrophyNode(
                    match = finalMatch,
                    champion = championTeam,
                    onClick = { finalMatch?.let { onMatchClick(it.id) } }
                )
            }
        }
    }
}

@Composable
private fun FacingMatchHeader(
    match: Match?,
    teamMap: Map<Long, Team>,
    onMatchClick: (Long) -> Unit
) {
    val home = match?.let { teamMap[it.homeTeamId] }
    val away = match?.let { teamMap[it.awayTeamId] }
    val isFinished = match?.status == "FINISHED"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = match != null) { match?.let { onMatchClick(it.id) } },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home Shield
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (home != null) {
                        TeamCrestView(team = home, size = 32.dp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = home.name.take(9),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("?", fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                        Text("TBD", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }

                // VS or Score
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (match != null && (match.status == "FINISHED" || match.status == "LIVE")) {
                        Text(
                            text = "${match.homeScore} - ${match.awayScore}",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = Color(0xFFF59E0B)
                        )
                        if (match.homePenalties != null && match.awayPenalties != null) {
                            Text(
                                text = "(${match.homePenalties}-${match.awayPenalties}p)",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    } else {
                        Text("vs", fontWeight = FontWeight.Black, fontSize = 11.sp, color = Color(0xFFF59E0B))
                    }
                }

                // Away Shield
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (away != null) {
                        TeamCrestView(team = away, size = 32.dp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = away.name.take(9),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("?", fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                        Text("TBD", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}

@Composable
private fun WinnerCircleNode(
    team: Team?,
    label: String,
    isDecided: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (isDecided) Color(0xFF0F172A) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .border(
                    width = 2.5.dp,
                    color = if (isDecided) Color(0xFFF59E0B) else Color(0xFFF59E0B).copy(alpha = 0.4f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (team != null) {
                TeamCrestView(team = team, size = 38.dp)
            } else {
                Text(
                    text = "?",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFF59E0B)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = team?.name ?: label,
            fontSize = 11.sp,
            fontWeight = if (isDecided) FontWeight.Bold else FontWeight.Medium,
            color = if (isDecided) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun GrandFinalTrophyNode(
    match: Match?,
    champion: Team?,
    onClick: () -> Unit
) {
    val isFinished = match?.status == "FINISHED"

    Card(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .clickable(enabled = match != null) { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFF59E0B)),
            width = 2.5.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Trophy Node with circle
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
                    .border(3.dp, Color(0xFFF59E0B), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (champion != null) {
                    TeamCrestView(team = champion, size = 48.dp)
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(34.dp)
                        )
                        Text(
                            text = "?",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFF59E0B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (champion != null) {
                Text(
                    text = "¡CAMPEÓN DEL TORNEO!",
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    color = Color(0xFFF59E0B)
                )
                Text(
                    text = champion.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                if (match != null && isFinished) {
                    Text(
                        text = "Marcador Final: ${match.homeScore} - ${match.awayScore}" +
                                if (match.homePenalties != null) " (${match.homePenalties}-${match.awayPenalties} pen.)" else "",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            } else {
                Text(
                    text = "GRAN FINAL",
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    color = Color(0xFFF59E0B)
                )
                Text(
                    text = if (match != null) "Partido por disputar" else "Por definir finalistas",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

@Composable
private fun PhaseHeaderBadge(title: String, isGold: Boolean = false) {
    Box(
        modifier = Modifier
            .background(
                if (isGold) Color(0xFFF59E0B) else MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 14.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (isGold) Color(0xFF0F172A) else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Black,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun PhaseConnectorArrow() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(14.dp)
                .background(Color(0xFFF59E0B).copy(alpha = 0.5f))
        )
        Icon(
            Icons.Default.ArrowDownward,
            contentDescription = null,
            tint = Color(0xFFF59E0B),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun KnockoutMatchCard(
    match: Match,
    teamMap: Map<Long, Team>,
    onMatchClick: (Long) -> Unit
) {
    val home = teamMap[match.homeTeamId]
    val away = teamMap[match.awayTeamId]
    val isFinished = match.status == "FINISHED"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onMatchClick(match.id) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = match.roundName,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (isFinished) "FINALIZADO" else "PROGRAMADO",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isFinished) Color(0xFF10B981) else Color(0xFFF59E0B)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Home team
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    if (home != null) {
                        TeamCrestView(team = home, size = 24.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(home.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    } else {
                        Text("Por definir", color = MaterialTheme.colorScheme.outline, fontSize = 13.sp)
                    }
                }
                Text(
                    text = if (isFinished || match.status == "LIVE") "${match.homeScore}" else "-",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = if (isFinished && match.homeScore > match.awayScore) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Away team
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    if (away != null) {
                        TeamCrestView(team = away, size = 24.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(away.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    } else {
                        Text("Por definir", color = MaterialTheme.colorScheme.outline, fontSize = 13.sp)
                    }
                }
                Text(
                    text = if (isFinished || match.status == "LIVE") "${match.awayScore}" else "-",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = if (isFinished && match.awayScore > match.homeScore) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurface
                )
            }

            if (match.homePenalties != null && match.awayPenalties != null) {
                Text(
                    text = "Penales: (${match.homePenalties} - ${match.awayPenalties})",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

private fun calculateWinnerId(match: Match): Long? {
    if (match.status != "FINISHED") return null
    return if (match.homeScore > match.awayScore) {
        match.homeTeamId
    } else if (match.awayScore > match.homeScore) {
        match.awayTeamId
    } else {
        val hp = match.homePenalties ?: 0
        val ap = match.awayPenalties ?: 0
        if (hp > ap) match.homeTeamId
        else if (ap > hp) match.awayTeamId
        else null
    }
}
