package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.Match
import com.example.data.local.entity.MatchEvent
import com.example.data.local.entity.Player
import com.example.data.local.entity.Team
import com.example.data.local.entity.Tournament
import com.example.data.local.entity.TournamentTeamCrossRef
import com.example.domain.rules.AssisterStat
import com.example.domain.rules.CardStat
import com.example.domain.rules.GoalkeeperStat
import com.example.domain.rules.MvpStat
import com.example.domain.rules.PrizeCalculator
import com.example.domain.rules.ScorerStat
import com.example.domain.rules.StandingsCalculator
import com.example.domain.rules.StatsCalculator
import com.example.domain.rules.TeamStanding
import com.example.ui.viewmodel.TournamentViewModel
import com.example.util.GraphicCardGenerator
import com.example.util.TeamCrestView
import com.example.util.TeamJerseyView
import com.example.util.WhatsAppShareHelper
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ScrollableTabRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentDetailScreen(
    tournamentId: Long,
    viewModel: TournamentViewModel,
    onBack: () -> Unit,
    onMatchClick: (Long) -> Unit,
    onOpenChampionStudio: (championTeam: Team) -> Unit
) {
    val context = LocalContext.current
    val tournaments by viewModel.tournaments.collectAsStateWithLifecycle()
    val allTeams by viewModel.allTeams.collectAsStateWithLifecycle()

    val tournament = tournaments.firstOrNull { it.id == tournamentId }

    var selectedSubTab by remember { mutableIntStateOf(0) }
    var enrolledTeams by remember { mutableStateOf<List<Team>>(emptyList()) }
    var crossRefs by remember { mutableStateOf<List<TournamentTeamCrossRef>>(emptyList()) }
    var matches by remember { mutableStateOf<List<Match>>(emptyList()) }
    var events by remember { mutableStateOf<List<MatchEvent>>(emptyList()) }
    var players by remember { mutableStateOf<List<Player>>(emptyList()) }

    var showEnrollDialog by remember { mutableStateOf(false) }
    var showChampionDialog by remember { mutableStateOf(false) }

    // Repository queries for detail
    val app = com.example.TheBiGApp.instance
    LaunchedEffect(tournamentId) {
        app.repository.getTeamsForTournament(tournamentId).collect { enrolledTeams = it }
    }
    LaunchedEffect(tournamentId) {
        app.repository.getCrossRefsForTournament(tournamentId).collect { crossRefs = it }
    }
    LaunchedEffect(tournamentId) {
        app.repository.getMatchesForTournament(tournamentId).collect { matches = it }
    }
    LaunchedEffect(tournamentId) {
        app.repository.getAllEventsForTournament(tournamentId).collect { events = it }
    }
    LaunchedEffect(enrolledTeams) {
        val tIds = enrolledTeams.map { it.id }
        if (tIds.isNotEmpty()) {
            players = app.repository.getPlayersForTeamsOnce(tIds)
        }
    }

    if (tournament == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Torneo no encontrado")
        }
        return
    }

    val standings = remember(enrolledTeams, matches) {
        StandingsCalculator.calculate(enrolledTeams, matches)
    }

    val topScorers = remember(events, players, enrolledTeams) {
        StatsCalculator.getTopScorers(events, players, enrolledTeams)
    }

    val topAssisters = remember(events, players, enrolledTeams) {
        StatsCalculator.getTopAssisters(events, players, enrolledTeams)
    }

    val goalkeeperRankings = remember(matches, players, enrolledTeams, tournament) {
        StatsCalculator.getGoalkeeperRankings(matches, players, enrolledTeams, tournament.minMatchesForBestGk)
    }

    val topCards = remember(events, players, enrolledTeams) {
        StatsCalculator.getTopCards(events, players, enrolledTeams)
    }

    val topMvps = remember(events, players, enrolledTeams) {
        StatsCalculator.getTopMvps(events, players, enrolledTeams)
    }

    val financialSummary = remember(tournament, crossRefs) {
        PrizeCalculator.calculate(tournament, crossRefs)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = tournament.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Edición ${tournament.edition} • ${enrolledTeams.size} Equipos",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (tournament.championTeamId != null) {
                                val champ = enrolledTeams.firstOrNull { it.id == tournament.championTeamId }
                                if (champ != null) {
                                    onOpenChampionStudio(champ)
                                } else {
                                    showChampionDialog = true
                                }
                            } else {
                                showChampionDialog = true
                            }
                        },
                        modifier = Modifier.testTag("champion_action_button")
                    ) {
                        Icon(
                            Icons.Default.EmojiEvents,
                            contentDescription = "Campeón y Foto",
                            tint = if (tournament.championTeamId != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 4 Sub-Tabs: Tablas | Partidos / Fixture | Estadísticas | Finanzas
            TabRow(
                selectedTabIndex = selectedSubTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedSubTab == 0,
                    onClick = { selectedSubTab = 0 },
                    text = { Text("Tablas", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedSubTab == 1,
                    onClick = { selectedSubTab = 1 },
                    text = { Text("Partidos", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedSubTab == 2,
                    onClick = { selectedSubTab = 2 },
                    text = { Text("Estadísticas", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedSubTab == 3,
                    onClick = { selectedSubTab = 3 },
                    text = { Text("Finanzas", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
            }

            when (selectedSubTab) {
                0 -> StandingsTab(
                    tournament = tournament,
                    standings = standings,
                    enrolledTeams = enrolledTeams,
                    onEnrollClick = { showEnrollDialog = true },
                    onShareText = {
                        val text = WhatsAppShareHelper.buildStandingsText(tournament, standings, topScorers)
                        WhatsAppShareHelper.shareTextToWhatsApp(context, text)
                    },
                    onShareCard = {
                        val bitmap = GraphicCardGenerator.generateStandingsBitmap(tournament, standings)
                        GraphicCardGenerator.shareBitmap(context, bitmap, "Tabla de Posiciones - ${tournament.name}")
                    }
                )
                1 -> FixtureTab(
                    tournamentId = tournamentId,
                    matches = matches,
                    enrolledTeams = enrolledTeams,
                    onGenerateRoundRobin = {
                        viewModel.generateRoundRobinFixture(tournamentId, enrolledTeams)
                    },
                    onMatchClick = onMatchClick
                )
                2 -> StatsTab(
                    topScorers = topScorers,
                    topAssisters = topAssisters,
                    topCards = topCards,
                    topMvps = topMvps,
                    goalkeeperRankings = goalkeeperRankings,
                    minMatchesGk = tournament.minMatchesForBestGk
                )
                3 -> FinancesTab(
                    summary = financialSummary,
                    enrolledTeams = enrolledTeams,
                    crossRefs = crossRefs,
                    onToggleTeamFee = { teamId, currentPaid ->
                        viewModel.toggleTeamFee(tournamentId, teamId, currentPaid)
                    }
                )
            }
        }
    }

    if (showEnrollDialog) {
        EnrollTeamsDialog(
            allTeams = allTeams,
            enrolledTeamIds = enrolledTeams.map { it.id }.toSet(),
            onDismiss = { showEnrollDialog = false },
            onEnroll = { teamId ->
                viewModel.enrollTeam(tournamentId, teamId)
            },
            onRemove = { teamId ->
                viewModel.removeTeam(tournamentId, teamId)
            }
        )
    }

    if (showChampionDialog) {
        SelectChampionDialog(
            enrolledTeams = enrolledTeams,
            currentChampionId = tournament.championTeamId,
            currentRunnerUpId = tournament.runnerUpTeamId,
            onDismiss = { showChampionDialog = false },
            onConfirm = { champId, runnerId ->
                viewModel.setTournamentChampion(tournamentId, champId, runnerId)
                showChampionDialog = false
                val champTeam = enrolledTeams.firstOrNull { it.id == champId }
                if (champTeam != null) {
                    onOpenChampionStudio(champTeam)
                }
            }
        )
    }
}

@Composable
private fun StandingsTab(
    tournament: Tournament,
    standings: List<TeamStanding>,
    enrolledTeams: List<Team>,
    onEnrollClick: () -> Unit,
    onShareText: () -> Unit,
    onShareCard: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Actions Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onEnrollClick,
                modifier = Modifier
                    .weight(1f)
                    .testTag("enroll_teams_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Inscribir Equipos", fontSize = 12.sp)
            }
            Button(
                onClick = onShareText,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("WhatsApp", fontSize = 12.sp)
            }
            Button(
                onClick = onShareCard,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Tarjeta", fontSize = 12.sp)
            }
        }

        if (enrolledTeams.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.GroupAdd,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No hay equipos inscritos en este torneo",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Inscribe al menos dos equipos desde la base de datos de clubes para comenzar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Standings Table Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Tabla General de Posiciones",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Criterios: Puntos > Diferencia de Goles > Enfrentamiento Directo > Goles a Favor",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Table Header Compact [# | Escudo | Nombre | J | Dif | Pts]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("#", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(24.dp))
                        Text("Escudo", fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.width(36.dp))
                        Text("Nombre", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f).padding(start = 4.dp))
                        Text("J", fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.width(32.dp))
                        Text("Dif", fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.width(38.dp))
                        Text("Pts", fontWeight = FontWeight.Black, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center, modifier = Modifier.width(38.dp))
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    standings.forEachIndexed { index, s ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${s.position}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.width(24.dp),
                                color = if (s.position == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Box(modifier = Modifier.width(36.dp), contentAlignment = Alignment.Center) {
                                TeamCrestView(
                                    shieldIndex = s.team.shieldVectorIndex,
                                    primaryColorHex = s.team.primaryColorHex,
                                    secondaryColorHex = s.team.secondaryColorHex,
                                    borderColorHex = s.team.borderColorHex,
                                    customShieldUri = s.team.customShieldUri,
                                    size = 24.dp
                                )
                            }
                            Text(
                                text = s.team.name,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                modifier = Modifier.weight(1f).padding(start = 4.dp)
                            )
                            Text("${s.pj}", fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.width(32.dp))
                            Text(
                                text = if (s.dg > 0) "+${s.dg}" else "${s.dg}",
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium,
                                color = when {
                                    s.dg > 0 -> MaterialTheme.colorScheme.secondary
                                    s.dg < 0 -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.width(38.dp)
                            )
                            Text(
                                text = "${s.pts}",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(38.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FixtureTab(
    tournamentId: Long,
    matches: List<Match>,
    enrolledTeams: List<Team>,
    onGenerateRoundRobin: () -> Unit,
    onMatchClick: (Long) -> Unit
) {
    val teamMap = remember(enrolledTeams) { enrolledTeams.associateBy { it.id } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (matches.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.SportsSoccer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No hay partidos generados",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Genera el calendario automático todos contra todos con los equipos inscritos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onGenerateRoundRobin,
                        enabled = enrolledTeams.size >= 2,
                        modifier = Modifier.testTag("generate_round_robin_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Generar Fixture Todos Contra Todos")
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${matches.size} Partidos Programados",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(
                    onClick = onGenerateRoundRobin,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Regenerar", fontSize = 11.sp)
                }
            }

            val groupedMatches = remember(matches) {
                matches.groupBy { it.roundName.ifBlank { "Jornada 1" } }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                groupedMatches.forEach { (jornadaName, roundMatches) ->
                    item(key = "header_$jornadaName") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = jornadaName,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${roundMatches.size} ${if (roundMatches.size == 1) "partido" else "partidos"}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    items(roundMatches, key = { it.id }) { match ->
                        val home = teamMap[match.homeTeamId]
                        val away = teamMap[match.awayTeamId]
                        MatchRowCard(
                            match = match,
                            homeTeam = home,
                            awayTeam = away,
                            onClick = { onMatchClick(match.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MatchRowCard(
    match: Match,
    homeTeam: Team?,
    awayTeam: Team?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("match_row_${match.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (match.status) {
                "LIVE" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                "FINISHED" -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = match.roundName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                MatchStatusBadge(status = match.status, period = match.currentPeriod)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home Team
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (homeTeam != null) {
                        TeamCrestView(
                            shieldIndex = homeTeam.shieldVectorIndex,
                            primaryColorHex = homeTeam.primaryColorHex,
                            secondaryColorHex = homeTeam.secondaryColorHex,
                            borderColorHex = homeTeam.borderColorHex,
                            customShieldUri = homeTeam.customShieldUri,
                            size = 28.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = homeTeam?.name ?: "Local",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                }

                // Score
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    if (match.status == "SCHEDULED") {
                        Text(
                            text = "VS",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    } else {
                        Text(
                            text = "${match.homeScore} - ${match.awayScore}",
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 16.sp
                        )
                    }
                }

                // Away Team
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = awayTeam?.name ?: "Visitante",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                    if (awayTeam != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        TeamCrestView(
                            shieldIndex = awayTeam.shieldVectorIndex,
                            primaryColorHex = awayTeam.primaryColorHex,
                            secondaryColorHex = awayTeam.secondaryColorHex,
                            borderColorHex = awayTeam.borderColorHex,
                            customShieldUri = awayTeam.customShieldUri,
                            size = 28.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MatchStatusBadge(status: String, period: String) {
    val (label, bg, fg) = when (status) {
        "LIVE" -> Triple("EN VIVO", Color(0xFFEF4444), Color.White)
        "FINISHED" -> Triple("FINALIZADO", MaterialTheme.colorScheme.secondary, Color.White)
        else -> Triple("PROGRAMADO", MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = fg,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp
        )
    }
}

@Composable
private fun StatsTab(
    topScorers: List<ScorerStat>,
    topAssisters: List<AssisterStat>,
    topCards: List<CardStat>,
    topMvps: List<MvpStat>,
    goalkeeperRankings: List<GoalkeeperStat>,
    minMatchesGk: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Scorers (Goles a Favor / Pichichi)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Goles a Favor (Pichichi)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Text(
                    text = "Autogoles excluidos automáticamente del conteo individual",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (topScorers.isEmpty()) {
                    Text(
                        text = "Sin goles registrados todavía",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    topScorers.take(10).forEachIndexed { index, sc ->
                        StatPlayerItemRow(
                            position = index + 1,
                            playerName = sc.player.name,
                            team = sc.team,
                            dorsal = sc.player.dorsal,
                            isGoalkeeper = sc.player.isGoalkeeper,
                            statValue = "${sc.goals} ${if (sc.goals == 1) "gol" else "goles"}",
                            statColor = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // 2. Top Assisters
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Máximos Asistidores",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (topAssisters.isEmpty()) {
                    Text(
                        text = "Sin asistencias registradas todavía",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    topAssisters.take(10).forEachIndexed { index, asst ->
                        StatPlayerItemRow(
                            position = index + 1,
                            playerName = asst.player.name,
                            team = asst.team,
                            dorsal = asst.player.dorsal,
                            isGoalkeeper = asst.player.isGoalkeeper,
                            statValue = "${asst.assists} asist.",
                            statColor = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        // 3. Tarjetas Amarillas / Rojas
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(Color(0xFFEAB308), RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(Color(0xFFEF4444), RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tarjetas Amarillas / Rojas",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Text(
                    text = "Control disciplinario oficial del torneo",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (topCards.isEmpty()) {
                    Text(
                        text = "Sin tarjetas sancionadas todavía",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    topCards.take(10).forEachIndexed { index, card ->
                        StatPlayerItemRow(
                            position = index + 1,
                            playerName = card.player.name,
                            team = card.team,
                            dorsal = card.player.dorsal,
                            isGoalkeeper = card.player.isGoalkeeper,
                            statValue = "${card.yellowCards}A  /  ${card.redCards}R",
                            statColor = if (card.redCards > 0) Color(0xFFEF4444) else Color(0xFFEAB308)
                        )
                    }
                }
            }
        }

        // 4. Jugador del Partido (MVP)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Jugador del Partido (MVP)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Text(
                    text = "Distinciones individuales otorgadas en cada jornada",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (topMvps.isEmpty()) {
                    Text(
                        text = "Sin designaciones de MVP todavía",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    topMvps.take(10).forEachIndexed { index, mvp ->
                        StatPlayerItemRow(
                            position = index + 1,
                            playerName = mvp.player.name,
                            team = mvp.team,
                            dorsal = mvp.player.dorsal,
                            isGoalkeeper = mvp.player.isGoalkeeper,
                            statValue = "${mvp.mvpAwards} MVP",
                            statColor = Color(0xFFF59E0B)
                        )
                    }
                }
            }
        }

        // 5. Goalkeeper Rankings (Valla Menos Vencida)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mejor Portero (Valla Menos Vencida)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Text(
                    text = "Regla de Justicia del Portero: Mínimo $minMatchesGk partidos jugados bajo palos para calificar al trofeo",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (goalkeeperRankings.isEmpty()) {
                    Text(
                        text = "Sin datos de porteros todavía",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    goalkeeperRankings.forEachIndexed { index, gk ->
                        StatPlayerItemRow(
                            position = index + 1,
                            playerName = gk.player.name,
                            team = gk.team,
                            dorsal = gk.player.dorsal,
                            isGoalkeeper = true,
                            statValue = "${gk.goalsConceded} GC (${"%.2f".format(if (gk.coefficient > 100) 0.0 else gk.coefficient)}/p)",
                            statColor = MaterialTheme.colorScheme.primary,
                            extraBadge = if (gk.qualifiesForAward) "Califica" else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPlayerItemRow(
    position: Int,
    playerName: String,
    team: Team?,
    dorsal: Int,
    isGoalkeeper: Boolean,
    statValue: String,
    statColor: Color,
    extraBadge: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Indicador circular numerado (1, 2, 3...)
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(
                    color = when (position) {
                        1 -> Color(0xFFF59E0B) // Oro
                        2 -> Color(0xFF94A3B8) // Plata
                        3 -> Color(0xFFD97706) // Bronce
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$position",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = when (position) {
                    1, 2, 3 -> Color.White
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Miniatura gráfica vectorizada de la camiseta del equipo con sus colores reales
        TeamJerseyView(
            team = team,
            isGoalkeeper = isGoalkeeper,
            dorsal = dorsal,
            size = 28.dp
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = playerName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                if (extraBadge != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = extraBadge,
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Text(
                text = "${team?.name ?: "Sin Club"} • Dorsal #$dorsal",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = statValue,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = statColor
        )
    }
}

@Composable
private fun FinancesTab(
    summary: com.example.domain.rules.TournamentFinancialSummary?,
    enrolledTeams: List<Team>,
    crossRefs: List<TournamentTeamCrossRef>,
    onToggleTeamFee: (teamId: Long, currentPaid: Boolean) -> Unit
) {
    if (summary == null) return

    val teamMap = remember(enrolledTeams) { enrolledTeams.associateBy { it.id } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Recaudo Estimado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$${summary.estimatedRevenue}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                    Text("${summary.enrolledTeamsCount} equipos x $${summary.entryFee}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Recaudo Real", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$${summary.actualRevenue}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.secondary)
                    Text("${summary.paidTeamsCount} equipos cobrados", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                }
            }
        }

        // Teams Payment Control
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Control de Cuota por Equipo",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "Marca los equipos que ya entregaron el dinero de su inscripción",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (crossRefs.isEmpty()) {
                    Text(
                        text = "No hay equipos inscritos todavía",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    crossRefs.forEach { ref ->
                        val team = teamMap[ref.teamId]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleTeamFee(ref.teamId, ref.feePaid) }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = ref.feePaid,
                                    onCheckedChange = { onToggleTeamFee(ref.teamId, ref.feePaid) }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = team?.name ?: "Equipo ${ref.teamId}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                            Text(
                                text = if (ref.feePaid) "Pagado ($${summary.entryFee})" else "Pendiente",
                                fontWeight = FontWeight.Bold,
                                color = if (ref.feePaid) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Prizes Breakdown
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Módulo de Premios",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "Los premios individuales aplican a cualquier jugador registrado aunque su equipo haya sido eliminado",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                summary.prizes.forEach { prize ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(prize.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(prize.winnerNote, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            text = if (prize.enabled) "$${prize.amount}" else "Desactivado",
                            fontWeight = FontWeight.Bold,
                            color = if (prize.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Premios Asignados", fontWeight = FontWeight.Bold)
                    Text("$${summary.totalPrizesAllocated}", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Balance Estimado Final", fontWeight = FontWeight.Bold)
                    Text("$${summary.estimatedNetBalance}", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@Composable
private fun EnrollTeamsDialog(
    allTeams: List<Team>,
    enrolledTeamIds: Set<Long>,
    onDismiss: () -> Unit,
    onEnroll: (Long) -> Unit,
    onRemove: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Inscribir Equipos al Torneo", fontWeight = FontWeight.Bold) },
        text = {
            if (allTeams.isEmpty()) {
                Text("No hay equipos guardados en el banco de clubes. Primero crea equipos en la pestaña 'Equipos'.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allTeams, key = { it.id }) { team ->
                        val isEnrolled = enrolledTeamIds.contains(team.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isEnrolled) onRemove(team.id) else onEnroll(team.id)
                                }
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TeamCrestView(
                                    shieldIndex = team.shieldVectorIndex,
                                    primaryColorHex = team.primaryColorHex,
                                    secondaryColorHex = team.secondaryColorHex,
                                    borderColorHex = team.borderColorHex,
                                    size = 28.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(team.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                            Checkbox(
                                checked = isEnrolled,
                                onCheckedChange = { checked ->
                                    if (checked) onEnroll(team.id) else onRemove(team.id)
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Listo")
            }
        }
    )
}

@Composable
private fun SelectChampionDialog(
    enrolledTeams: List<Team>,
    currentChampionId: Long?,
    currentRunnerUpId: Long?,
    onDismiss: () -> Unit,
    onConfirm: (champId: Long, runnerId: Long?) -> Unit
) {
    var selectedChampId by remember { mutableStateOf(currentChampionId ?: enrolledTeams.firstOrNull()?.id) }
    var selectedRunnerId by remember { mutableStateOf(currentRunnerUpId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Asignar Campeón del Torneo", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Selecciona el equipo Campeón:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                enrolledTeams.forEach { team ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedChampId = team.id }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedChampId == team.id,
                            onCheckedChange = { if (it) selectedChampId = team.id }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(team.name, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Selecciona el equipo Subcampeón (opcional):", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                enrolledTeams.filter { it.id != selectedChampId }.forEach { team ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedRunnerId = team.id }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedRunnerId == team.id,
                            onCheckedChange = { if (it) selectedRunnerId = team.id }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(team.name, fontWeight = FontWeight.Medium)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedChampId != null) {
                        onConfirm(selectedChampId!!, selectedRunnerId)
                    }
                },
                enabled = selectedChampId != null
            ) {
                Text("Continuar a Foto")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
