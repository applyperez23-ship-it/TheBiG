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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import com.example.R
import com.example.util.TeamJerseyView
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
    var showCelebrationDialog by remember { mutableStateOf(false) }
    var selectedMatchForSquad by remember { mutableStateOf<Match?>(null) }
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
                                showCelebrationDialog = true
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
            val pagerState = rememberPagerState(pageCount = { 4 })

            LaunchedEffect(pagerState.currentPage) {
                selectedSubTab = pagerState.currentPage
            }

            LaunchedEffect(selectedSubTab) {
                if (pagerState.currentPage != selectedSubTab) {
                    pagerState.animateScrollToPage(selectedSubTab)
                }
            }

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

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> StandingsTabContent(
                        tournament = tournament,
                        standings = standings,
                        matches = matches,
                        enrolledTeams = enrolledTeams,
                        onMatchClick = onMatchClick,
                        onExportTable = {
                            val bmp = GraphicCardGenerator.generateStandingsInstagramBitmap(tournament, standings, context)
                            GraphicCardGenerator.shareBitmap(context, bmp, "Compartir Tabla")
                        },
                        onOpenEnroll = { showEnrollDialog = true },
                        onCelebrateChampion = { showCelebrationDialog = true }
                    )
                    1 -> FixtureTabContent(
                        tournament = tournament,
                        matches = matches,
                        enrolledTeams = enrolledTeams,
                        players = players,
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
                        onAdvancePhase = {
                            scope.launch {
                                val nextMatches = TournamentScheduler.advanceToNextPhase(
                                    tournamentId = tournamentId,
                                    tournamentType = tournament.tournamentType,
                                    allMatches = matches,
                                    enrolledTeams = enrolledTeams
                                )
                                for (m in nextMatches) {
                                    app.repository.saveMatch(m)
                                }
                            }
                        },
                        onMatchClick = onMatchClick,
                        onShowRoster = { match -> selectedMatchForSquad = match },
                        onExportFixture = {
                            val teamMap = enrolledTeams.associateBy { it.id }
                            val bmp = GraphicCardGenerator.generateFixtureInstagramBitmap(tournament, matches, teamMap)
                            GraphicCardGenerator.shareBitmap(context, bmp, "Compartir Fixture")
                        }
                    )
                    2 -> StatisticsTabContent(
                        tournament = tournament,
                        topScorers = topScorers,
                        topAssisters = topAssisters,
                        goalkeepers = goalkeeperRankings,
                        standings = standings,
                        context = context,
                        onExportScorers = {
                            val bmp = GraphicCardGenerator.generateScorersInstagramBitmap(tournament, topScorers)
                            GraphicCardGenerator.shareBitmap(context, bmp, "Compartir Goleadores")
                        },
                        onExportUnifiedLeaders = {
                            val bmp = GraphicCardGenerator.generateUnifiedLeadersBitmap(
                                tournament = tournament,
                                scorers = topScorers,
                                assisters = topAssisters,
                                goalkeepers = goalkeeperRankings,
                                standings = standings,
                                context = context
                            )
                            GraphicCardGenerator.shareBitmap(context, bmp, "Líderes Oficiales - ${tournament.name}")
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
                        },
                        onUpdateCardFines = { yellow, red, mvpPool ->
                            viewModel.updateTournament(
                                tournament.copy(
                                    yellowCardFine = yellow,
                                    redCardFine = red,
                                    prizeMvpEnabled = mvpPool
                                )
                            )
                        }
                    )
                }
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

    if (showCelebrationDialog && tournament.championTeamId != null) {
        val champ = enrolledTeams.firstOrNull { it.id == tournament.championTeamId }
        val runnerUp = enrolledTeams.firstOrNull { it.id == tournament.runnerUpTeamId }
        if (champ != null) {
            ChampionCelebrationDialog(
                tournament = tournament,
                championTeam = champ,
                runnerUpTeam = runnerUp,
                onOpenStudio = {
                    showCelebrationDialog = false
                    onOpenChampionStudio(champ)
                },
                onChangeChampion = {
                    showCelebrationDialog = false
                    showChampionDialog = true
                },
                onDismiss = { showCelebrationDialog = false }
            )
        }
    }

    selectedMatchForSquad?.let { m ->
        val homeT = enrolledTeams.firstOrNull { it.id == m.homeTeamId }
        val awayT = enrolledTeams.firstOrNull { it.id == m.awayTeamId }
        val hPlayers = players.filter { it.teamId == m.homeTeamId }
        val aPlayers = players.filter { it.teamId == m.awayTeamId }
        MatchSquadRosterDialog(
            match = m,
            homeTeam = homeT,
            awayTeam = awayT,
            homePlayers = hPlayers,
            awayPlayers = aPlayers,
            onDismiss = { selectedMatchForSquad = null }
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
    onOpenEnroll: () -> Unit,
    onCelebrateChampion: () -> Unit
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

        if (tournament.championTeamId != null) {
            val champTeam = enrolledTeams.firstOrNull { it.id == tournament.championTeamId }
            if (champTeam != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCelebrateChampion() },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF59E0B).copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TeamCrestView(team = champTeam, size = 36.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("¡CAMPEÓN DEL TORNEO!", fontWeight = FontWeight.Black, fontSize = 11.sp, color = Color(0xFFF59E0B))
                                Text(champTeam.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                        Button(
                            onClick = onCelebrateChampion,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                        ) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Celebrar", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.Black)
                        }
                    }
                }
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
    players: List<Player>,
    onGenerate: () -> Unit,
    onAdvancePhase: () -> Unit,
    onMatchClick: (Long) -> Unit,
    onShowRoster: (Match) -> Unit,
    onExportFixture: () -> Unit
) {
    val teamMap = remember(enrolledTeams) { enrolledTeams.associateBy { it.id } }
    val canAdvance = remember(tournament, matches) {
        TournamentScheduler.canAdvanceToNextPhase(tournament.tournamentType, matches)
    }

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

        // Siguiente Fase Interactive Button
        if (canAdvance) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.18f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("¡FASE ACTUAL CONCLUIDA!", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color(0xFF10B981))
                        Text("Todos los cruces fueron completados. Avanza a la siguiente etapa.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = onAdvancePhase,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Siguiente Fase", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(match.roundName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = { onShowRoster(match) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.FormatListNumbered,
                                            contentDescription = "Alineación",
                                            modifier = Modifier.size(15.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
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
    tournament: com.example.data.local.entity.Tournament,
    topScorers: List<com.example.domain.rules.ScorerStat>,
    topAssisters: List<com.example.domain.rules.AssisterStat>,
    goalkeepers: List<com.example.domain.rules.GoalkeeperStat>,
    standings: List<TeamStanding>,
    context: android.content.Context,
    onExportScorers: () -> Unit,
    onExportUnifiedLeaders: () -> Unit
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
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = onExportUnifiedLeaders,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Líderes 1:1", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.Black)
                }
                OutlinedButton(
                    onClick = onExportScorers,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Goleadores", fontSize = 11.sp)
                }
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
 * Isolated Finances Tab with auto-balancing 100% prizes, card fines, and instant reaction player fee checklist.
 */
@Composable
private fun IsolatedFinancesTabContent(
    tournament: com.example.data.local.entity.Tournament,
    enrolledTeams: List<Team>,
    players: List<Player>,
    crossRefs: List<TournamentTeamCrossRef>,
    onTogglePlayerFee: (playerId: Long, current: Boolean) -> Unit,
    onUpdatePrizes: (champion: Double, runnerUp: Double, scorer: Double, bestGk: Double) -> Unit,
    onUpdateCardFines: (yellow: Double, red: Double, mvpPool: Boolean) -> Unit
) {
    val totalTeams = enrolledTeams.size
    val totalCollected = if (tournament.financialModel == "BY_PLAYER") {
        PrizeCalculator.roundToTen(tournament.feePerPlayer * (tournament.playersPerTeam.coerceAtLeast(1)) * totalTeams)
    } else {
        PrizeCalculator.roundToTen((if (tournament.totalTeamFee > 0) tournament.totalTeamFee else tournament.entryFee) * totalTeams)
    }

    // Local instant checkbox reaction map
    val localFeeMap = remember { mutableStateMapOf<Long, Boolean>() }
    LaunchedEffect(players) {
        players.forEach { p ->
            localFeeMap[p.id] = p.feePaid
        }
    }

    // Sliders for dynamic 100% hierarchical rebalancing
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

    var yellowFineText by remember(tournament.yellowCardFine) { mutableStateOf(tournament.yellowCardFine.toInt().toString()) }
    var redFineText by remember(tournament.redCardFine) { mutableStateOf(tournament.redCardFine.toInt().toString()) }
    var mvpPoolToggle by remember(tournament.prizeMvpEnabled) { mutableStateOf(tournament.prizeMvpEnabled) }

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

        // Proportional Hierarchical Auto-balancing Sliders
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Reparto Proporcional de Premios (Total 100%):", fontWeight = FontWeight.Bold, fontSize = 13.sp)

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

        // Card Fines and Sanctions Section (Clean Backspace inputs)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Tarjetas, Multas y Destino:", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = yellowFineText,
                        onValueChange = { input ->
                            val clean = input.filter { it.isDigit() }
                            yellowFineText = clean
                            val yAmount = clean.toDoubleOrNull() ?: 0.0
                            val rAmount = redFineText.toDoubleOrNull() ?: 0.0
                            onUpdateCardFines(yAmount, rAmount, mvpPoolToggle)
                        },
                        label = { Text("Multa Amarilla ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = redFineText,
                        onValueChange = { input ->
                            val clean = input.filter { it.isDigit() }
                            redFineText = clean
                            val yAmount = yellowFineText.toDoubleOrNull() ?: 0.0
                            val rAmount = clean.toDoubleOrNull() ?: 0.0
                            onUpdateCardFines(yAmount, rAmount, mvpPoolToggle)
                        },
                        label = { Text("Multa Roja ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Destino de Multas al Pozo MVP", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Text(
                            text = if (mvpPoolToggle) "Las multas recaudadas suman al premio MVP del torneo" else "Las multas van al pozo general de premios",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = mvpPoolToggle,
                        onCheckedChange = { checked ->
                            mvpPoolToggle = checked
                            val yAmount = yellowFineText.toDoubleOrNull() ?: 0.0
                            val rAmount = redFineText.toDoubleOrNull() ?: 0.0
                            onUpdateCardFines(yAmount, rAmount, checked)
                        }
                    )
                }
            }
        }

        // Player Fee Checklist with instant 0ms visual feedback
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
                                val isPaid = localFeeMap[p.id] ?: p.feePaid
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val nextStatus = !isPaid
                                            localFeeMap[p.id] = nextStatus
                                            onTogglePlayerFee(p.id, isPaid)
                                        }
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("#${p.dorsal} ${p.name}", fontSize = 12.sp)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (isPaid) "Pagado" else "Pendiente",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isPaid) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                                        )
                                        Checkbox(
                                            checked = isPaid,
                                            onCheckedChange = { checked ->
                                                localFeeMap[p.id] = checked
                                                onTogglePlayerFee(p.id, isPaid)
                                            }
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

@Composable
private fun ChampionCelebrationDialog(
    tournament: com.example.data.local.entity.Tournament,
    championTeam: Team,
    runnerUpTeam: Team?,
    onOpenStudio: () -> Unit,
    onChangeChampion: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.thebig_champion_trophy_banner),
                        contentDescription = "Trofeo de Campeón",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }

                Text(
                    text = "¡FELICITACIONES AL EQUIPO CAMPEÓN!",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    color = Color(0xFFF59E0B)
                )

                Text(
                    text = tournament.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(2.dp))

                TeamCrestView(team = championTeam, size = 64.dp)
                Text(
                    text = championTeam.name,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Campeón Oficial TheBiG",
                    fontSize = 12.sp,
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.Bold
                )

                runnerUpTeam?.let { ru ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TeamCrestView(team = ru, size = 26.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Subcampeón", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                            Text(ru.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = onOpenStudio,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                ) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ir al Estudio de Campeón", fontWeight = FontWeight.Black, color = Color.Black)
                }

                OutlinedButton(
                    onClick = onChangeChampion,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Reasignar Campeón", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

@Composable
private fun MatchSquadRosterDialog(
    match: Match,
    homeTeam: Team?,
    awayTeam: Team?,
    homePlayers: List<Player>,
    awayPlayers: List<Player>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(match.roundName, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text("Alineaciones y Plantillas", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Home Team Roster
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TeamCrestView(team = homeTeam, size = 26.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(homeTeam?.name ?: "Local", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        if (homePlayers.isEmpty()) {
                            Text("Sin jugadores registrados", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        } else {
                            homePlayers.forEach { p ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (homeTeam != null) {
                                            TeamJerseyView(team = homeTeam, isGoalkeeper = p.isGoalkeeper, dorsal = p.dorsal, size = 26.dp)
                                        } else {
                                            TeamJerseyView(primaryColorHex = "#3B82F6", secondaryColorHex = "#1E293B", isGoalkeeper = p.isGoalkeeper, dorsal = p.dorsal, size = 26.dp)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("#${p.dorsal} ${p.name}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }
                                    Text(if (p.isGoalkeeper) "ARQUERO" else "CAMPO", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }

                // Away Team Roster
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TeamCrestView(team = awayTeam, size = 26.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(awayTeam?.name ?: "Visitante", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        if (awayPlayers.isEmpty()) {
                            Text("Sin jugadores registrados", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        } else {
                            awayPlayers.forEach { p ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (awayTeam != null) {
                                            TeamJerseyView(team = awayTeam, isGoalkeeper = p.isGoalkeeper, dorsal = p.dorsal, size = 26.dp)
                                        } else {
                                            TeamJerseyView(primaryColorHex = "#EF4444", secondaryColorHex = "#FFFFFF", isGoalkeeper = p.isGoalkeeper, dorsal = p.dorsal, size = 26.dp)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("#${p.dorsal} ${p.name}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }
                                    Text(if (p.isGoalkeeper) "ARQUERO" else "CAMPO", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}
