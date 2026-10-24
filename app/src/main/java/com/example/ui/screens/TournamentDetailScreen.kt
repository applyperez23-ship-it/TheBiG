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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
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
import com.example.data.local.entity.TournamentTeamCrossRef
import com.example.domain.rules.PrizeCalculator
import com.example.domain.rules.StandingsCalculator
import com.example.domain.rules.StatsCalculator
import com.example.domain.rules.TeamStanding
import com.example.ui.viewmodel.TournamentViewModel
import com.example.util.GraphicCardGenerator
import com.example.util.TeamCrestView

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

    val financialSummary = remember(tournament, crossRefs, events) {
        PrizeCalculator.calculate(tournament, crossRefs, events)
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
                    standings = standings,
                    onExportInstagram = {
                        val bmp = GraphicCardGenerator.generateStandingsInstagramBitmap(tournament, standings)
                        GraphicCardGenerator.shareBitmap(context, bmp, "Compartir Tabla 1:1")
                    },
                    onOpenEnroll = { showEnrollDialog = true }
                )
                1 -> FixtureTabContent(
                    matches = matches,
                    enrolledTeams = enrolledTeams,
                    onGenerate = { viewModel.generateFixture(tournamentId) },
                    onMatchClick = onMatchClick,
                    onExportInstagram = {
                        val teamMap = enrolledTeams.associateBy { it.id }
                        val bmp = GraphicCardGenerator.generateFixtureInstagramBitmap(tournament, matches, teamMap)
                        GraphicCardGenerator.shareBitmap(context, bmp, "Compartir Fixture 1:1")
                    }
                )
                2 -> StatisticsTabContent(
                    topScorers = topScorers,
                    topAssisters = topAssisters,
                    goalkeepers = goalkeeperRankings,
                    onExportInstagramScorers = {
                        val bmp = GraphicCardGenerator.generateScorersInstagramBitmap(tournament, topScorers)
                        GraphicCardGenerator.shareBitmap(context, bmp, "Compartir Goleadores 1:1")
                    }
                )
                3 -> FinancialTabContent(
                    summary = financialSummary,
                    crossRefs = crossRefs,
                    teams = enrolledTeams,
                    onToggleFee = { tId, curr -> viewModel.updateTeamFeeStatus(tournamentId, tId, !curr) }
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
    standings: List<TeamStanding>,
    onExportInstagram: () -> Unit,
    onOpenEnroll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onExportInstagram,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("export_standings_instagram_button")
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Exportar 1:1 Instagram Feed", fontSize = 12.sp)
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("#  Equipo", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("PJ", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("PG", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("DG", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("PTS", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
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
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${s.position}.", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                TeamCrestView(
                                    shieldIndex = s.team.shieldVectorIndex,
                                    primaryColorHex = s.team.primaryColorHex,
                                    secondaryColorHex = s.team.secondaryColorHex,
                                    borderColorHex = s.team.borderColorHex,
                                    shortName = s.team.shortName,
                                    customShieldUri = s.team.customShieldUri,
                                    size = 28.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(s.team.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text("${s.pj}", fontSize = 12.sp)
                                Text("${s.pg}", fontSize = 12.sp)
                                Text("${s.dg}", fontSize = 12.sp)
                                Text("${s.pts}", fontWeight = FontWeight.Black, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FixtureTabContent(
    matches: List<Match>,
    enrolledTeams: List<Team>,
    onGenerate: () -> Unit,
    onMatchClick: (Long) -> Unit,
    onExportInstagram: () -> Unit
) {
    val teamMap = remember(enrolledTeams) { enrolledTeams.associateBy { it.id } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onExportInstagram,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                enabled = matches.isNotEmpty()
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Exportar Fixture 1:1", fontSize = 12.sp)
            }

            if (matches.isEmpty() && enrolledTeams.size >= 2) {
                Button(
                    onClick = onGenerate,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Generar Partidos", fontSize = 12.sp)
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
                    text = "Aún no se han generado los partidos. Inscribe equipos y toca 'Generar Partidos'.",
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
                items(matches, key = { it.id }) { m ->
                    val home = teamMap[m.homeTeamId]
                    val away = teamMap[m.awayTeamId]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onMatchClick(m.id) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(m.roundName.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Text("${home?.name ?: "Local"} vs ${away?.name ?: "Visitante"}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Text(
                                text = if (m.status == "FINISHED") "${m.homeScore} - ${m.awayScore}" else "JUGAR",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = if (m.status == "FINISHED") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
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
    onExportInstagramScorers: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Tabla de Goleadores", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Button(
                onClick = onExportInstagramScorers,
                shape = RoundedCornerShape(8.dp),
                enabled = topScorers.isNotEmpty()
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Exportar Goleadores 1:1", fontSize = 11.sp)
            }
        }

        if (topScorers.isEmpty()) {
            Text("No se han registrado goles aún.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            topScorers.take(5).forEachIndexed { idx, sc ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${idx + 1}. ${sc.player.name} (${sc.team.name})", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("${sc.goals} Goles", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text("Máximos Asistidores", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        if (topAssisters.isEmpty()) {
            Text("No se han registrado asistencias aún.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            topAssisters.take(5).forEachIndexed { idx, ast ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${idx + 1}. ${ast.player.name} (${ast.team.name})", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("${ast.assists} Asist.", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun FinancialTabContent(
    summary: com.example.domain.rules.TournamentFinancialSummary,
    crossRefs: List<TournamentTeamCrossRef>,
    teams: List<Team>,
    onToggleFee: (teamId: Long, current: Boolean) -> Unit
) {
    val teamMap = remember(teams) { teams.associateBy { it.id } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Calculadora Financiera Inteligente", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Fondo Total Estimado:", style = MaterialTheme.typography.bodyMedium)
                    Text("$${summary.estimatedRevenue.toInt()}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Desglose por Jugador:", style = MaterialTheme.typography.bodyMedium)
                    Text("$${summary.feePerPlayer.toInt()}/jugador", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Recaudado Real:", style = MaterialTheme.typography.bodyMedium)
                    Text("$${summary.actualRevenue.toInt()}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Pozo de Sanciones (Amarillas + Rojas):", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text("$${summary.totalSanctionsPool.toInt()}", fontWeight = FontWeight.Black, color = Color(0xFFF59E0B))
                }
                Text(
                    text = "El pozo acumulativo por sanciones financia automáticamente el Premio Económico al MVP del torneo.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text("Premios Asignados", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        summary.prizes.forEach { p ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(p.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(p.winnerNote, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("$${p.amount.toInt()}", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text("Control de Cuotas por Club", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        crossRefs.forEach { cr ->
            val tm = teamMap[cr.teamId] ?: return@forEach
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(tm.name, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (cr.feePaid) "Pagado" else "Pendiente", fontSize = 12.sp, color = if (cr.feePaid) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error)
                        Checkbox(checked = cr.feePaid, onCheckedChange = { onToggleFee(tm.id, cr.feePaid) })
                    }
                }
            }
        }
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
        title = { Text("Inscribir Equipos al Torneo", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Equipos Disponibles:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                if (availableTeams.isEmpty()) {
                    Text("No hay equipos adicionales disponibles.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    availableTeams.forEach { tm ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEnroll(tm.id) }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(tm.name, fontSize = 13.sp)
                            Button(onClick = { onEnroll(tm.id) }, shape = RoundedCornerShape(6.dp)) {
                                Text("Inscribir", fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Equipos Inscritos (${enrolledTeams.size}):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                enrolledTeams.forEach { tm ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(tm.name, fontSize = 13.sp)
                        IconButton(onClick = { onRemove(tm.id) }) {
                            Icon(Icons.Default.Close, contentDescription = "Desinscribir", tint = MaterialTheme.colorScheme.error)
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

@Composable
private fun ChampionSelectDialog(
    teams: List<Team>,
    currentChampId: Long?,
    onDismiss: () -> Unit,
    onSelect: (champId: Long, runnerUpId: Long?) -> Unit
) {
    var selectedChampId by remember { mutableStateOf<Long?>(currentChampId ?: teams.firstOrNull()?.id) }
    var selectedRunnerUpId by remember { mutableStateOf<Long?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Proclamar Equipo Campeón", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Selecciona el equipo Campeón:", fontWeight = FontWeight.SemiBold)
                teams.forEach { tm ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedChampId = tm.id }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = selectedChampId == tm.id,
                            onClick = { selectedChampId = tm.id }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tm.name)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedChampId != null) {
                        onSelect(selectedChampId!!, selectedRunnerUpId)
                    }
                },
                enabled = selectedChampId != null
            ) {
                Text("Proclamar Campeón")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
