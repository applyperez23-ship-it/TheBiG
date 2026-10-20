package com.example.ui.screens

import android.content.Context
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
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
import com.example.data.local.entity.Match
import com.example.data.local.entity.MatchEvent
import com.example.data.local.entity.Player
import com.example.data.local.entity.Team
import androidx.compose.material.icons.filled.Star
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
    val context = LocalContext.current
    val matches by viewModel.matches.collectAsStateWithLifecycle()
    val timerState by viewModel.timerState.collectAsStateWithLifecycle()

    val match = matches.firstOrNull { it.id == matchId }

    var homeTeam by remember { mutableStateOf<Team?>(null) }
    var awayTeam by remember { mutableStateOf<Team?>(null) }
    var homePlayers by remember { mutableStateOf<List<Player>>(emptyList()) }
    var awayPlayers by remember { mutableStateOf<List<Player>>(emptyList()) }
    var events by remember { mutableStateOf<List<MatchEvent>>(emptyList()) }
    var matchDurationMinutes by remember { mutableIntStateOf(20) }

    var showGoalDialog by remember { mutableStateOf<Team?>(null) }
    var showPenaltyIncidentDialog by remember { mutableStateOf<Team?>(null) }
    var showPenaltyTeamSelector by remember { mutableStateOf(false) }
    var showCardDialog by remember { mutableStateOf<Team?>(null) }
    var showSubDialog by remember { mutableStateOf<Team?>(null) }
    var showPenaltyShootoutDialog by remember { mutableStateOf(false) }
    var showMvpDialog by remember { mutableStateOf(false) }
    var showDeleteMatchDialog by remember { mutableStateOf(false) }

    val app = com.example.TheBiGApp.instance

    LaunchedEffect(match) {
        if (match != null) {
            homeTeam = app.repository.getTeamOnce(match.homeTeamId)
            awayTeam = app.repository.getTeamOnce(match.awayTeamId)
            val tournament = app.repository.getTournamentOnce(match.tournamentId)
            if (tournament != null) {
                matchDurationMinutes = tournament.matchDurationMinutes
            }
        }
    }

    LaunchedEffect(match?.homeTeamId) {
        match?.homeTeamId?.let { tid ->
            app.repository.getPlayersForTeam(tid).collect { homePlayers = it }
        }
    }

    LaunchedEffect(match?.awayTeamId) {
        match?.awayTeamId?.let { tid ->
            app.repository.getPlayersForTeam(tid).collect { awayPlayers = it }
        }
    }

    LaunchedEffect(matchId) {
        app.repository.getEventsForMatch(matchId).collect { events = it }
    }

    if (match == null || homeTeam == null || awayTeam == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Cargando partido...")
        }
        return
    }

    // Determine current displayed elapsed time and full clock format
    val isCurrentLiveMatch = timerState.matchId == match.id
    val currentElapsedSeconds = if (isCurrentLiveMatch) timerState.elapsedSeconds else match.elapsedSeconds

    val totalDurationSeconds = matchDurationMinutes * 60
    val remainingSeconds = (totalDurationSeconds - currentElapsedSeconds).coerceAtLeast(0)

    val elapsedFormatted = "%02d:%02d".format(currentElapsedSeconds / 60, currentElapsedSeconds % 60)
    val totalFormatted = "%02d:00".format(matchDurationMinutes)
    val remainingFormatted = "%02d:%02d".format(remainingSeconds / 60, remainingSeconds % 60)

    // Full clock representation: "04:37 / 20:00 - Restan 15:23"
    val fullClockDisplay = "$elapsedFormatted / $totalFormatted - Restan $remainingFormatted"

    val currentPeriod = if (isCurrentLiveMatch) timerState.period else match.currentPeriod
    val isRunning = isCurrentLiveMatch && timerState.isRunning

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Acta de Partido", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = {
                    if (match.status != "FINISHED") {
                        Button(
                            onClick = {
                                viewModel.finishMatch(context, match.id)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("finish_match_button")
                        ) {
                            Text("Finalizar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    IconButton(
                        onClick = { showDeleteMatchDialog = true },
                        modifier = Modifier.testTag("delete_match_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar Partido",
                            tint = MaterialTheme.colorScheme.error
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Scoreboard Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = match.roundName.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Home Team
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            TeamCrestView(
                                shieldIndex = homeTeam!!.shieldVectorIndex,
                                primaryColorHex = homeTeam!!.primaryColorHex,
                                secondaryColorHex = homeTeam!!.secondaryColorHex,
                                borderColorHex = homeTeam!!.borderColorHex,
                                shortName = homeTeam!!.shortName,
                                customShieldUri = homeTeam!!.customShieldUri,
                                size = 56.dp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = homeTeam!!.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        // Score Box
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 20.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "${match.homeScore} - ${match.awayScore}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 28.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (match.homePenalties != null && match.awayPenalties != null) {
                                Text(
                                    text = "Penaltis: (${match.homePenalties} - ${match.awayPenalties})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }

                        // Away Team
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            TeamCrestView(
                                shieldIndex = awayTeam!!.shieldVectorIndex,
                                primaryColorHex = awayTeam!!.primaryColorHex,
                                secondaryColorHex = awayTeam!!.secondaryColorHex,
                                borderColorHex = awayTeam!!.borderColorHex,
                                shortName = awayTeam!!.shortName,
                                customShieldUri = awayTeam!!.customShieldUri,
                                size = 56.dp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = awayTeam!!.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Timer & Period controls with Complete Clock Display
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            // Full clock format requested: "04:37 / 20:00 - Restan 15:23"
                            Text(
                                text = fullClockDisplay,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = if (isRunning) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = formatPeriod(currentPeriod),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!isRunning && match.status != "FINISHED") {
                                Button(
                                    onClick = {
                                        if (isCurrentLiveMatch) {
                                            viewModel.resumeLiveMatch(context)
                                        } else {
                                            viewModel.startLiveMatch(context, match, homeTeam!!, awayTeam!!)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("start_timer_button")
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Iniciar")
                                }
                            } else if (isRunning) {
                                OutlinedButton(
                                    onClick = { viewModel.pauseLiveMatch(context) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("pause_timer_button")
                                ) {
                                    Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pausar")
                                }
                            }
                        }
                    }

                    // Period Selector Chips
                    if (match.status != "FINISHED") {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            PeriodChip(label = "1T", selected = currentPeriod == "PRIMER_TIEMPO") {
                                viewModel.updatePeriod(context, match.id, "PRIMER_TIEMPO")
                            }
                            PeriodChip(label = "Descanso", selected = currentPeriod == "DESCANSO") {
                                viewModel.updatePeriod(context, match.id, "DESCANSO")
                            }
                            PeriodChip(label = "2T", selected = currentPeriod == "SEGUNDO_TIEMPO") {
                                viewModel.updatePeriod(context, match.id, "SEGUNDO_TIEMPO")
                            }
                            PeriodChip(label = "Extra", selected = currentPeriod == "TIEMPO_EXTRA_1" || currentPeriod == "TIEMPO_EXTRA_2") {
                                viewModel.updatePeriod(context, match.id, "TIEMPO_EXTRA_1")
                            }
                            PeriodChip(label = "Tanda Penales", selected = currentPeriod == "PENALTIS") {
                                viewModel.updatePeriod(context, match.id, "PENALTIS")
                                showPenaltyShootoutDialog = true
                            }
                        }
                    }
                }
            }

            // Quick Match Action Buttons
            if (match.status != "FINISHED") {
                Text(
                    text = "Registrar Incidencias",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showGoalDialog = homeTeam },
                        modifier = Modifier.weight(1f).testTag("goal_home_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Gol ${homeTeam!!.name.take(6)}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { showGoalDialog = awayTeam },
                        modifier = Modifier.weight(1f).testTag("goal_away_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Gol ${awayTeam!!.name.take(6)}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showCardDialog = homeTeam },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Tarjeta", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = { showSubDialog = homeTeam },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Sustitución", fontSize = 11.sp)
                    }

                    // Requirement 4: Replaced confusing shootout button with clear regular time Penalty option
                    Button(
                        onClick = { showPenaltyTeamSelector = true },
                        modifier = Modifier.weight(1.3f).testTag("penalty_incident_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Penal / Falta", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                val currentMvpEvent = events.firstOrNull { it.eventType == "MVP" }
                val mvpPlayer = if (currentMvpEvent?.playerId != null) {
                    (homePlayers + awayPlayers).firstOrNull { it.id == currentMvpEvent.playerId }
                } else null

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showMvpDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (mvpPlayer != null) "MVP: ${mvpPlayer.name}" else "Elegir MVP del Partido",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (match.isKnockout || match.currentPeriod == "PENALTIS") {
                        OutlinedButton(
                            onClick = { showPenaltyShootoutDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Tanda Penaltis", fontSize = 11.sp)
                        }
                    }
                }
            }

            // Timeline / Acta de Partido
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Acta Oficial de Incidencias (${events.size})",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (events.isEmpty()) {
                        Text(
                            text = "No hay incidencias registradas en este partido todavía.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val allPlayers = (homePlayers + awayPlayers).associateBy { it.id }
                        events.forEach { event ->
                            val team = if (event.teamId == homeTeam!!.id) homeTeam else awayTeam
                            val player = event.playerId?.let { allPlayers[it] }
                            val assist = event.assistPlayerId?.let { allPlayers[it] }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${event.minute}'",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.width(32.dp)
                                    )
                                    EventIcon(type = event.eventType)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = formatEventDescription(event.eventType, player?.name, assist?.name),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = team?.name ?: "",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (match.status != "FINISHED") {
                                    IconButton(
                                        onClick = { viewModel.deleteEvent(event) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Borrar evento",
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(16.dp)
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

    // Selector for which team was awarded a penalty in regular time
    if (showPenaltyTeamSelector) {
        AlertDialog(
            onDismissRequest = { showPenaltyTeamSelector = false },
            shape = RoundedCornerShape(16.dp),
            title = { Text("Penal / Falta en el Área", fontWeight = FontWeight.Bold) },
            text = { Text("¿Para qué equipo se sancionó el penal?") },
            confirmButton = {
                Button(
                    onClick = {
                        showPenaltyTeamSelector = false
                        showPenaltyIncidentDialog = homeTeam
                    }
                ) {
                    Text(homeTeam!!.name)
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showPenaltyTeamSelector = false
                        showPenaltyIncidentDialog = awayTeam
                    }
                ) {
                    Text(awayTeam!!.name)
                }
            }
        )
    }

    // Dedicated Regular-time Penalty Dialog with RadioButtons
    if (showPenaltyIncidentDialog != null) {
        val targetTeam = showPenaltyIncidentDialog!!
        val playersList = if (targetTeam.id == homeTeam!!.id) homePlayers else awayPlayers
        val currentMin = (currentElapsedSeconds / 60).coerceAtLeast(1)

        PenaltyIncidentDialog(
            team = targetTeam,
            players = playersList,
            initialMinute = currentMin,
            onDismiss = { showPenaltyIncidentDialog = null },
            onConfirm = { kickerId, scored, minute ->
                if (scored) {
                    viewModel.addGoal(
                        matchId = match.id,
                        teamId = targetTeam.id,
                        playerId = kickerId,
                        assistPlayerId = null,
                        isOwnGoal = false,
                        isPenalty = true,
                        minute = minute
                    )
                } else {
                    viewModel.addPenaltyMissed(
                        matchId = match.id,
                        teamId = targetTeam.id,
                        playerId = kickerId,
                        minute = minute
                    )
                }
                showPenaltyIncidentDialog = null
            }
        )
    }

    // Goal Dialog with mandatory RadioButtons
    if (showGoalDialog != null) {
        val targetTeam = showGoalDialog!!
        val playersList = if (targetTeam.id == homeTeam!!.id) homePlayers else awayPlayers
        val currentMin = (currentElapsedSeconds / 60).coerceAtLeast(1)

        GoalEventDialog(
            team = targetTeam,
            players = playersList,
            initialMinute = currentMin,
            onDismiss = { showGoalDialog = null },
            onConfirm = { playerId, assistId, isOwn, isPenalty, min ->
                viewModel.addGoal(
                    matchId = match.id,
                    teamId = targetTeam.id,
                    playerId = playerId,
                    assistPlayerId = assistId,
                    isOwnGoal = isOwn,
                    isPenalty = isPenalty,
                    minute = min
                )
                showGoalDialog = null
            }
        )
    }

    // Card Dialog with RadioButtons
    if (showCardDialog != null) {
        val currentMin = (currentElapsedSeconds / 60).coerceAtLeast(1)
        val targetTeam = showCardDialog!!
        val playersList = if (targetTeam.id == homeTeam!!.id) homePlayers else awayPlayers

        CardEventDialog(
            team = targetTeam,
            players = playersList,
            initialMinute = currentMin,
            onDismiss = { showCardDialog = null },
            onConfirm = { playerId, isRed, min ->
                viewModel.addCard(match.id, targetTeam.id, playerId, isRed, min)
                showCardDialog = null
            }
        )
    }

    // Substitution Dialog with RadioButtons
    if (showSubDialog != null) {
        val currentMin = (currentElapsedSeconds / 60).coerceAtLeast(1)
        val targetTeam = showSubDialog!!
        val playersList = if (targetTeam.id == homeTeam!!.id) homePlayers else awayPlayers

        SubEventDialog(
            team = targetTeam,
            players = playersList,
            initialMinute = currentMin,
            onDismiss = { showSubDialog = null },
            onConfirm = { outId, inId, min ->
                viewModel.addSubstitution(match.id, targetTeam.id, outId, inId, min)
                showSubDialog = null
            }
        )
    }

    // Penalty Shootout Dialog (Tanda de desempate)
    if (showPenaltyShootoutDialog) {
        PenaltyShootoutDialog(
            homeTeam = homeTeam!!,
            awayTeam = awayTeam!!,
            currentHome = match.homePenalties ?: 0,
            currentAway = match.awayPenalties ?: 0,
            onDismiss = { showPenaltyShootoutDialog = false },
            onConfirm = { homeP, awayP ->
                viewModel.recordPenaltyShootoutScore(match.id, homeP, awayP)
                showPenaltyShootoutDialog = false
            }
        )
    }

    if (showMvpDialog) {
        val currentMvpEvent = events.firstOrNull { it.eventType == "MVP" }
        MvpSelectionDialog(
            homeTeam = homeTeam!!,
            awayTeam = awayTeam!!,
            homePlayers = homePlayers,
            awayPlayers = awayPlayers,
            currentMvpPlayerId = currentMvpEvent?.playerId,
            onDismiss = { showMvpDialog = false },
            onConfirm = { team, player ->
                viewModel.setMatchMvp(match.id, team.id, player.id)
                showMvpDialog = false
            }
        )
    }

    // Requirement 5: Safety Delete Dialog for Match
    if (showDeleteMatchDialog) {
        DeleteConfirmationDialog(
            itemName = "el partido '${homeTeam!!.name} vs ${awayTeam!!.name}'",
            onDismiss = { showDeleteMatchDialog = false },
            onConfirm = {
                viewModel.deleteMatch(match.id)
                showDeleteMatchDialog = false
                onBack()
            }
        )
    }
}

@Composable
private fun PeriodChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun EventIcon(type: String) {
    when (type) {
        "GOAL", "PENALTY_GOAL" -> Icon(Icons.Default.SportsSoccer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        "PENALTY_MISSED" -> Box(modifier = Modifier.size(12.dp).background(Color(0xFFEF4444), RoundedCornerShape(2.dp)))
        "OWN_GOAL" -> Icon(Icons.Default.SportsSoccer, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
        "YELLOW_CARD" -> Box(modifier = Modifier.size(12.dp, 16.dp).background(Color(0xFFEAB308), RoundedCornerShape(2.dp)))
        "RED_CARD" -> Box(modifier = Modifier.size(12.dp, 16.dp).background(Color(0xFFEF4444), RoundedCornerShape(2.dp)))
        "SUBSTITUTION" -> Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
        "MVP" -> Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
        else -> Icon(Icons.Default.SportsSoccer, contentDescription = null, modifier = Modifier.size(18.dp))
    }
}

private fun formatPeriod(p: String): String {
    return when (p) {
        "PRIMER_TIEMPO" -> "Primer Tiempo"
        "DESCANSO" -> "Descanso"
        "SEGUNDO_TIEMPO" -> "Segundo Tiempo"
        "TIEMPO_EXTRA_1" -> "Tiempo Extra 1"
        "TIEMPO_EXTRA_2" -> "Tiempo Extra 2"
        "PENALTIS" -> "Tanda de Penaltis"
        "FINALIZADO" -> "Partido Finalizado"
        else -> p
    }
}

private fun formatEventDescription(type: String, playerName: String?, assistName: String?): String {
    val p = playerName ?: "Jugador"
    return when (type) {
        "GOAL" -> if (assistName != null) "Gol: $p (Asistencia: $assistName)" else "Gol: $p"
        "PENALTY_GOAL" -> "Gol de Penal: $p"
        "PENALTY_MISSED" -> "Penal Fallado / Atajado: $p"
        "OWN_GOAL" -> "Autogol: $p"
        "YELLOW_CARD" -> "Tarjeta Amarilla: $p"
        "RED_CARD" -> "Tarjeta Roja: $p"
        "SUBSTITUTION" -> "Entra: $p / Sale: ${assistName ?: "Compañero"}"
        "MVP" -> "Jugador del Partido (MVP): $p"
        else -> "$type: $p"
    }
}

@Composable
private fun PenaltyIncidentDialog(
    team: Team,
    players: List<Player>,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (kickerId: Long?, scored: Boolean, minute: Int) -> Unit
) {
    var selectedKickerId by remember { mutableStateOf<Long?>(players.firstOrNull()?.id) }
    var scored by remember { mutableStateOf(true) }
    var minuteStr by remember { mutableStateOf("$initialMinute") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = { Text("Penal / Falta en el Área - ${team.name}", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = minuteStr,
                    onValueChange = { minuteStr = it },
                    label = { Text("Minuto") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Resultado del Penal:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = scored,
                        onClick = { scored = true },
                        label = { Text("Gol Anotado") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = !scored,
                        onClick = { scored = false },
                        label = { Text("Fallado / Atajado") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Ejecutante del Penal (Obligatorio 1):", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                players.forEach { pl ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedKickerId = pl.id }
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedKickerId == pl.id,
                            onClick = { selectedKickerId = pl.id }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("#${pl.dorsal} ${pl.name}")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val m = minuteStr.toIntOrNull() ?: initialMinute
                    onConfirm(selectedKickerId, scored, m)
                },
                enabled = selectedKickerId != null
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
private fun GoalEventDialog(
    team: Team,
    players: List<Player>,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (playerId: Long?, assistId: Long?, isOwn: Boolean, isPenalty: Boolean, minute: Int) -> Unit
) {
    var selectedPlayerId by remember { mutableStateOf<Long?>(players.firstOrNull()?.id) }
    var selectedAssistId by remember { mutableStateOf<Long?>(null) }
    var isOwnGoal by remember { mutableStateOf(false) }
    var isPenalty by remember { mutableStateOf(false) }
    var minuteStr by remember { mutableStateOf("$initialMinute") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = { Text("Registrar Gol - ${team.name}", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = minuteStr,
                    onValueChange = { minuteStr = it },
                    label = { Text("Minuto") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = !isOwnGoal && !isPenalty,
                        onClick = { isOwnGoal = false; isPenalty = false },
                        label = { Text("Gol Jugada") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = isPenalty,
                        onClick = { isPenalty = true; isOwnGoal = false; selectedAssistId = null },
                        label = { Text("Penalti") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = isOwnGoal,
                        onClick = { isOwnGoal = true; isPenalty = false; selectedAssistId = null },
                        label = { Text("Autogol") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = if (isOwnGoal) "Jugador que anotó en contra (Obligatorio 1):" else "Autor del Gol (Obligatorio 1):",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                players.forEach { pl ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedPlayerId = pl.id }
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedPlayerId == pl.id,
                            onClick = { selectedPlayerId = pl.id }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("#${pl.dorsal} ${pl.name}")
                    }
                }

                if (!isOwnGoal && !isPenalty) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Asistencia (Máximo 1 Jugador):", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                    // Option for No Assist
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedAssistId = null }
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedAssistId == null,
                            onClick = { selectedAssistId = null }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sin Asistencia (Individual)")
                    }

                    players.filter { it.id != selectedPlayerId }.forEach { pl ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedAssistId = pl.id }
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedAssistId == pl.id,
                                onClick = { selectedAssistId = pl.id }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("#${pl.dorsal} ${pl.name}")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val m = minuteStr.toIntOrNull() ?: initialMinute
                    onConfirm(selectedPlayerId, selectedAssistId, isOwnGoal, isPenalty, m)
                },
                enabled = selectedPlayerId != null
            ) {
                Text("Confirmar Gol")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun CardEventDialog(
    team: Team,
    players: List<Player>,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (playerId: Long?, isRed: Boolean, minute: Int) -> Unit
) {
    var selectedPlayerId by remember { mutableStateOf<Long?>(players.firstOrNull()?.id) }
    var isRed by remember { mutableStateOf(false) }
    var minuteStr by remember { mutableStateOf("$initialMinute") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = { Text("Registrar Tarjeta - ${team.name}", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = minuteStr,
                    onValueChange = { minuteStr = it },
                    label = { Text("Minuto") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = !isRed,
                        onClick = { isRed = false },
                        label = { Text("Amarilla") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = isRed,
                        onClick = { isRed = true },
                        label = { Text("Roja") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Jugador Sancionado (Obligatorio 1):", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                players.forEach { pl ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedPlayerId = pl.id }
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedPlayerId == pl.id,
                            onClick = { selectedPlayerId = pl.id }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("#${pl.dorsal} ${pl.name}")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val m = minuteStr.toIntOrNull() ?: initialMinute
                    onConfirm(selectedPlayerId, isRed, m)
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
private fun SubEventDialog(
    team: Team,
    players: List<Player>,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (playerOutId: Long?, playerInId: Long?, minute: Int) -> Unit
) {
    var outId by remember { mutableStateOf<Long?>(players.firstOrNull()?.id) }
    var inId by remember { mutableStateOf<Long?>(players.getOrNull(1)?.id) }
    var minuteStr by remember { mutableStateOf("$initialMinute") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = { Text("Sustitución - ${team.name}", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = minuteStr,
                    onValueChange = { minuteStr = it },
                    label = { Text("Minuto") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Sale del campo (Obligatorio 1):", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                players.forEach { pl ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { outId = pl.id }
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = outId == pl.id,
                            onClick = { outId = pl.id }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("#${pl.dorsal} ${pl.name}")
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text("Entra al campo (Obligatorio 1):", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                players.filter { it.id != outId }.forEach { pl ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { inId = pl.id }
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = inId == pl.id,
                            onClick = { inId = pl.id }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("#${pl.dorsal} ${pl.name}")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val m = minuteStr.toIntOrNull() ?: initialMinute
                    onConfirm(outId, inId, m)
                },
                enabled = outId != null && inId != null
            ) {
                Text("Confirmar Cambio")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun PenaltyShootoutDialog(
    homeTeam: Team,
    awayTeam: Team,
    currentHome: Int,
    currentAway: Int,
    onDismiss: () -> Unit,
    onConfirm: (homePenalties: Int, awayPenalties: Int) -> Unit
) {
    var homeP by remember { mutableIntStateOf(currentHome) }
    var awayP by remember { mutableIntStateOf(currentAway) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = { Text("Tanda de Penaltis (Desempate)", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Anotados por cada equipo:", style = MaterialTheme.typography.bodySmall)

                // Home
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(homeTeam.name, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { if (homeP > 0) homeP-- }, modifier = Modifier.size(36.dp)) {
                            Text("-")
                        }
                        Text("$homeP", fontWeight = FontWeight.Black, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 12.dp))
                        Button(onClick = { homeP++ }, modifier = Modifier.size(36.dp)) {
                            Text("+")
                        }
                    }
                }

                // Away
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(awayTeam.name, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { if (awayP > 0) awayP-- }, modifier = Modifier.size(36.dp)) {
                            Text("-")
                        }
                        Text("$awayP", fontWeight = FontWeight.Black, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 12.dp))
                        Button(onClick = { awayP++ }, modifier = Modifier.size(36.dp)) {
                            Text("+")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(homeP, awayP) }) {
                Text("Guardar Penaltis")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun MvpSelectionDialog(
    homeTeam: Team,
    awayTeam: Team,
    homePlayers: List<Player>,
    awayPlayers: List<Player>,
    currentMvpPlayerId: Long?,
    onDismiss: () -> Unit,
    onConfirm: (Team, Player) -> Unit
) {
    var selectedTeamId by remember { mutableStateOf(homeTeam.id) }
    var selectedPlayerId by remember { mutableStateOf<Long?>(currentMvpPlayerId ?: homePlayers.firstOrNull()?.id) }

    val activeTeam = if (selectedTeamId == homeTeam.id) homeTeam else awayTeam
    val activePlayers = if (selectedTeamId == homeTeam.id) homePlayers else awayPlayers

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Elegir Jugador del Partido (MVP)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedTeamId == homeTeam.id,
                        onClick = {
                            selectedTeamId = homeTeam.id
                            if (homePlayers.isNotEmpty()) selectedPlayerId = homePlayers.firstOrNull()?.id
                        },
                        label = { Text(homeTeam.name, maxLines = 1) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedTeamId == awayTeam.id,
                        onClick = {
                            selectedTeamId = awayTeam.id
                            if (awayPlayers.isNotEmpty()) selectedPlayerId = awayPlayers.firstOrNull()?.id
                        },
                        label = { Text(awayTeam.name, maxLines = 1) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Selecciona al jugador más valioso:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                if (activePlayers.isEmpty()) {
                    Text("No hay jugadores registrados en este equipo", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                } else {
                    activePlayers.forEach { pl ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPlayerId = pl.id }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedPlayerId == pl.id,
                                onClick = { selectedPlayerId = pl.id }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            TeamJerseyView(team = activeTeam, isGoalkeeper = pl.isGoalkeeper, dorsal = pl.dorsal, size = 26.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "#${pl.dorsal} ${pl.name} ${if (pl.isGoalkeeper) "(Portero)" else ""}",
                                fontWeight = if (selectedPlayerId == pl.id) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = (homePlayers + awayPlayers).firstOrNull { it.id == selectedPlayerId }
                    if (p != null) {
                        val t = if (p.teamId == homeTeam.id) homeTeam else awayTeam
                        onConfirm(t, p)
                    }
                },
                enabled = selectedPlayerId != null
            ) {
                Text("Asignar MVP")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

