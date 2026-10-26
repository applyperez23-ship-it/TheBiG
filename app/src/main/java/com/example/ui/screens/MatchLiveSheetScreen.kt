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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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

    // Display formatted referee clock: "04:37 / 20:00 - Restan 15:23"
    val clockString = if (liveTimerState.matchId == matchId && currentMatch.status == "LIVE") {
        liveTimerState.fullClockDisplay
    } else {
        val totalMinutes = tournament?.matchDurationMinutes ?: 20
        val elapsed = currentMatch.elapsedSeconds
        val elapsedStr = "%02d:%02d".format(elapsed / 60, elapsed % 60)
        val remaining = ((totalMinutes * 60) - elapsed).coerceAtLeast(0)
        val remainingStr = "%02d:%02d".format(remaining / 60, remaining % 60)
        "$elapsedStr / %02d:00 - Restan $remainingStr".format(totalMinutes)
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
                            text = "${tournament?.name ?: "TheBiG"} - ${currentMatch.roundName}",
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
                                appendLine("${tournament?.name} - ${currentMatch.roundName}")
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
                                appendLine("Desarrollado por Daniel (el Grande)")
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
            // Live Referee Clock Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
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
                            fontSize = 16.sp,
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
                                    if (homeTeam != null && awayTeam != null) {
                                        viewModel.startLiveMatch(context, currentMatch, homeTeam!!, awayTeam!!)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                modifier = Modifier.testTag("start_match_button")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Iniciar Partido")
                            }
                        } else if (currentMatch.status == "LIVE") {
                            if (liveTimerState.isRunning) {
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
                                        fontSize = 14.sp,
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

            // Scoreboard Header Card (Direct Scoreboard Tapping to Score Goals)
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
                            text = "Toca el escudo o equipo para anotar gol rápido",
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
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = currentMatch.status != "FINISHED") {
                                    goalTargetTeam = homeTeam
                                    showGoalDialog = true
                                }
                                .testTag("scoreboard_home_team"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (currentMatch.status != "FINISHED") MaterialTheme.colorScheme.surface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            )
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            ) {
                                TeamCrestView(team = homeTeam, size = 50.dp)
                                Spacer(modifier = Modifier.height(4.dp))
                                homeTeam?.let { tm ->
                                    TeamJerseyView(team = tm, size = 32.dp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = homeTeam?.name ?: "Local",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
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
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Away Team Interactive Column
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = currentMatch.status != "FINISHED") {
                                    goalTargetTeam = awayTeam
                                    showGoalDialog = true
                                }
                                .testTag("scoreboard_away_team"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (currentMatch.status != "FINISHED") MaterialTheme.colorScheme.surface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            )
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            ) {
                                TeamCrestView(team = awayTeam, size = 50.dp)
                                Spacer(modifier = Modifier.height(4.dp))
                                awayTeam?.let { tm ->
                                    TeamJerseyView(team = tm, size = 32.dp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = awayTeam?.name ?: "Visitante",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
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
                fontSize = 16.sp
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
                            text = "Acta cerrada. El marcador y las incidencias están protegidos contra modificaciones accidentales.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Timeline / Chronology of Events
            Text(
                text = "Cronologia de Incidencias (${events.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            if (events.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "Aun no se han registrado incidencias en este partido.",
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
                fontSize = 16.sp
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
            title = "Eliminar Incidencia",
            message = "Estas seguro de eliminar esta incidencia (${ev.eventType} en el minuto ${ev.minute}')? Si es un gol, el marcador se actualizara en consecuencia.",
            onConfirm = {
                viewModel.deleteEvent(ev)
                eventToDelete = null
            },
            onDismiss = {
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
                        viewModel.startLiveMatch(context, currentMatch, homeTeam!!, awayTeam!!)
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

    // Dialog 1: Add Goal with Mandatory Strict Player Selection
    if (showGoalDialog && homeTeam != null && awayTeam != null) {
        GoalIncidentDialog(
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
                // Minute Badge
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${event.minute}'",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        when (event.eventType) {
                            "GOAL" -> {
                                Icon(Icons.Default.SportsSoccer, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Gol", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            "PENALTY_GOAL" -> {
                                Icon(Icons.Default.SportsSoccer, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Gol de Penal", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            "OWN_GOAL" -> {
                                Text("Autogol", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
                            }
                            "PENALTY_MISSED" -> {
                                Text("Penal Atajado/Fallado", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
                            }
                            "YELLOW_CARD" -> {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp, 14.dp)
                                        .background(Color(0xFFF59E0B), RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Tarjeta Amarilla", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            "RED_CARD" -> {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp, 14.dp)
                                        .background(Color(0xFFEF4444), RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Tarjeta Roja", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFEF4444))
                            }
                            "SUBSTITUTION" -> {
                                Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cambio", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            "MVP" -> {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Jugador MVP", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFF59E0B))
                            }
                        }
                    }

                    val playerText = buildString {
                        if (player != null) {
                            append(player.name)
                            if (player.number > 0) append(" (#${player.number})")
                            if (player.isCaptain) append(" (C)")
                        } else {
                            append("Jugador no asignado")
                        }
                        if (assistPlayer != null) {
                            append(" - Asist: ${assistPlayer.name}")
                        }
                        if (team != null) {
                            append(" [${team.name}]")
                        }
                    }
                    Text(
                        text = playerText,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (canDelete) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Borrar Incidencia",
                        tint = MaterialTheme.colorScheme.error,
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
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
                            text = "#${p.number}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(30.dp)
                        )
                        Text(
                            text = p.name,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        if (p.isCaptain) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFF59E0B), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "CAPITAN (C)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.Black
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        if (p.isGoalkeeper) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF10B981), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "PORTERO",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Dialog: Goal with strict 1-touch player selection
@Composable
fun GoalIncidentDialog(
    homeTeam: Team,
    awayTeam: Team,
    initialTeam: Team? = null,
    homePlayers: List<Player>,
    awayPlayers: List<Player>,
    defaultMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (teamId: Long, authorId: Long?, assistId: Long?, isOwnGoal: Boolean, isPen: Boolean, minute: Int) -> Unit
) {
    var selectedTeamId by remember(initialTeam) { mutableLongStateOf(initialTeam?.id ?: homeTeam.id) }
    var selectedAuthorId by remember { mutableStateOf<Long?>(null) }
    var selectedAssistId by remember { mutableStateOf<Long?>(null) }
    var isOwnGoal by remember { mutableStateOf(false) }
    var isPenalty by remember { mutableStateOf(false) }
    var minuteText by remember { mutableStateOf(defaultMinute.toString()) }

    val currentRoster = if (selectedTeamId == homeTeam.id) homePlayers else awayPlayers

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Gol Oficial", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Select Scoring Team
                Text("Equipo beneficiado con el Gol:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            selectedTeamId = homeTeam.id
                            selectedAuthorId = null
                            selectedAssistId = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTeamId == homeTeam.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(homeTeam.name, maxLines = 1, fontSize = 12.sp)
                    }
                    Button(
                        onClick = {
                            selectedTeamId = awayTeam.id
                            selectedAuthorId = null
                            selectedAssistId = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTeamId == awayTeam.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(awayTeam.name, maxLines = 1, fontSize = 12.sp)
                    }
                }

                // Goal Type Options
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Checkbox(
                        checked = isPenalty,
                        onCheckedChange = {
                            isPenalty = it
                            if (it) isOwnGoal = false
                        }
                    )
                    Text("Penal reglamentario", fontSize = 13.sp)

                    Spacer(modifier = Modifier.width(12.dp))

                    androidx.compose.material3.Checkbox(
                        checked = isOwnGoal,
                        onCheckedChange = {
                            isOwnGoal = it
                            if (it) {
                                isPenalty = false
                                selectedAssistId = null
                            }
                        }
                    )
                    Text("Autogol", fontSize = 13.sp)
                }

                // Minute Input
                OutlinedTextField(
                    value = minuteText,
                    onValueChange = { minuteText = it },
                    label = { Text("Minuto") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Mandatory Author Selection (Strict: 1 tap from active roster, no unassigned)
                Text(
                    text = if (isOwnGoal) "Jugador que anota en contra (Obligatorio):" else "Autor del Gol (Obligatorio - Plantilla Activa):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (selectedAuthorId == null && !isOwnGoal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )

                if (currentRoster.isEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Este equipo no tiene jugadores inscritos en su plantilla oficial. Registra jugadores en el menú Equipos para poder anotar goles.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, if (selectedAuthorId != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        currentRoster.forEach { player ->
                            val isSelected = selectedAuthorId == player.id
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedAuthorId = player.id
                                        if (selectedAssistId == player.id) selectedAssistId = null
                                    },
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            selectedAuthorId = player.id
                                            if (selectedAssistId == player.id) selectedAssistId = null
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "#${player.number} ${player.name}${if (player.isCaptain) " (C)" else ""}",
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected || player.isCaptain) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                // Assist RadioButtons (if not own goal)
                if (!isOwnGoal && currentRoster.isNotEmpty()) {
                    Text("Asistidor (Opcional):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedAssistId = null },
                            shape = RoundedCornerShape(6.dp),
                            color = if (selectedAssistId == null) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedAssistId == null,
                                    onClick = { selectedAssistId = null }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sin Asistencia", fontSize = 12.sp)
                            }
                        }

                        currentRoster.filter { it.id != selectedAuthorId }.forEach { player ->
                            val isSelected = selectedAssistId == player.id
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedAssistId = player.id },
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f) else Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedAssistId = player.id }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("#${player.number} ${player.name}${if (player.isCaptain) " (C)" else ""}", fontSize = 12.sp)
                                }
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
                    onConfirm(selectedTeamId, selectedAuthorId, selectedAssistId, isOwnGoal, isPenalty, minute)
                },
                enabled = selectedAuthorId != null,
                modifier = Modifier.testTag("confirm_add_goal_button")
            ) {
                Text("Guardar Gol")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// Dialog: Penalty in regular match (scored vs missed)
@Composable
fun PenaltyIncidentDialog(
    homeTeam: Team,
    awayTeam: Team,
    homePlayers: List<Player>,
    awayPlayers: List<Player>,
    defaultMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (teamId: Long, playerId: Long?, isScored: Boolean, minute: Int) -> Unit
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
                Text("Equipo ejecutante:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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

                // Outcome
                Text("Resultado del Penal:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                    fontSize = 13.sp,
                    color = if (selectedPlayerId == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )

                if (currentRoster.isEmpty()) {
                    Text("No hay jugadores inscritos en este equipo.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, if (selectedPlayerId != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        currentRoster.forEach { player ->
                            val isSelected = selectedPlayerId == player.id
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedPlayerId = player.id },
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedPlayerId = player.id }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("#${player.number} ${player.name}${if (player.isCaptain) " (C)" else ""}", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val m = minuteText.toIntOrNull() ?: defaultMinute
                    onConfirm(selectedTeamId, selectedPlayerId, isScored, m)
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

// Dialog: Yellow / Red Card with Strict Player Selection
@Composable
fun CardIncidentDialog(
    homeTeam: Team,
    awayTeam: Team,
    homePlayers: List<Player>,
    awayPlayers: List<Player>,
    defaultMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (teamId: Long, playerId: Long?, isRed: Boolean, minute: Int) -> Unit
) {
    var selectedTeamId by remember { mutableLongStateOf(homeTeam.id) }
    var selectedPlayerId by remember { mutableStateOf<Long?>(null) }
    var isRed by remember { mutableStateOf(false) }
    var minuteText by remember { mutableStateOf(defaultMinute.toString()) }

    val currentRoster = if (selectedTeamId == homeTeam.id) homePlayers else awayPlayers

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Sanción / Tarjeta", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Equipo sancionado:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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

                Text("Tipo de Tarjeta (Alimenta el Pozo MVP):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { isRed = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isRed) Color(0xFFF59E0B) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Amarilla", color = if (!isRed) Color.Black else Color.Unspecified, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { isRed = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRed) Color(0xFFEF4444) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Roja", color = Color.White, fontWeight = FontWeight.Bold)
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
                    text = "Jugador Sancionado (Obligatorio de la Plantilla):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (selectedPlayerId == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )

                if (currentRoster.isEmpty()) {
                    Text("No hay jugadores inscritos en este equipo.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, if (selectedPlayerId != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        currentRoster.forEach { player ->
                            val isSelected = selectedPlayerId == player.id
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedPlayerId = player.id },
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedPlayerId = player.id }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("#${player.number} ${player.name}${if (player.isCaptain) " (C)" else ""}", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val m = minuteText.toIntOrNull() ?: defaultMinute
                    onConfirm(selectedTeamId, selectedPlayerId, isRed, m)
                },
                enabled = selectedPlayerId != null
            ) {
                Text("Registrar Sanción")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// Dialog: Substitution with Strict Player Selection
@Composable
fun SubstitutionDialog(
    homeTeam: Team,
    awayTeam: Team,
    homePlayers: List<Player>,
    awayPlayers: List<Player>,
    defaultMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (teamId: Long, playerOutId: Long?, playerInId: Long?, minute: Int) -> Unit
) {
    var selectedTeamId by remember { mutableLongStateOf(homeTeam.id) }
    var selectedOutId by remember { mutableStateOf<Long?>(null) }
    var selectedInId by remember { mutableStateOf<Long?>(null) }
    var minuteText by remember { mutableStateOf(defaultMinute.toString()) }

    val currentRoster = if (selectedTeamId == homeTeam.id) homePlayers else awayPlayers

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cambio Táctico", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Equipo:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { selectedTeamId = homeTeam.id; selectedOutId = null; selectedInId = null },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTeamId == homeTeam.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(homeTeam.name, maxLines = 1, fontSize = 12.sp)
                    }
                    Button(
                        onClick = { selectedTeamId = awayTeam.id; selectedOutId = null; selectedInId = null },
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

                Text(
                    text = "Sale Jugador (Obligatorio):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (selectedOutId == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (selectedOutId != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    currentRoster.forEach { player ->
                        val isSelected = selectedOutId == player.id
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedOutId = player.id },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f) else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedOutId = player.id }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("#${player.number} ${player.name}", fontSize = 12.sp)
                            }
                        }
                    }
                }

                Text(
                    text = "Entra Jugador (Obligatorio):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (selectedInId == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (selectedInId != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    currentRoster.filter { it.id != selectedOutId }.forEach { player ->
                        val isSelected = selectedInId == player.id
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedInId = player.id },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedInId = player.id }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("#${player.number} ${player.name}", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val m = minuteText.toIntOrNull() ?: defaultMinute
                    onConfirm(selectedTeamId, selectedOutId, selectedInId, m)
                },
                enabled = selectedOutId != null && selectedInId != null
            ) {
                Text("Registrar Cambio")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// Dialog: Shootout Score
@Composable
fun ShootoutScoreDialog(
    initialHome: Int,
    initialAway: Int,
    onDismiss: () -> Unit,
    onConfirm: (homePen: Int, awayPen: Int) -> Unit
) {
    var homePenText by remember { mutableStateOf(initialHome.toString()) }
    var awayPenText by remember { mutableStateOf(initialAway.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tanda de Penaltis") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Registra la cantidad de penales acertados por cada equipo:")
                OutlinedTextField(
                    value = homePenText,
                    onValueChange = { homePenText = it },
                    label = { Text("Penaltis Local") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = awayPenText,
                    onValueChange = { awayPenText = it },
                    label = { Text("Penaltis Visitante") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val h = homePenText.toIntOrNull() ?: 0
                    val a = awayPenText.toIntOrNull() ?: 0
                    onConfirm(h, a)
                }
            ) {
                Text("Guardar Penaltis")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// Dialog: MVP Selection
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
        title = { Text("Eleccion de Jugador MVP") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Selecciona el equipo del jugador mas valioso del encuentro:")
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

                Text("Jugador MVP:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    currentRoster.forEach { player ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPlayerId = player.id },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedPlayerId == player.id,
                                onClick = { selectedPlayerId = player.id }
                            )
                            val label = buildString {
                                append("#${player.number} ${player.name}")
                                if (player.isCaptain) append(" (C)")
                            }
                            Text(label, fontSize = 12.sp, fontWeight = if (player.isCaptain) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedPlayerId?.let { onConfirm(selectedTeamId, it) }
                },
                enabled = selectedPlayerId != null
            ) {
                Text("Premiar MVP")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun ActionIconButton(
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
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(tint.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ActionCardIconButton(
    label: String,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Color(0xFFF59E0B).copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Box(
                    modifier = Modifier
                        .size(7.dp, 13.dp)
                        .background(Color(0xFFF59E0B), RoundedCornerShape(1.dp))
                )
                Box(
                    modifier = Modifier
                        .size(7.dp, 13.dp)
                        .background(Color(0xFFEF4444), RoundedCornerShape(1.dp))
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
