package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.TheBiGApp
import com.example.data.local.entity.Match
import com.example.data.local.entity.MatchEvent
import com.example.data.local.entity.Player
import com.example.data.local.entity.Team
import com.example.data.local.entity.TournamentModes
import com.example.data.local.entity.TournamentTeamCrossRef
import com.example.domain.rules.PrizeCalculator
import com.example.domain.rules.PrizeCategory
import com.example.domain.rules.StandingsCalculator
import com.example.domain.rules.StatsCalculator
import com.example.domain.rules.TeamStanding
import com.example.domain.rules.TournamentScheduler
import com.example.ui.components.BracketDiagramView
import com.example.ui.viewmodel.TournamentViewModel
import com.example.util.GraphicCardGenerator
import com.example.util.TeamCrestView
import com.example.util.TheBiGBackupHelper
import kotlinx.coroutines.launch

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
    val scope = rememberCoroutineScope()

    val app = TheBiGApp.instance
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

    val financialSummary = remember(tournament, crossRefs, events, players) {
        PrizeCalculator.calculate(tournament, crossRefs, events, players)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        // Tournament title without "Edición X"
                        Text(
                            text = tournament.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${enrolledTeams.size} Equipos • ${formatModeName(tournament.tournamentType)}",
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
                    // Export .tb file
                    IconButton(
                        onClick = {
                            scope.launch {
                                val uri = TheBiGBackupHelper.exportTournamentTbFile(context, app.repository, tournamentId)
                                if (uri != null) {
                                    TheBiGBackupHelper.shareTbFile(context, uri, tournament.name)
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Compartir .tb")
                    }

                    // Champion & Trophy action
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
                            tint = if (tournament.championTeamId != null) Color(0xFFF59E0B) else MaterialTheme.colorScheme.outline
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
            TabRow(
                selectedTabIndex = selectedSubTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedSubTab == 0,
                    onClick = { selectedSubTab = 0 },
                    text = { Text("Tablas", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedSubTab == 1,
                    onClick = { selectedSubTab = 1 },
                    text = { Text("Fixture", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedSubTab == 2,
                    onClick = { selectedSubTab = 2 },
                    text = { Text("Líderes", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedSubTab == 3,
                    onClick = { selectedSubTab = 3 },
                    text = { Text("Finanzas", fontWeight = FontWeight.Bold) }
                )
            }

            when (selectedSubTab) {
                0 -> StandingsTabContent(
                    tournament = tournament,
                    standings = standings,
                    matches = matches,
                    enrolledTeams = enrolledTeams,
                    onMatchClick = onMatchClick,
                    onExportTable = {
                        val bmp = GraphicCardGenerator.generateStandingsInstagramBitmap(tournament, standings)
                        GraphicCardGenerator.shareBitmap(context, bmp, "Compartir Tabla")
                    },
                    onOpenEnroll = { showEnrollDialog = true }
                )
                1 -> FixtureTabContent(
                    tournament = tournament,
                    matches = matches,
                    enrolledTeams = enrolledTeams,
                    onGenerate = {
                        scope.launch {
                            val newMatches = TournamentScheduler.generateSchedule(
                                tournamentId = tournamentId,
                                tournamentType = tournament.tournamentType,
                                teams = enrolledTeams,
                                homeAndAway = false
                            )
                            for (m in newMatches) {
                                app.repository.saveMatch(m)
                            }
                        }
                    },
                    onMatchClick = onMatchClick,
                    onExportFixture = {
                        val teamMap = enrolledTeams.associateBy { it.id }
                        val bmp = GraphicCardGenerator.generateFixtureInstagramBitmap(tournament, matches, teamMap)
                        GraphicCardGenerator.shareBitmap(context, bmp, "Compartir Fixture")
                    }
                )
                2 -> StatisticsTabContent(
                    topScorers = topScorers,
                    topAssisters = topAssisters,
                    goalkeepers = goalkeeperRankings,
                    onExportScorers = {
                        val bmp = GraphicCardGenerator.generateScorersInstagramBitmap(tournament, topScorers)
                        GraphicCardGenerator.shareBitmap(context, bmp, "Compartir Goleadores")
                    }
                )
                3 -> IsolatedFinancesTabContent(
                    tournament = tournament,
                    enrolledTeams = enrolledTeams,
                    players = players,
                    crossRefs = crossRefs,
                    onTogglePlayerFee = { playerId, current ->
                        scope.launch {
                            app.repository.updatePlayerFeeStatus(playerId, !current)
                        }
                    },
                    onUpdatePrizes = { ch, ru, sc, gk ->
                        viewModel.updateTournament(
                            tournament.copy(
                                prizeChampionAmount = ch,
                                prizeRunnerUpAmount = ru,
                                prizeTopScorerAmount = sc,
                                prizeBestGkAmount = gk
                            )
                        )
                    }
                )
            }
        }
    }

    if (showEnrollDialog) {
        val enrolledIds = enrolledTeams.map { it.id }.toSet()
        val availableTeams = allTeams.filter { !enrolledIds.contains(it.id) }

        EnrollTeamsDialog(
            availableTeams = availableTeams,
            enrolledTeams = enrolledTeams,
            onDismiss = { showEnrollDialog = false },
            onEnroll = { teamId -> viewModel.enrollTeam(tournamentId, teamId, feePaid = false) },
            onRemove = { teamId -> viewModel.removeTeamFromTournament(tournamentId, teamId) }
        )
    }

    if (showChampionDialog) {
        ChampionSelectDialog(
            teams = enrolledTeams,
            currentChampId = tournament.championTeamId,
            onDismiss = { showChampionDialog = false },
            onSelect = { champId, runnerUpId ->
                viewModel.setChampion(tournamentId, champId, runnerUpId)
                showChampionDialog = false
            }
        )
    }
}

@Composable
private fun StandingsTabContent(
    tournament: com.example.data.local.entity.Tournament,
    standings: List<TeamStanding>,
    matches: List<Match>,
    enrolledTeams: List<Team>,
    onMatchClick: (Long) -> Unit,
    onExportTable: () -> Unit,
    onOpenEnroll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onExportTable,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("export_standings_button")
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Compartir Tabla", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = onOpenEnroll,
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Inscribir Clubes", fontSize = 12.sp)
            }
        }

        // Bracket view if tournament has knockout stages
        if (tournament.tournamentType == TournamentModes.CUP || tournament.tournamentType == TournamentModes.GROUPS_CUP) {
            BracketDiagramView(matches = matches, teams = enrolledTeams, onMatchClick = onMatchClick)
            Spacer(modifier = Modifier.height(4.dp))
        }

        if (standings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Inscribe al menos 2 equipos para visualizar la tabla de posiciones.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // Perfectly aligned Standings Table Header: PJ, PG, PE, PP, GF, GC, DG, PTS
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "#  Club",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TableHeaderCell("PJ")
                        TableHeaderCell("PG")
                        TableHeaderCell("PE")
                        TableHeaderCell("PP")
                        TableHeaderCell("GF")
                        TableHeaderCell("GC")
                        TableHeaderCell("DG")
                        TableHeaderCell("PTS", isHighlight = true)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(standings, key = { it.team.id }) { s ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("${s.position}.", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(20.dp))
                                TeamCrestView(team = s.team, size = 24.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(s.team.name, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1)
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TableDataCell("${s.pj}")
                                TableDataCell("${s.pg}")
                                TableDataCell("${s.pe}")
                                TableDataCell("${s.pp}")
                                TableDataCell("${s.gf}")
                                TableDataCell("${s.gc}")
                                TableDataCell("${s.dg}")
                                TableDataCell("${s.pts}", isHighlight = true)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TableHeaderCell(text: String, isHighlight: Boolean = false) {
    Text(
        text = text,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        textAlign = TextAlign.Center,
        color = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.width(24.dp)
    )
}

@Composable
private fun TableDataCell(text: String, isHighlight: Boolean = false) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = if (isHighlight) FontWeight.Black else FontWeight.Normal,
        textAlign = TextAlign.Center,
        color = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.width(24.dp)
    )
}

@Composable
private fun FixtureTabContent(
    tournament: com.example.data.local.entity.Tournament,
    matches: List<Match>,
    enrolledTeams: List<Team>,
    onGenerate: () -> Unit,
    onMatchClick: (Long) -> Unit,
    onExportFixture: () -> Unit
) {
    val teamMap = remember(enrolledTeams) { enrolledTeams.associateBy { it.id } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onExportFixture,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                enabled = matches.isNotEmpty()
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Compartir Fixture", fontSize = 12.sp)
            }

            if (matches.isEmpty() && enrolledTeams.size >= 2) {
                Button(
                    onClick = onGenerate,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Generar Calendario", fontSize = 12.sp)
                }
            }
        }

        if (matches.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (enrolledTeams.size < 2) "Inscribe al menos 2 equipos para generar el fixture."
                    else "Presiona 'Generar Calendario' para calcular los partidos según el modo ${formatModeName(tournament.tournamentType)}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(matches, key = { it.id }) { match ->
                    val homeTeam = teamMap[match.homeTeamId]
                    val awayTeam = teamMap[match.awayTeamId]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onMatchClick(match.id) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (match.status == "LIVE") MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(match.roundName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TeamCrestView(team = homeTeam, size = 24.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(homeTeam?.name ?: "Local", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TeamCrestView(team = awayTeam, size = 24.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(awayTeam?.name ?: "Visitante", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                if (match.status == "FINISHED" || match.status == "LIVE") {
                                    Text("${match.homeScore} - ${match.awayScore}", fontWeight = FontWeight.Black, fontSize = 18.sp)
                                } else {
                                    Text("VS", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.outline)
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = when (match.status) {
                                        "LIVE" -> Color(0xFF10B981)
                                        "FINISHED" -> MaterialTheme.colorScheme.outlineVariant
                                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    },
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text(
                                        text = when (match.status) {
                                            "LIVE" -> "EN VIVO"
                                            "FINISHED" -> "FINAL"
                                            else -> "PROGRAMADO"
                                        },
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (match.status == "LIVE") Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatisticsTabContent(
    topScorers: List<com.example.domain.rules.ScorerStat>,
    topAssisters: List<com.example.domain.rules.AssisterStat>,
    goalkeepers: List<com.example.domain.rules.GoalkeeperStat>,
    onExportScorers: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Líderes y Distinciones", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Button(
                onClick = onExportScorers,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Compartir Goleadores", fontSize = 12.sp)
            }
        }

        // Top Scorers
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SportsSoccer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Máximos Goleadores (Pichichi)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                if (topScorers.isEmpty()) {
                    Text("Sin goles registrados", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    topScorers.take(5).forEachIndexed { index, s ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${index + 1}. ${s.player.name} (${s.team.shortName})", fontSize = 12.sp)
                            Text("${s.goals} goles", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        // Top Assisters
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Máximos Asistidores", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                if (topAssisters.isEmpty()) {
                    Text("Sin asistencias registradas", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    topAssisters.take(5).forEachIndexed { index, a ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${index + 1}. ${a.player.name} (${a.team.shortName})", fontSize = 12.sp)
                            Text("${a.assists} asistencias", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }

        // Top Goalkeepers (Valla Menos Vencida)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Valla Menos Vencida (Porteros)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                if (goalkeepers.isEmpty()) {
                    Text("Sin estadísticas de portería", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    goalkeepers.take(5).forEachIndexed { index, gk ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${index + 1}. ${gk.player.name} (${gk.team.shortName})", fontSize = 12.sp)
                            Text("${gk.goalsConceded} goles (${gk.matchesPlayed} PJ)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF10B981))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Isolated Finances Tab with auto-balancing 100% prizes and player fee checklist.
 */
@Composable
private fun IsolatedFinancesTabContent(
    tournament: com.example.data.local.entity.Tournament,
    enrolledTeams: List<Team>,
    players: List<Player>,
    crossRefs: List<TournamentTeamCrossRef>,
    onTogglePlayerFee: (playerId: Long, current: Boolean) -> Unit,
    onUpdatePrizes: (champion: Double, runnerUp: Double, scorer: Double, bestGk: Double) -> Unit
) {
    val totalTeams = enrolledTeams.size
    val totalCollected = if (tournament.financialModel == "BY_PLAYER") {
        PrizeCalculator.roundToTen(tournament.feePerPlayer * (tournament.playersPerTeam.coerceAtLeast(1)) * totalTeams)
    } else {
        PrizeCalculator.roundToTen((if (tournament.totalTeamFee > 0) tournament.totalTeamFee else tournament.entryFee) * totalTeams)
    }

    // Sliders for dynamic 100% hierarchical rebalancing (Campeón >= Subcampeón >= Resto)
    var percentages by remember(tournament) {
        val total = (tournament.prizeChampionAmount + tournament.prizeRunnerUpAmount + tournament.prizeTopScorerAmount + tournament.prizeBestGkAmount).coerceAtLeast(1.0)
        mutableStateOf<Map<PrizeCategory, Float>>(
            mapOf(
                PrizeCategory.CHAMPION to ((tournament.prizeChampionAmount / total) * 100f).toFloat(),
                PrizeCategory.RUNNER_UP to ((tournament.prizeRunnerUpAmount / total) * 100f).toFloat(),
                PrizeCategory.TOP_SCORER to ((tournament.prizeTopScorerAmount / total) * 100f).toFloat(),
                PrizeCategory.BEST_GK to ((tournament.prizeBestGkAmount / total) * 100f).toFloat()
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Pool Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF59E0B).copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("POZO TOTAL DE PREMIOS (REDONDEO A 10)", color = Color(0xFFF59E0B), fontWeight = FontWeight.Black, fontSize = 11.sp)
                    val feeDesc = if (tournament.financialModel == "BY_PLAYER") {
                        "$totalTeams Clubes × ($${tournament.feePerPlayer.toInt()} × ${tournament.playersPerTeam} jug)"
                    } else {
                        "$totalTeams Clubes × $${(if (tournament.totalTeamFee > 0) tournament.totalTeamFee else tournament.entryFee).toInt()}"
                    }
                    Text(feeDesc, fontSize = 12.sp)
                }
                Text("$${"%.0f".format(totalCollected)}", fontWeight = FontWeight.Black, fontSize = 22.sp, color = Color(0xFFF59E0B))
            }
        }

        // Proportional Hierarchical Auto-balancing Sliders (Sum = 100%, Campeón >= Subcampeón)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Reparto Proporcional de Premios (Total 100%):", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                // Champion Slider
                PrizeSliderRow(
                    label = "Campeón",
                    percentage = percentages[PrizeCategory.CHAMPION] ?: 50f,
                    amount = PrizeCalculator.roundToTen(totalCollected * ((percentages[PrizeCategory.CHAMPION] ?: 50f).toDouble() / 100.0)),
                    color = Color(0xFF10B981),
                    onValueChange = { newVal ->
                        percentages = PrizeCalculator.rebalanceHierarchicalPercentages(PrizeCategory.CHAMPION, newVal, percentages)
                        val ch = PrizeCalculator.roundToTen(totalCollected * ((percentages[PrizeCategory.CHAMPION] ?: 0f).toDouble() / 100.0))
                        val ru = PrizeCalculator.roundToTen(totalCollected * ((percentages[PrizeCategory.RUNNER_UP] ?: 0f).toDouble() / 100.0))
                        val sc = PrizeCalculator.roundToTen(totalCollected * ((percentages[PrizeCategory.TOP_SCORER] ?: 0f).toDouble() / 100.0))
                        val gk = PrizeCalculator.roundToTen(totalCollected * ((percentages[PrizeCategory.BEST_GK] ?: 0f).toDouble() / 100.0))
                        onUpdatePrizes(ch, ru, sc, gk)
                    }
                )

                // Runner Up Slider
                PrizeSliderRow(
                    label = "Subcampeón",
                    percentage = percentages[PrizeCategory.RUNNER_UP] ?: 25f,
                    amount = PrizeCalculator.roundToTen(totalCollected * ((percentages[PrizeCategory.RUNNER_UP] ?: 25f).toDouble() / 100.0)),
                    color = Color(0xFF3B82F6),
                    onValueChange = { newVal ->
                        percentages = PrizeCalculator.rebalanceHierarchicalPercentages(PrizeCategory.RUNNER_UP, newVal, percentages)
                        val ch = PrizeCalculator.roundToTen(totalCollected * ((percentages[PrizeCategory.CHAMPION] ?: 0f).toDouble() / 100.0))
                        val ru = PrizeCalculator.roundToTen(totalCollected * ((percentages[PrizeCategory.RUNNER_UP] ?: 0f).toDouble() / 100.0))
                        val sc = PrizeCalculator.roundToTen(totalCollected * ((percentages[PrizeCategory.TOP_SCORER] ?: 0f).toDouble() / 100.0))
                        val gk = PrizeCalculator.roundToTen(totalCollected * ((percentages[PrizeCategory.BEST_GK] ?: 0f).toDouble() / 100.0))
                        onUpdatePrizes(ch, ru, sc, gk)
                    }
                )

                // Top Scorer Slider
                PrizeSliderRow(
                    label = "Goleador",
                    percentage = percentages[PrizeCategory.TOP_SCORER] ?: 15f,
                    amount = PrizeCalculator.roundToTen(totalCollected * ((percentages[PrizeCategory.TOP_SCORER] ?: 15f).toDouble() / 100.0)),
                    color = Color(0xFFEC4899),
                    onValueChange = { newVal ->
                        percentages = PrizeCalculator.rebalanceHierarchicalPercentages(PrizeCategory.TOP_SCORER, newVal, percentages)
                        val ch = PrizeCalculator.roundToTen(totalCollected * ((percentages[PrizeCategory.CHAMPION] ?: 0f).toDouble() / 100.0))
                        val ru = PrizeCalculator.roundToTen(totalCollected * ((percentages[PrizeCategory.RUNNER_UP] ?: 0f).toDouble() / 100.0))
                        val sc = PrizeCalculator.roundToTen(totalCollected * ((percentages[PrizeCategory.TOP_SCORER] ?: 0f).toDouble() / 100.0))
                        val gk = PrizeCalculator.roundToTen(totalCollected * ((percentages[PrizeCategory.BEST_GK] ?: 0f).toDouble() / 100.0))
                        onUpdatePrizes(ch, ru, sc, gk)
                    }
                )

                // Best GK Slider
                PrizeSliderRow(
                    label = "Valla Menos Vencida",
                    percentage = percentages[PrizeCategory.BEST_GK] ?: 10f,
                    amount = PrizeCalculator.roundToTen(totalCollected * ((percentages[PrizeCategory.BEST_GK] ?: 10f).toDouble() / 100.0)),
                    color = Color(0xFFF59E0B),
                    onValueChange = { newVal ->
                        percentages = PrizeCalculator.rebalanceHierarchicalPercentages(PrizeCategory.BEST_GK, newVal, percentages)
                        val ch = PrizeCalculator.roundToTen(totalCollected * ((percentages[PrizeCategory.CHAMPION] ?: 0f).toDouble() / 100.0))
                        val ru = PrizeCalculator.roundToTen(totalCollected * ((percentages[PrizeCategory.RUNNER_UP] ?: 0f).toDouble() / 100.0))
                        val sc = PrizeCalculator.roundToTen(totalCollected * ((percentages[PrizeCategory.TOP_SCORER] ?: 0f).toDouble() / 100.0))
                        val gk = PrizeCalculator.roundToTen(totalCollected * ((percentages[PrizeCategory.BEST_GK] ?: 0f).toDouble() / 100.0))
                        onUpdatePrizes(ch, ru, sc, gk)
                    }
                )
            }
        }

        // Player Fee Checklist
        Text("Control de Cuotas por Jugador:", fontWeight = FontWeight.Bold, fontSize = 14.sp)

        if (enrolledTeams.isEmpty()) {
            Text("No hay clubes inscritos en el torneo.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            enrolledTeams.forEach { team ->
                val teamPlayers = players.filter { it.teamId == team.id }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TeamCrestView(team = team, size = 26.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(team.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        if (teamPlayers.isEmpty()) {
                            Text("Sin jugadores en la plantilla", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            teamPlayers.forEach { p ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onTogglePlayerFee(p.id, p.feePaid) }
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("#${p.dorsal} ${p.name}", fontSize = 12.sp)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (p.feePaid) "Pagado" else "Pendiente",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (p.feePaid) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                                        )
                                        Checkbox(checked = p.feePaid, onCheckedChange = { onTogglePlayerFee(p.id, p.feePaid) })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrizeSliderRow(
    label: String,
    percentage: Float,
    amount: Double,
    color: Color,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$label: ${percentage.toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text("$${"%.2f".format(amount)}", color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        Slider(
            value = percentage,
            onValueChange = onValueChange,
            valueRange = 0f..100f
        )
    }
}

private fun formatModeName(mode: String): String {
    return when (mode) {
        TournamentModes.LEAGUE -> "Liga Tradicional"
        TournamentModes.CUP -> "Copa (Eliminatoria)"
        TournamentModes.GROUPS_CUP -> "Grupos + Copa"
        TournamentModes.MIXED -> "Modo Mixto"
        else -> mode
    }
}

@Composable
private fun EnrollTeamsDialog(
    availableTeams: List<Team>,
    enrolledTeams: List<Team>,
    onDismiss: () -> Unit,
    onEnroll: (Long) -> Unit,
    onRemove: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gestionar Clubes Participantes", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Inscritos (${enrolledTeams.size}):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                if (enrolledTeams.isEmpty()) {
                    Text("No hay equipos inscritos.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    enrolledTeams.forEach { t ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TeamCrestView(team = t, size = 24.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(t.name, fontSize = 13.sp)
                            }
                            IconButton(onClick = { onRemove(t.id) }) {
                                Icon(Icons.Default.Close, contentDescription = "Remover", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text("Disponibles para inscribir:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                if (availableTeams.isEmpty()) {
                    Text("Todos los clubes creados ya están inscritos.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    availableTeams.forEach { t ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TeamCrestView(team = t, size = 24.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(t.name, fontSize = 13.sp)
                            }
                            Button(
                                onClick = { onEnroll(t.id) },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Inscribir", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Listo") }
        }
    )
}

@Composable
private fun ChampionSelectDialog(
    teams: List<Team>,
    currentChampId: Long?,
    onDismiss: () -> Unit,
    onSelect: (champId: Long, runnerUpId: Long?) -> Unit
) {
    var selectedChampId by remember { mutableStateOf(currentChampId) }
    var selectedRunnerUpId by remember { mutableStateOf<Long?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Asignar Campeón Oficial", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Selecciona el equipo Campeón del torneo:")
                teams.forEach { team ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedChampId = team.id }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = selectedChampId == team.id,
                            onClick = { selectedChampId = team.id }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        TeamCrestView(team = team, size = 26.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(team.name, fontSize = 13.sp, fontWeight = if (selectedChampId == team.id) FontWeight.Bold else FontWeight.Normal)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("Selecciona el Subcampeón (Opcional):")
                teams.filter { it.id != selectedChampId }.forEach { team ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedRunnerUpId = team.id }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = selectedRunnerUpId == team.id,
                            onClick = { selectedRunnerUpId = team.id }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        TeamCrestView(team = team, size = 26.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(team.name, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedChampId?.let { onSelect(it, selectedRunnerUpId) }
                },
                enabled = selectedChampId != null
            ) {
                Text("Guardar Campeón")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
