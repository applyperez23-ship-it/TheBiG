package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import com.example.data.local.entity.Tournament
import com.example.ui.components.DeleteConfirmationDialog
import com.example.ui.components.KickoffAnimationOverlay
import com.example.ui.viewmodel.MatchViewModel
import com.example.util.TeamCrestView
import com.example.util.TeamJerseyView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchLiveSheetScreen(
    matchId: Long,
    viewModel: MatchViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val app = TheBiGApp.instance

    var match by remember { mutableStateOf<Match?>(null) }
    var homeTeam by remember { mutableStateOf<Team?>(null) }
    var awayTeam by remember { mutableStateOf<Team?>(null) }
    var tournament by remember { mutableStateOf<Tournament?>(null) }
    var homePlayers by remember { mutableStateOf<List<Player>>(emptyList()) }
    var awayPlayers by remember { mutableStateOf<List<Player>>(emptyList()) }
    var events by remember { mutableStateOf<List<MatchEvent>>(emptyList()) }

    val liveTimerState by viewModel.timerState.collectAsStateWithLifecycle()

    // Dialog states
    var showGoalDialog by remember { mutableStateOf(false) }
    var goalTargetTeam by remember { mutableStateOf<Team?>(null) }
    var showPenaltyIncidentDialog by remember { mutableStateOf(false) }
    var showCardDialog by remember { mutableStateOf(false) }
    var showSubDialog by remember { mutableStateOf(false) }
    var showShootoutDialog by remember { mutableStateOf(false) }
    var showMvpDialog by remember { mutableStateOf(false) }
    var showReopenDialog by remember { mutableStateOf(false) }
    var showKickoffAnimation by remember { mutableStateOf(false) }
    var showForfeitDialog by remember { mutableStateOf(false) }
    var eventToDelete by remember { mutableStateOf<MatchEvent?>(null) }

    LaunchedEffect(matchId) {
        app.repository.getMatch(matchId).collect { m ->
            match = m
            m?.let {
                homeTeam = app.repository.getTeamOnce(it.homeTeamId)
                awayTeam = app.repository.getTeamOnce(it.awayTeamId)
                tournament = app.repository.getTournamentOnce(it.tournamentId)
                homePlayers = app.repository.getPlayersForTeamOnce(it.homeTeamId)
                awayPlayers = app.repository.getPlayersForTeamOnce(it.awayTeamId)
            }
        }
    }

    LaunchedEffect(matchId) {
        app.repository.getEventsForMatch(matchId).collect { events = it }
    }

    val currentMatch = match
    if (currentMatch == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Cargando partido...")
        }
        return
    }

    val matchDuration = tournament?.matchDurationMinutes ?: 20

    // Formatted referee clock display
    val clockString = if (liveTimerState.matchId == matchId && currentMatch.status == "LIVE") {
        liveTimerState.fullClockDisplay
    } else {
        val elapsed = currentMatch.elapsedSeconds
        val elapsedStr = "%02d:%02d".format(elapsed / 60, elapsed % 60)
        val remaining = ((matchDuration * 60) - elapsed).coerceAtLeast(0)
        val remainingStr = "%02d:%02d".format(remaining / 60, remaining % 60)
        "$elapsedStr / %02d:00 - Restan $remainingStr".format(matchDuration)
    }

    val currentPeriod = if (liveTimerState.matchId == matchId && currentMatch.status == "LIVE") {
        liveTimerState.currentPeriod
    } else {
        val halfTime = (matchDuration * 60) / 2
        when {
            currentMatch.elapsedSeconds >= matchDuration * 60 -> "Finalizado"
            currentMatch.elapsedSeconds > halfTime -> "2do Tiempo"
            currentMatch.elapsedSeconds == halfTime -> "Descanso"
            else -> "1er Tiempo"
        }
    }

    val currentMinute = if (liveTimerState.matchId == matchId && currentMatch.status == "LIVE") {
        (liveTimerState.elapsedSeconds / 60).coerceAtLeast(1)
    } else {
        (currentMatch.elapsedSeconds / 60).coerceAtLeast(1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Acta del Partido",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "${tournament?.name ?: "TheBiG"} • ${currentMatch.roundName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val summary = buildString {
                                appendLine("Acta Oficial TheBiG")
                                appendLine("${tournament?.name} • ${currentMatch.roundName}")
                                appendLine("${homeTeam?.name} ${currentMatch.homeScore} - ${currentMatch.awayScore} ${awayTeam?.name}")
                                if (currentMatch.homePenalties != null) {
                                    appendLine("Penaltis: ${currentMatch.homePenalties} - ${currentMatch.awayPenalties}")
                                }
                                appendLine("Incidencias:")
                                events.forEach { ev ->
                                    val tName = if (ev.teamId == homeTeam?.id) homeTeam?.name else awayTeam?.name
                                    val pName = (homePlayers + awayPlayers).firstOrNull { it.id == ev.playerId }?.name ?: ""
                                    appendLine("- ${ev.minute}' ${ev.eventType}: $pName ($tName)")
                                }
                                appendLine("TheBiG • Acta Oficial")
                            }
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Acta TheBiG: ${homeTeam?.name} vs ${awayTeam?.name}")
                                putExtra(Intent.EXTRA_TEXT, summary)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Compartir Acta"))
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Compartir Acta")
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Live Referee Clock Card with Period Tag
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Period badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = currentPeriod.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassBottom,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = clockString,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Timer Controls
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentMatch.status == "SCHEDULED") {
                            Button(
                                onClick = {
                                    val prefs = context.getSharedPreferences("thebig_prefs", android.content.Context.MODE_PRIVATE)
                                    val isPowerSaving = prefs.getBoolean("power_saving_mode", false)
                                    if (isPowerSaving) {
                                        if (homeTeam != null && awayTeam != null) {
                                            viewModel.startLiveMatch(context, currentMatch, homeTeam!!, awayTeam!!, matchDuration)
                                        }
                                    } else {
                                        showKickoffAnimation = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                modifier = Modifier.testTag("start_match_button")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Iniciar Partido")
                            }

                            OutlinedButton(
                                onClick = { showForfeitDialog = true },
                                modifier = Modifier.testTag("forfeit_match_button")
                            ) {
                                Text("Forfait (3-0)", fontSize = 11.sp)
                            }
                        } else if (currentMatch.status == "LIVE") {
                            if (currentPeriod == "Descanso") {
                                Button(
                                    onClick = { viewModel.startSecondHalf(context) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Iniciar 2do Tiempo")
                                }
                            } else if (liveTimerState.isRunning) {
                                OutlinedButton(
                                    onClick = { viewModel.pauseLiveMatch(context) },
                                    modifier = Modifier.testTag("pause_match_button")
                                ) {
                                    Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pausar")
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.resumeLiveMatch(context) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    modifier = Modifier.testTag("resume_match_button")
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reanudar")
                                }
                            }

                            Button(
                                onClick = {
                                    viewModel.finishMatch(
                                        context,
                                        matchId,
                                        if (liveTimerState.matchId == matchId) liveTimerState.elapsedSeconds else currentMatch.elapsedSeconds
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.testTag("finish_match_button")
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Finalizar")
                            }
                        } else {
                            // Finished state
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "ACTA CERRADA • PARTIDO CONCLUIDO",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedButton(
                                    onClick = { showReopenDialog = true },
                                    modifier = Modifier.testTag("reopen_match_button")
                                ) {
                                    Text("Reabrir Partido")
                                }
                            }
                        }
                    }
                }
            }

            // Scoreboard Card with High-Contrast Active vs Inactive Team Switcher
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (currentMatch.status != "FINISHED") {
                        Text(
                            text = "Toca el escudo de un equipo para registrar gol directo:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Home Team Interactive Column
                        val isHomeActive = goalTargetTeam?.id == homeTeam?.id
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = currentMatch.status != "FINISHED") {
                                    goalTargetTeam = homeTeam
                                    showGoalDialog = true
                                }
                                .testTag("scoreboard_home_team"),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isHomeActive) 2.dp else 1.dp,
                                color = if (isHomeActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isHomeActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            ) {
                                TeamCrestView(team = homeTeam, size = 48.dp)
                                Spacer(modifier = Modifier.height(4.dp))
                                homeTeam?.let { tm ->
                                    TeamJerseyView(team = tm, size = 28.dp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = homeTeam?.name ?: "Local",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                                if (currentMatch.status != "FINISHED") {
                                    Text(
                                        text = "+ Gol",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // Score in Middle
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.surface,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "${currentMatch.homeScore} - ${currentMatch.awayScore}",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (currentMatch.homePenalties != null && currentMatch.awayPenalties != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Penaltis: ${currentMatch.homePenalties} - ${currentMatch.awayPenalties}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Away Team Interactive Column
                        val isAwayActive = goalTargetTeam?.id == awayTeam?.id
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = currentMatch.status != "FINISHED") {
                                    goalTargetTeam = awayTeam
                                    showGoalDialog = true
                                }
                                .testTag("scoreboard_away_team"),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isAwayActive) 2.dp else 1.dp,
                                color = if (isAwayActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isAwayActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            ) {
                                TeamCrestView(team = awayTeam, size = 48.dp)
                                Spacer(modifier = Modifier.height(4.dp))
                                awayTeam?.let { tm ->
                                    TeamJerseyView(team = tm, size = 28.dp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = awayTeam?.name ?: "Visitante",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                                if (currentMatch.status != "FINISHED") {
                                    Text(
                                        text = "+ Gol",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Referee Actions: Modern Icon Action Bar
            Text(
                text = "Acciones e Incidencias Oficiales",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )

            if (currentMatch.status != "FINISHED") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ActionIconButton(
                            icon = Icons.Default.SportsSoccer,
                            label = "Gol",
                            tint = MaterialTheme.colorScheme.primary,
                            onClick = {
                                goalTargetTeam = null
                                showGoalDialog = true
                            },
                            testTag = "action_add_goal"
                        )

                        ActionIconButton(
                            icon = Icons.Default.Warning,
                            label = "Penal",
                            tint = Color(0xFFEF4444),
                            onClick = { showPenaltyIncidentDialog = true },
                            testTag = "action_add_penalty"
                        )

                        ActionCardIconButton(
                            label = "Tarjeta",
                            onClick = { showCardDialog = true },
                            testTag = "action_add_card"
                        )

                        ActionIconButton(
                            icon = Icons.Default.SwapHoriz,
                            label = "Cambio",
                            tint = MaterialTheme.colorScheme.secondary,
                            onClick = { showSubDialog = true },
                            testTag = "action_add_sub"
                        )

                        ActionIconButton(
                            icon = Icons.Default.FormatListNumbered,
                            label = "Penaltis",
                            tint = Color(0xFF8B5CF6),
                            onClick = { showShootoutDialog = true },
                            testTag = "action_add_shootout"
                        )

                        ActionIconButton(
                            icon = Icons.Default.Star,
                            label = "MVP",
                            tint = Color(0xFFF59E0B),
                            onClick = { showMvpDialog = true },
                            testTag = "action_select_mvp"
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Acta cerrada. El marcador y las incidencias están protegidos contra modificaciones.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Timeline / Chronology of Events
            Text(
                text = "Cronología de Incidencias (${events.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )

            if (events.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "Aún no se han registrado incidencias en este partido.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                events.forEach { event ->
                    val team = if (event.teamId == homeTeam?.id) homeTeam else awayTeam
                    val player = (homePlayers + awayPlayers).firstOrNull { it.id == event.playerId }
                    val assistPlayer = (homePlayers + awayPlayers).firstOrNull { it.id == event.assistPlayerId }

                    IncidentTimelineItem(
                        event = event,
                        team = team,
                        player = player,
                        assistPlayer = assistPlayer,
                        canDelete = currentMatch.status != "FINISHED",
                        onDelete = { eventToDelete = event }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Team Rosters with Captain Highlighting
            Text(
                text = "Alineaciones y Capitanes",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )

            RosterPreviewCard(
                team = homeTeam,
                players = homePlayers,
                title = "Local: ${homeTeam?.name ?: ""}"
            )

            RosterPreviewCard(
                team = awayTeam,
                players = awayPlayers,
                title = "Visitante: ${awayTeam?.name ?: ""}"
            )
        }
    }

    // Defensive Deletion Dialog for Event
    eventToDelete?.let { ev ->
        DeleteConfirmationDialog(
            itemName = "la incidencia (${ev.eventType} en el minuto ${ev.minute}')",
            onDismiss = { eventToDelete = null },
            onConfirm = {
                viewModel.deleteEvent(ev)
                eventToDelete = null
            }
        )
    }

    // Reopen Concluded Match Confirmation Dialog
    if (showReopenDialog && homeTeam != null && awayTeam != null) {
        AlertDialog(
            onDismissRequest = { showReopenDialog = false },
            title = { Text("Reabrir Partido Concluido", fontWeight = FontWeight.Bold) },
            text = {
                Text("¿Estás seguro de reabrir el acta oficial de este partido? El cronómetro, marcador y registro de incidencias volverán a habilitarse para edición.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.startLiveMatch(context, currentMatch, homeTeam!!, awayTeam!!, matchDuration)
                        showReopenDialog = false
                    }
                ) {
                    Text("Confirmar Reapertura")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showReopenDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Dialog 1: Add Goal with 2-step Modal (Step 1: Scorer, Step 2: Assist)
    if (showGoalDialog && homeTeam != null && awayTeam != null) {
        TwoStepGoalIncidentDialog(
            homeTeam = homeTeam!!,
            awayTeam = awayTeam!!,
            initialTeam = goalTargetTeam,
            homePlayers = homePlayers,
            awayPlayers = awayPlayers,
            defaultMinute = currentMinute,
            onDismiss = {
                showGoalDialog = false
                goalTargetTeam = null
            },
            onConfirm = { teamId, authorId, assistId, isOwnGoal, isPen, minute ->
                viewModel.addGoal(
                    matchId = matchId,
                    teamId = teamId,
                    playerId = authorId,
                    assistPlayerId = assistId,
                    isOwnGoal = isOwnGoal,
                    isPenalty = isPen,
                    minute = minute
                )
                showGoalDialog = false
                goalTargetTeam = null
            }
        )
    }

    // Dialog 2: Penalty in Regular Time Dialog
    if (showPenaltyIncidentDialog && homeTeam != null && awayTeam != null) {
        PenaltyIncidentDialog(
            homeTeam = homeTeam!!,
            awayTeam = awayTeam!!,
            homePlayers = homePlayers,
            awayPlayers = awayPlayers,
            defaultMinute = currentMinute,
            onDismiss = { showPenaltyIncidentDialog = false },
            onConfirm = { teamId, playerId, isScored, minute ->
                if (isScored) {
                    viewModel.addGoal(
                        matchId = matchId,
                        teamId = teamId,
                        playerId = playerId,
                        assistPlayerId = null,
                        isOwnGoal = false,
                        isPenalty = true,
                        minute = minute
                    )
                } else {
                    viewModel.addPenaltyMissed(
                        matchId = matchId,
                        teamId = teamId,
                        playerId = playerId,
                        minute = minute
                    )
                }
                showPenaltyIncidentDialog = false
            }
        )
    }

    // Dialog 3: Card Dialog
    if (showCardDialog && homeTeam != null && awayTeam != null) {
        CardIncidentDialog(
            homeTeam = homeTeam!!,
            awayTeam = awayTeam!!,
            homePlayers = homePlayers,
            awayPlayers = awayPlayers,
            defaultMinute = currentMinute,
            onDismiss = { showCardDialog = false },
            onConfirm = { teamId, playerId, isRed, minute ->
                viewModel.addCard(
                    matchId = matchId,
                    teamId = teamId,
                    playerId = playerId,
                    isRed = isRed,
                    minute = minute
                )
                showCardDialog = false
            }
        )
    }

    // Dialog 4: Substitution Dialog
    if (showSubDialog && homeTeam != null && awayTeam != null) {
        SubstitutionDialog(
            homeTeam = homeTeam!!,
            awayTeam = awayTeam!!,
            homePlayers = homePlayers,
            awayPlayers = awayPlayers,
            defaultMinute = currentMinute,
            onDismiss = { showSubDialog = false },
            onConfirm = { teamId, outId, inId, minute ->
                viewModel.addSubstitution(
                    matchId = matchId,
                    teamId = teamId,
                    playerOutId = outId,
                    playerInId = inId,
                    minute = minute
                )
                showSubDialog = false
            }
        )
    }

    // Dialog 5: Shootout Score Dialog
    if (showShootoutDialog) {
        ShootoutScoreDialog(
            homeTeam = homeTeam,
            awayTeam = awayTeam,
            initialHome = currentMatch.homePenalties ?: 0,
            initialAway = currentMatch.awayPenalties ?: 0,
            onDismiss = { showShootoutDialog = false },
            onConfirm = { hPen, aPen ->
                viewModel.recordPenaltyShootoutScore(matchId, hPen, aPen)
                showShootoutDialog = false
            }
        )
    }

    // Dialog 6: MVP Selection Dialog
    if (showMvpDialog && homeTeam != null && awayTeam != null) {
        MvpSelectionDialog(
            homeTeam = homeTeam!!,
            awayTeam = awayTeam!!,
            homePlayers = homePlayers,
            awayPlayers = awayPlayers,
            onDismiss = { showMvpDialog = false },
            onConfirm = { teamId, playerId ->
                viewModel.setMatchMvp(matchId, teamId, playerId)
                showMvpDialog = false
            }
        )
    }

    // Kickoff Parabolic Animation Overlay (<1.5s)
    if (showKickoffAnimation) {
        KickoffAnimationOverlay(
            onComplete = {
                showKickoffAnimation = false
                if (homeTeam != null && awayTeam != null) {
                    viewModel.startLiveMatch(context, currentMatch, homeTeam!!, awayTeam!!, matchDuration)
                }
            }
        )
    }

    // Forfeit 3-0 Dialog
    if (showForfeitDialog && homeTeam != null && awayTeam != null) {
        AlertDialog(
            onDismissRequest = { showForfeitDialog = false },
            title = { Text("Declarar Victoria por Forfait (3-0)", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Text(
                    text = "Selecciona el equipo ganador. El partido se cerrará inmediatamente con marcador oficial 3-0.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            viewModel.declareForfeit(context, matchId, homeTeam!!.id)
                            showForfeitDialog = false
                        }
                    ) {
                        Text("Gana ${homeTeam!!.name}", fontSize = 11.sp)
                    }
                    Button(
                        onClick = {
                            viewModel.declareForfeit(context, matchId, awayTeam!!.id)
                            showForfeitDialog = false
                        }
                    ) {
                        Text("Gana ${awayTeam!!.name}", fontSize = 11.sp)
                    }
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showForfeitDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun ActionIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(6.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(tint.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ActionCardIconButton(
    label: String,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(6.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFFF59E0B).copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp, 12.dp).background(Color(0xFFF59E0B), RoundedCornerShape(1.dp)))
                Box(modifier = Modifier.size(8.dp, 12.dp).background(Color(0xFFEF4444), RoundedCornerShape(1.dp)))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun IncidentTimelineItem(
    event: MatchEvent,
    team: Team?,
    player: Player?,
    assistPlayer: Player?,
    canDelete: Boolean = true,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${event.minute}'",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        when (event.eventType) {
                            "GOAL" -> {
                                Icon(Icons.Default.SportsSoccer, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Gol", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            "PENALTY_GOAL" -> {
                                Icon(Icons.Default.SportsSoccer, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Gol de Penal", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            "OWN_GOAL" -> {
                                Text("Autogol", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                            }
                            "PENALTY_MISSED" -> {
                                Text("Penal Atajado/Fallado", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                            }
                            "YELLOW_CARD" -> {
                                Box(modifier = Modifier.size(9.dp, 13.dp).background(Color(0xFFF59E0B), RoundedCornerShape(2.dp)))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Tarjeta Amarilla", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            "RED_CARD" -> {
                                Box(modifier = Modifier.size(9.dp, 13.dp).background(Color(0xFFEF4444), RoundedCornerShape(2.dp)))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Tarjeta Roja", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFEF4444))
                            }
                            "SUBSTITUTION" -> {
                                Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cambio", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            "MVP" -> {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Jugador MVP", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFF59E0B))
                            }
                        }
                    }

                    val playerText = buildString {
                        if (player != null) {
                            append(player.name)
                            if (player.dorsal > 0) append(" (#${player.dorsal})")
                            if (player.isCaptain) append(" (C)")
                        }
                        if (assistPlayer != null) {
                            append(" • Asist: ${assistPlayer.name}")
                        }
                        if (team != null) {
                            append(" • ${team.name}")
                        }
                    }
                    Text(text = playerText, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (canDelete) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar incidencia",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RosterPreviewCard(
    team: Team?,
    players: List<Player>,
    title: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(6.dp))
            if (players.isEmpty()) {
                Text(
                    text = "Sin jugadores registrados en el plantel",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                players.forEach { p ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "#${p.dorsal}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(30.dp)
                        )
                        Text(text = p.name, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        if (p.isCaptain) {
                            Surface(
                                color = Color(0xFFF59E0B),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                Text(
                                    text = "CAPITÁN (C)",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (p.isGoalkeeper) {
                            Surface(
                                color = Color(0xFF10B981),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "PORTERO",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
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

/**
 * 2-Step Ultra-Compact Goal Dialog:
 * Completely fits on screen without vertical scroll.
 * 2-column clean grid with mini jerseys (TeamJerseyView) and prominent player names.
 * Step 1: Tap scorer -> advances to Step 2
 * Step 2: Tap "Sin Asistencia" or tap assister -> immediately registers goal and closes!
 */
@Composable
fun TwoStepGoalIncidentDialog(
    homeTeam: Team,
    awayTeam: Team,
    initialTeam: Team? = null,
    homePlayers: List<Player>,
    awayPlayers: List<Player>,
    defaultMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (teamId: Long, authorId: Long, assistId: Long?, isOwnGoal: Boolean, isPen: Boolean, minute: Int) -> Unit
) {
    var step by remember { mutableIntStateOf(1) } // 1: Scorer, 2: Assist
    var selectedTeamId by remember(initialTeam) { mutableLongStateOf(initialTeam?.id ?: homeTeam.id) }
    var selectedAuthorId by remember { mutableStateOf<Long?>(null) }
    var isOwnGoal by remember { mutableStateOf(false) }
    var isPenalty by remember { mutableStateOf(false) }
    var minuteText by remember { mutableStateOf(defaultMinute.toString()) }

    val currentTeam = if (selectedTeamId == homeTeam.id) homeTeam else awayTeam
    val currentRoster = if (selectedTeamId == homeTeam.id) homePlayers else awayPlayers

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 580.dp)
                .padding(6.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // Header: Crest, Team Name, and Step Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TeamCrestView(team = currentTeam, size = 32.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (step == 1) "Anotador: ${currentTeam.name}" else "Asistencia: ${currentTeam.name}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (step == 1) "Paso 1 de 2 • Toca el autor del gol" else "Paso 2 de 2 • Toca asistidor o 'Sin Asistencia'",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                // Compact Bar: Switch Team, Minute, Penalty/Autogol
                if (step == 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                selectedTeamId = if (selectedTeamId == homeTeam.id) awayTeam.id else homeTeam.id
                                selectedAuthorId = null
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Cambiar a ${if (selectedTeamId == homeTeam.id) awayTeam.shortName else homeTeam.shortName}", fontSize = 11.sp)
                        }

                        FilterChip(
                            selected = isPenalty,
                            onClick = {
                                isPenalty = !isPenalty
                                if (isPenalty) isOwnGoal = false
                            },
                            label = { Text("Penal", fontSize = 10.sp) }
                        )

                        FilterChip(
                            selected = isOwnGoal,
                            onClick = {
                                isOwnGoal = !isOwnGoal
                                if (isOwnGoal) isPenalty = false
                            },
                            label = { Text("Autogol", fontSize = 10.sp) }
                        )

                        OutlinedTextField(
                            value = minuteText,
                            onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) minuteText = it },
                            label = { Text("Min", fontSize = 9.sp) },
                            singleLine = true,
                            modifier = Modifier.width(54.dp)
                        )
                    }
                } else {
                    // Step 2 Prominent "Sin Asistencia" Button
                    Button(
                        onClick = {
                            val min = minuteText.toIntOrNull() ?: defaultMinute
                            onConfirm(selectedTeamId, selectedAuthorId!!, null, isOwnGoal, isPenalty, min)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.SportsSoccer, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gol Directo (Sin Asistencia)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                // 2-Column Clean Grid of Players with Mini Jerseys
                val candidatePlayers = if (step == 1) currentRoster else currentRoster.filter { it.id != selectedAuthorId }

                if (candidatePlayers.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Sin jugadores en plantilla", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        items(candidatePlayers) { player ->
                            val isChosen = if (step == 1) selectedAuthorId == player.id else false
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (step == 1) {
                                            selectedAuthorId = player.id
                                            if (isOwnGoal) {
                                                val min = minuteText.toIntOrNull() ?: defaultMinute
                                                onConfirm(selectedTeamId, player.id, null, true, false, min)
                                            } else {
                                                step = 2
                                            }
                                        } else {
                                            val min = minuteText.toIntOrNull() ?: defaultMinute
                                            onConfirm(selectedTeamId, selectedAuthorId!!, player.id, isOwnGoal, isPenalty, min)
                                        }
                                    },
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isChosen) 2.dp else 1.dp,
                                    color = if (isChosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isChosen) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TeamJerseyView(
                                        team = currentTeam,
                                        isGoalkeeper = player.isGoalkeeper,
                                        dorsal = player.dorsal,
                                        size = 28.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = player.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "#${player.dorsal}${if (player.isCaptain) " (C)" else ""}",
                                            fontSize = 10.sp,
                                            color = if (player.isCaptain) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = if (player.isCaptain) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Footer button (only needed in Step 2 to go back)
                if (step == 2) {
                    OutlinedButton(
                        onClick = { step = 1 },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cambiar Anotador", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun PenaltyIncidentDialog(
    homeTeam: Team,
    awayTeam: Team,
    homePlayers: List<Player>,
    awayPlayers: List<Player>,
    defaultMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (teamId: Long, playerId: Long, isScored: Boolean, minute: Int) -> Unit
) {
    var selectedTeamId by remember { mutableLongStateOf(homeTeam.id) }
    var selectedPlayerId by remember { mutableStateOf<Long?>(null) }
    var isScored by remember { mutableStateOf(true) }
    var minuteText by remember { mutableStateOf(defaultMinute.toString()) }

    val currentRoster = if (selectedTeamId == homeTeam.id) homePlayers else awayPlayers

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Penal / Falta en el Área", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Equipo ejecutante:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { selectedTeamId = homeTeam.id; selectedPlayerId = null },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTeamId == homeTeam.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(homeTeam.name, maxLines = 1, fontSize = 12.sp)
                    }
                    Button(
                        onClick = { selectedTeamId = awayTeam.id; selectedPlayerId = null },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTeamId == awayTeam.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(awayTeam.name, maxLines = 1, fontSize = 12.sp)
                    }
                }

                Text("Resultado del Penal:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { isScored = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isScored) Color(0xFF10B981) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Gol Convertido", fontSize = 12.sp)
                    }
                    Button(
                        onClick = { isScored = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isScored) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Fallado / Atajado", fontSize = 12.sp)
                    }
                }

                OutlinedTextField(
                    value = minuteText,
                    onValueChange = { minuteText = it },
                    label = { Text("Minuto") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Ejecutante (Obligatorio de la Plantilla):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (selectedPlayerId == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    currentRoster.forEach { player ->
                        val isSelected = selectedPlayerId == player.id
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPlayerId = player.id },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = isSelected, onClick = { selectedPlayerId = player.id })
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "#${pDorsal(player.dorsal)} ${player.name}${if (player.isCaptain) " (C)" else ""}",
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val minute = minuteText.toIntOrNull() ?: defaultMinute
                    onConfirm(selectedTeamId, selectedPlayerId!!, isScored, minute)
                },
                enabled = selectedPlayerId != null
            ) {
                Text("Registrar Penal")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun CardIncidentDialog(
    homeTeam: Team,
    awayTeam: Team,
    homePlayers: List<Player>,
    awayPlayers: List<Player>,
    defaultMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (teamId: Long, playerId: Long, isRed: Boolean, minute: Int) -> Unit
) {
    var selectedTeamId by remember { mutableLongStateOf(homeTeam.id) }
    var selectedPlayerId by remember { mutableStateOf<Long?>(null) }
    var isRed by remember { mutableStateOf(false) }
    var minuteText by remember { mutableStateOf(defaultMinute.toString()) }

    val currentRoster = if (selectedTeamId == homeTeam.id) homePlayers else awayPlayers

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Amonestación o Expulsión", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Equipo sancionado:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { selectedTeamId = homeTeam.id; selectedPlayerId = null },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTeamId == homeTeam.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(homeTeam.name, maxLines = 1, fontSize = 12.sp)
                    }
                    Button(
                        onClick = { selectedTeamId = awayTeam.id; selectedPlayerId = null },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTeamId == awayTeam.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(awayTeam.name, maxLines = 1, fontSize = 12.sp)
                    }
                }

                Text("Tipo de Tarjeta:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { isRed = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isRed) Color(0xFFF59E0B) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Amarilla", fontSize = 12.sp)
                    }
                    Button(
                        onClick = { isRed = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRed) Color(0xFFEF4444) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Roja Directa", fontSize = 12.sp)
                    }
                }

                OutlinedTextField(
                    value = minuteText,
                    onValueChange = { minuteText = it },
                    label = { Text("Minuto") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Jugador sancionado (Obligatorio):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (selectedPlayerId == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    currentRoster.forEach { player ->
                        val isSelected = selectedPlayerId == player.id
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPlayerId = player.id },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = isSelected, onClick = { selectedPlayerId = player.id })
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "#${pDorsal(player.dorsal)} ${player.name}${if (player.isCaptain) " (C)" else ""}",
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val minute = minuteText.toIntOrNull() ?: defaultMinute
                    onConfirm(selectedTeamId, selectedPlayerId!!, isRed, minute)
                },
                enabled = selectedPlayerId != null
            ) {
                Text("Guardar Tarjeta")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun SubstitutionDialog(
    homeTeam: Team,
    awayTeam: Team,
    homePlayers: List<Player>,
    awayPlayers: List<Player>,
    defaultMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (teamId: Long, outId: Long, inId: Long, minute: Int) -> Unit
) {
    var selectedTeamId by remember { mutableLongStateOf(homeTeam.id) }
    var playerOutId by remember { mutableStateOf<Long?>(null) }
    var playerInId by remember { mutableStateOf<Long?>(null) }
    var minuteText by remember { mutableStateOf(defaultMinute.toString()) }

    val currentRoster = if (selectedTeamId == homeTeam.id) homePlayers else awayPlayers

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sustitución Oficial", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Equipo:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { selectedTeamId = homeTeam.id; playerOutId = null; playerInId = null },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTeamId == homeTeam.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(homeTeam.name, maxLines = 1, fontSize = 12.sp)
                    }
                    Button(
                        onClick = { selectedTeamId = awayTeam.id; playerOutId = null; playerInId = null },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTeamId == awayTeam.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(awayTeam.name, maxLines = 1, fontSize = 12.sp)
                    }
                }

                OutlinedTextField(
                    value = minuteText,
                    onValueChange = { minuteText = it },
                    label = { Text("Minuto") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Sale del campo:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .padding(4.dp)
                ) {
                    currentRoster.forEach { p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { playerOutId = p.id }
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = playerOutId == p.id, onClick = { playerOutId = p.id })
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("#${pDorsal(p.dorsal)} ${p.name}", fontSize = 12.sp)
                        }
                    }
                }

                Text("Entra al campo:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .padding(4.dp)
                ) {
                    currentRoster.filter { it.id != playerOutId }.forEach { p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { playerInId = p.id }
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = playerInId == p.id, onClick = { playerInId = p.id })
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("#${pDorsal(p.dorsal)} ${p.name}", fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val minute = minuteText.toIntOrNull() ?: defaultMinute
                    onConfirm(selectedTeamId, playerOutId!!, playerInId!!, minute)
                },
                enabled = playerOutId != null && playerInId != null
            ) {
                Text("Guardar Cambio")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun ShootoutScoreDialog(
    homeTeam: Team?,
    awayTeam: Team?,
    initialHome: Int,
    initialAway: Int,
    onDismiss: () -> Unit,
    onConfirm: (homePenalties: Int, awayPenalties: Int) -> Unit
) {
    var homeStr by remember { mutableStateOf(initialHome.toString()) }
    var awayStr by remember { mutableStateOf(initialAway.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tanda de Penaltis", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Registra la definición por penales:")
                OutlinedTextField(
                    value = homeStr,
                    onValueChange = { homeStr = it },
                    label = { Text("Penales ${homeTeam?.name ?: "Local"}") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = awayStr,
                    onValueChange = { awayStr = it },
                    label = { Text("Penales ${awayTeam?.name ?: "Visitante"}") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val h = homeStr.toIntOrNull() ?: 0
                    val a = awayStr.toIntOrNull() ?: 0
                    onConfirm(h, a)
                }
            ) {
                Text("Guardar Penales")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun MvpSelectionDialog(
    homeTeam: Team,
    awayTeam: Team,
    homePlayers: List<Player>,
    awayPlayers: List<Player>,
    onDismiss: () -> Unit,
    onConfirm: (teamId: Long, playerId: Long) -> Unit
) {
    var selectedTeamId by remember { mutableLongStateOf(homeTeam.id) }
    var selectedPlayerId by remember { mutableStateOf<Long?>(null) }
    val currentRoster = if (selectedTeamId == homeTeam.id) homePlayers else awayPlayers

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Elegir Jugador MVP", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { selectedTeamId = homeTeam.id; selectedPlayerId = null },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTeamId == homeTeam.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(homeTeam.name, maxLines = 1, fontSize = 12.sp)
                    }
                    Button(
                        onClick = { selectedTeamId = awayTeam.id; selectedPlayerId = null },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTeamId == awayTeam.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(awayTeam.name, maxLines = 1, fontSize = 12.sp)
                    }
                }

                Text("Selecciona el MVP del partido:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .padding(4.dp)
                ) {
                    currentRoster.forEach { p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPlayerId = p.id }
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedPlayerId == p.id, onClick = { selectedPlayerId = p.id })
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("#${pDorsal(p.dorsal)} ${p.name}", fontSize = 13.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedTeamId, selectedPlayerId!!) },
                enabled = selectedPlayerId != null
            ) {
                Text("Designar MVP")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

private fun pDorsal(dorsal: Int): String {
    return if (dorsal > 0) dorsal.toString() else "-"
}
