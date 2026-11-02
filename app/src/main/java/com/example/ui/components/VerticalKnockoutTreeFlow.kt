package com.example.ui.components

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.Match
import com.example.data.local.entity.Team
import com.example.util.TeamCrestView

@Composable
fun VerticalKnockoutTreeFlow(
    matches: List<Match>,
    teams: List<Team>,
    onMatchClick: (Long) -> Unit
) {
    val teamMap = remember(teams) { teams.associateBy { it.id } }

    val repechaje = matches.filter { it.roundName.contains("Repechaje", ignoreCase = true) || it.roundName.contains("Playoff", ignoreCase = true) }
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
        // Classification indicator divider
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
                    text = "CLASIFICADOS A FASE DE ELIMINACIÓN DIRECTA",
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
        }

        // Section 1: Repechaje / PlayOffs (if present)
        if (repechaje.isNotEmpty()) {
            PhaseHeaderBadge("REPECHAJE Y PLAYOFFS PREVIOS")
            repechaje.forEach { m ->
                KnockoutMatchCard(match = m, teamMap = teamMap, onMatchClick = onMatchClick)
            }
            PhaseConnectorArrow()
        }

        // Section 2: Cuartos de Final
        if (quarters.isNotEmpty()) {
            PhaseHeaderBadge("CUARTOS DE FINAL")
            quarters.forEach { m ->
                KnockoutMatchCard(match = m, teamMap = teamMap, onMatchClick = onMatchClick)
            }
            PhaseConnectorArrow()
        }

        // Section 3: Semifinales
        if (semis.isNotEmpty()) {
            PhaseHeaderBadge("SEMIFINALES")
            semis.forEach { m ->
                KnockoutMatchCard(match = m, teamMap = teamMap, onMatchClick = onMatchClick)
            }
            PhaseConnectorArrow()
        }

        // Section 4: GRAN FINAL
        PhaseHeaderBadge("GRAN FINAL DE CAMPEONATO", isGold = true)
        if (grandFinal.isNotEmpty()) {
            grandFinal.forEach { m ->
                GrandFinalCard(match = m, teamMap = teamMap, onMatchClick = onMatchClick)
            }
        } else {
            // Future placeholder for Gran Final
            FutureGrandFinalCard()
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
                    text = if (isFinished || match.status == "IN_PROGRESS") "${match.homeScore}" else "-",
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
                    text = if (isFinished || match.status == "IN_PROGRESS") "${match.awayScore}" else "-",
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

@Composable
private fun GrandFinalCard(
    match: Match,
    teamMap: Map<Long, Team>,
    onMatchClick: (Long) -> Unit
) {
    val home = teamMap[match.homeTeamId]
    val away = teamMap[match.awayTeamId]
    val isFinished = match.status == "FINISHED"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.5.dp, Color(0xFFF59E0B), RoundedCornerShape(16.dp))
            .background(Color(0xFF0F172A), RoundedCornerShape(16.dp))
            .clickable { onMatchClick(match.id) }
            .padding(14.dp)
    ) {
        // Watermark Gold Trophy in background
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .alpha(0.08f)
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = Color(0xFFF59E0B),
                modifier = Modifier.size(110.dp)
            )
        }

        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("GRAN FINAL", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color(0xFFF59E0B))
                }
                Text(
                    text = if (isFinished) "¡CAMPEONATO DEFINIDO!" else "POR DISPUTAR",
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = if (isFinished) Color(0xFF10B981) else Color(0xFFF59E0B)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    if (home != null) {
                        TeamCrestView(team = home, size = 44.dp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(home.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White, textAlign = TextAlign.Center)
                    } else {
                        Text("Finalista 1", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                }

                // Score
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isFinished) "${match.homeScore} - ${match.awayScore}" else "VS",
                        fontWeight = FontWeight.Black,
                        fontSize = if (isFinished) 26.sp else 18.sp,
                        color = Color(0xFFF59E0B)
                    )
                    if (match.homePenalties != null && match.awayPenalties != null) {
                        Text(
                            text = "(${match.homePenalties} - ${match.awayPenalties} pen.)",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                // Away
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    if (away != null) {
                        TeamCrestView(team = away, size = 44.dp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(away.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White, textAlign = TextAlign.Center)
                    } else {
                        Text("Finalista 2", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun FutureGrandFinalCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(0.5f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text("GRAN FINAL", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
            Text(
                "Se disputará entre los ganadores de Semifinales",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
